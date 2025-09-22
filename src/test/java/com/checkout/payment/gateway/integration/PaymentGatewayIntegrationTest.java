package com.checkout.payment.gateway.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.checkout.payment.gateway.domain.PaymentStatus;
import com.checkout.payment.gateway.dto.ErrorResponse;
import com.checkout.payment.gateway.dto.GetPaymentResponse;
import com.checkout.payment.gateway.dto.PostPaymentRequest;
import com.checkout.payment.gateway.dto.PostPaymentResponse;
import java.io.File;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PaymentGatewayIntegrationTest {

  @Container
  static DockerComposeContainer<?> environment = new DockerComposeContainer<>(
      new File("docker-compose.yml"))
      .withExposedService("bank_simulator", 8080)
      .waitingFor("bank_simulator", Wait.forListeningPort());

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    String bankApiUrl = "http://" + environment.getServiceHost("bank_simulator", 8080)
        + ":" + environment.getServicePort("bank_simulator", 8080) + "/payments";
    registry.add("bank.api.url", () -> bankApiUrl);
  }

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void shouldRetrievePaymentById() {
    PostPaymentRequest request = createValidRequest("1234567890123451");
    ResponseEntity<PostPaymentResponse> createResponse = restTemplate.postForEntity(
        "/payments", request, PostPaymentResponse.class);

    UUID paymentId = createResponse.getBody().id();

    ResponseEntity<GetPaymentResponse> getResponse = restTemplate.getForEntity(
        "/payments/" + paymentId, GetPaymentResponse.class);

    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getResponse.getBody().id()).isEqualTo(paymentId);
    assertThat(getResponse.getBody().status()).isEqualTo(PaymentStatus.AUTHORIZED);
  }

  @Test
  void shouldReturnNotFoundForNonExistentPayment() {
    ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
        "/payments/" + UUID.randomUUID(), ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody().message()).isEqualTo("Payment not found");
  }

  @Test
  void shouldProcessAuthorizedPayment() {
    PostPaymentRequest request = createValidRequest("1234567890123451");

    ResponseEntity<PostPaymentResponse> response = restTemplate.postForEntity(
        "/payments", request, PostPaymentResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().status()).isEqualTo(PaymentStatus.AUTHORIZED);
    assertThat(response.getBody().id()).isNotNull();
    assertThat(response.getBody().cardNumberLastFour()).isEqualTo("3451");
    assertThat(response.getBody().currency()).isEqualTo("USD");
    assertThat(response.getBody().amount()).isEqualTo(1000);
  }

  @Test
  void shouldProcessDeclinedPayment() {
    PostPaymentRequest request = createValidRequest("1234567890123452");

    ResponseEntity<PostPaymentResponse> response = restTemplate.postForEntity(
        "/payments", request, PostPaymentResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().status()).isEqualTo(PaymentStatus.DECLINED);
    assertThat(response.getBody().id()).isNotNull();
    assertThat(response.getBody().cardNumberLastFour()).isEqualTo("3452");
  }

  @Test
  void shouldProcessServiceErrorPayment() {
    PostPaymentRequest request = createValidRequest("1234567890123450");

    ResponseEntity<PostPaymentResponse> response = restTemplate.postForEntity(
        "/payments", request, PostPaymentResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody().id()).isNull();
  }

  private PostPaymentRequest createValidRequest(String cardNumber) {
    return new PostPaymentRequest(
        cardNumber,
        12,
        2028,
        "USD",
        1000,
        "123"
    );
  }
}
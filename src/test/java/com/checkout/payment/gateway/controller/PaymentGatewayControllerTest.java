package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.client.model.PostPaymentResponseBuilder;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentRequestBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import com.github.javafaker.Faker;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {
  private static final Faker faker = new Faker();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @LocalServerPort
  private Integer port;

  @RegisterExtension
  static WireMockExtension wireMock = WireMockExtension
      .newInstance()
      .options(wireMockConfig().dynamicPort())
      .build();

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("bank.base-url", wireMock::baseUrl);
  }

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
  }

  @Test
  void shouldReturn422WhenRequestIsNotValid() throws Exception {
    given()
        .contentType(ContentType.JSON)
        .body(PostPaymentRequestBuilder.builder().build())
        .when()
        .post("/payments")
        .then()
        .statusCode(422)
        .body("errors", hasItems(
            "Card number is required",
            "CVV is required",
            "Amount must be greater than 0",
            "Expiry month must be between 1 and 12",
            "Currency is required"
        ));

    given()
        .contentType(ContentType.JSON)
        .body(PostPaymentRequestBuilder.builder()
            .cardNumber("0")
            .build())
        .when()
        .post("/payments")
        .then()
        .statusCode(422)
        .body("errors", hasItems(
            "Card number must be between 14 and 19 digits"
        ));

    given()
        .contentType(ContentType.JSON)
        .body(PostPaymentRequestBuilder.builder()
            .cvv("12345")
            .build())
        .when()
        .post("/payments")
        .then()
        .statusCode(422)
        .body("errors", hasItems(
            "CVV must be 3 or 4 digits"
        ));

    given()
        .contentType(ContentType.JSON)
        .body(PostPaymentRequestBuilder.builder()
            .currency("FOUR")
            .build())
        .when()
        .post("/payments")
        .then()
        .statusCode(422)
        .body("errors", hasItems(
            "Currency must be 3 characters"
        ));
  }

  @Test
  void shouldCreatePayment() throws Exception {
    String currency = "EUR";
    String amount = "10025";
    String cvv = "123";


    wireMock.stubFor(
        WireMock.get(urlMatching("/payments"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withStatus(200)
                    .withBody(
                        objectMapper.writeValueAsString(PostPaymentResponseBuilder.builder()
                            .authorizationCode("auth_code")
                            .authorized(true)
                            .build())
                    )
            )
    );

    given()
        .contentType(ContentType.JSON)
        .body(PostPaymentRequestBuilder.builder()
            .currency(currency)
            .build())
        .when()
        .post("/payments")
        .then()
        .statusCode(200);
  }

  @Autowired
  private MockMvc mvc;
  @Autowired
  PaymentsRepository paymentsRepository;

  @Test
  void whenPaymentIsCreated() throws Exception {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4321");
    request.setExpiryMonth(12);
    request.setExpiryYear(2026);
    request.setCurrency("USD");
    request.setAmount(150);
    request.setCvv("123");

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.cardNumberLastFour").value(4321))
        .andExpect(jsonPath("$.expiryMonth").value(12))
        .andExpect(jsonPath("$.expiryYear").value(2026))
        .andExpect(jsonPath("$.currency").value("USD"))
        .andExpect(jsonPath("$.amount").value(150))
        .andExpect(jsonPath("$.id").exists());
  }

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    PostPaymentResponse payment = new PostPaymentResponse();
    payment.setId(UUID.randomUUID());
    payment.setAmount(10);
    payment.setCurrency("USD");
    payment.setStatus(PaymentStatus.AUTHORIZED);
    payment.setExpiryMonth(12);
    payment.setExpiryYear(2024);
    payment.setCardNumberLastFour(4321);

    paymentsRepository.add(payment);

    mvc.perform(MockMvcRequestBuilders.get("/payment/" + payment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(payment.getCardNumberLastFour()))
        .andExpect(jsonPath("$.expiryMonth").value(payment.getExpiryMonth()))
        .andExpect(jsonPath("$.expiryYear").value(payment.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
        .andExpect(jsonPath("$.amount").value(payment.getAmount()));
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Page not found"));
  }
}

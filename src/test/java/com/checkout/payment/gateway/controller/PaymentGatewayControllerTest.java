package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.Utils;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.Payment;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.model.IdempotencyStoreEntry;
import com.checkout.payment.gateway.model.PaymentRequestStatus;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.repository.IdempotencyStoreRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.checkout.payment.gateway.enums.PaymentStatus.AUTHORIZED;
import static com.checkout.payment.gateway.enums.PaymentStatus.DECLINED;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class PaymentGatewayControllerTest {

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

  @Autowired
  PaymentsRepository paymentsRepository;
  @Autowired
  IdempotencyStoreRepository idempotencyStoreRepository;

  @Test
  void shouldReturn422WhenRequestIsNotValid() throws Exception {
    given()
        .contentType(ContentType.JSON)
        .body(PostPaymentRequest.builder().build())
        .header("Idempotency-Key", UUID.randomUUID())
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
        .body(PostPaymentRequest.builder()
            .cardNumber("0")
            .build())
        .header("Idempotency-Key", UUID.randomUUID())
        .when()
        .post("/payments")
        .then()
        .statusCode(422)
        .body("errors", hasItems(
            "Card number must be between 14 and 19 digits"
        ));

    given()
        .contentType(ContentType.JSON)
        .body(PostPaymentRequest.builder()
            .cvv("12345")
            .build())
        .header("Idempotency-Key", UUID.randomUUID())
        .when()
        .post("/payments")
        .then()
        .statusCode(422)
        .body("errors", hasItems(
            "CVV must be 3 or 4 digits"
        ));

    given()
        .contentType(ContentType.JSON)
        .body(PostPaymentRequest.builder()
            .currency("FOUR")
            .build())
        .header("Idempotency-Key", UUID.randomUUID())
        .when()
        .post("/payments")
        .then()
        .statusCode(422)
        .body("errors", hasItems(
            "Currency must be 3 characters"
        ));
  }

  @Test
  void shouldReturn422WhenExpiryYearMonthIsInPast() throws Exception {
    YearMonth now = YearMonth.now();
    YearMonth past = now.minusMonths(1);
    given()
        .contentType(ContentType.JSON)
        .body(PostPaymentRequest.builder()
            .amount(100)
            .cardNumber(CardNumberGenerator.generateCardNumber(14))
            .expiryMonth(past.getMonthValue())
            .expiryYear(past.getYear())
            .currency("EUR")
            .cvv("123")
            .build())
        .header("Idempotency-Key", UUID.randomUUID())
        .when()
        .post("/payments")
        .then()
        .log().all()
        .statusCode(422)
        .body("errors", hasItems(
            "Card expiry date must be in the future"
        ));
  }

  @Test
  void shouldReturn422WhenCurrencyIsNotSupported() {
    YearMonth now = YearMonth.now();
    given()
        .contentType(ContentType.JSON)
        .body(PostPaymentRequest.builder()
            .amount(100)
            .cardNumber(CardNumberGenerator.generateCardNumber(14))
            .expiryMonth(now.getMonthValue())
            .expiryYear(now.getYear())
            .currency("JPY")
            .cvv("123")
            .build())
        .header("Idempotency-Key", UUID.randomUUID())
        .when()
        .post("/payments")
        .then()
        .log().all()
        .statusCode(422)
        .body("errors", hasItems(
            "Currency must be one of: [USD, EUR, GBP]"
        ));
  }

  @Test
  void shouldReturn422WhenRequestBodyNotMatch() throws Exception {
    YearMonth now = YearMonth.now();
    var idempotencyKey = UUID.randomUUID();

    var requestBuilder = PostPaymentRequest.builder()
        .amount(10025)
        .cardNumber(CardNumberGenerator.generateCardNumber(14))
        .expiryMonth(now.getMonthValue())
        .expiryYear(now.getYear())
        .currency("EUR");

    idempotencyStoreRepository.add(IdempotencyStoreEntry.builder()
        .idempotencyKey(idempotencyKey)
        .request(requestBuilder.cvv("123").build())
        .status(PaymentRequestStatus.SUCCESS)
        .build()
    );

    given()
        .contentType(ContentType.JSON)
        .body(requestBuilder.cvv("345").build())
        .header("Idempotency-Key", idempotencyKey)
        .when()
        .post("/payments")
        .then()
        .log().all()
        .statusCode(422)
        .body("message", is(
            "Idempotency key already used with different request"
        ));
  }

  @Test
  void shouldReturn201WhenRequestBodyMatches() {
    YearMonth now = YearMonth.now();
    var cardNumber = CardNumberGenerator.generateCardNumber(14);
    var cardNumberLastFour = Utils.getCardNumberLastFour(cardNumber);
    var idempotencyKey = UUID.randomUUID();
    var currency = "EUR";
    var amount = 10025;
    var cvv = "123";

    var request = PostPaymentRequest.builder()
        .amount(amount)
        .cardNumber(cardNumber)
        .expiryMonth(now.getMonthValue())
        .expiryYear(now.getYear())
        .currency(currency)
        .cvv(cvv)
        .build();

    var response = PostPaymentResponse.builder()
        .id(UUID.randomUUID())
        .expiryYear(now.getYear())
        .amount(amount)
        .expiryMonth(now.getMonthValue())
        .cardNumberLastFour(cardNumberLastFour)
        .currency(currency)
        .status(AUTHORIZED)
        .build();

    idempotencyStoreRepository.add(IdempotencyStoreEntry.builder()
        .idempotencyKey(idempotencyKey)
        .request(request)
            .response(response)
        .status(PaymentRequestStatus.SUCCESS)
        .build()
    );

    given()
        .contentType(ContentType.JSON)
        .body(request)
        .header("Idempotency-Key", idempotencyKey)
        .when()
        .post("/payments")
        .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("expiryYear", equalTo(now.getYear()))
        .body("expiryMonth", equalTo(now.getMonthValue()))
        .body("amount", equalTo(amount))
        .body("cardNumberLastFour", equalTo(cardNumberLastFour))
        .body("currency", equalTo(currency))
        .body("status", equalTo(AUTHORIZED.getName()));
  }

  @Test
  void shouldCreatePaymentWithAuthorizedStatus() throws Exception {
    String currency = "EUR";
    int amount = 10025;
    String cvv = "123";
    String cardNumber = CardNumberGenerator.generateCardNumber(14);
    YearMonth now = YearMonth.now();
    DateTimeFormatter expiryDateFormatter = DateTimeFormatter.ofPattern("MM/yyyy");
    var idempotencyKey = UUID.randomUUID();

    wireMock.stubFor(
        WireMock.post(urlMatching("/payments"))
            .withRequestBody(WireMock.equalToJson(objectMapper.writeValueAsString(com.checkout.payment.gateway.client.model.PostPaymentRequest.builder()
                .expiryDate(now.format(expiryDateFormatter))
                .amount(amount)
                .cardNumber(cardNumber)
                .currency(currency)
                .cvv(cvv)
                .build())))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withStatus(200)
                    .withBody(
                        objectMapper.writeValueAsString(com.checkout.payment.gateway.client.model.PostPaymentResponse.builder()
                            .authorizationCode("auth_code")
                            .authorized(true)
                            .build())
                    )
            )
    );

    var request = PostPaymentRequest.builder()
        .amount(amount)
        .cardNumber(cardNumber)
        .expiryMonth(now.getMonthValue())
        .expiryYear(now.getYear())
        .currency(currency)
        .cvv(cvv)
        .build();

    var response = given()
        .contentType(ContentType.JSON)
        .body(request)
        .header("Idempotency-Key", idempotencyKey)
        .when()
        .post("/payments")
        .then()
        .log().all()
        .statusCode(201)
        .extract()
        .as(PostPaymentResponse.class);

    assertNotNull(response.getId());
    assertEquals(AUTHORIZED.getName(), response.getStatus().getName());
    assertEquals(Utils.getCardNumberLastFour(cardNumber), response.getCardNumberLastFour());
    assertEquals(now.getMonthValue(), response.getExpiryMonth());
    assertEquals(now.getYear(), response.getExpiryYear());
    assertEquals(currency, response.getCurrency());
    assertEquals(amount, response.getAmount());

    var idempotencyStoreEntry = idempotencyStoreRepository.get(idempotencyKey);

    assertNotNull(idempotencyStoreEntry);
    assertEquals(request, idempotencyStoreEntry.getRequest());
    assertEquals(PaymentRequestStatus.SUCCESS, idempotencyStoreEntry.getStatus());

    var payment = paymentsRepository.get(response.getId()).orElseThrow();
    assertNotNull(payment);
    assertEquals(AUTHORIZED.getName(), payment.getStatus().getName());
    assertEquals(Utils.getCardNumberLastFour(cardNumber), payment.getCardNumberLastFour());
    assertEquals(now.getMonthValue(), payment.getExpiryMonth());
    assertEquals(now.getYear(), payment.getExpiryYear());
    assertEquals(currency, payment.getCurrency());
    assertEquals(amount, payment.getAmount());
  }

  @Test
  void shouldCreatePaymentWithDeclinedStatus() throws Exception {
    String currency = "EUR";
    int amount = 10025;
    String cvv = "123";
    String cardNumber = CardNumberGenerator.generateCardNumber(14);
    YearMonth now = YearMonth.now();
    DateTimeFormatter expiryDateFormatter = DateTimeFormatter.ofPattern("MM/yyyy");
    var idempotencyKey = UUID.randomUUID();

    wireMock.stubFor(
        WireMock.post(urlMatching("/payments"))
            .withRequestBody(WireMock.equalToJson(objectMapper.writeValueAsString(com.checkout.payment.gateway.client.model.PostPaymentRequest.builder()
                .expiryDate(now.format(expiryDateFormatter))
                .amount(amount)
                .cardNumber(cardNumber)
                .currency(currency)
                .cvv(cvv)
                .build())))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withStatus(200)
                    .withBody(
                        objectMapper.writeValueAsString(com.checkout.payment.gateway.client.model.PostPaymentResponse.builder()
                            .authorizationCode("")
                            .authorized(false)
                            .build())
                    )
            )
    );

    var request = PostPaymentRequest.builder()
        .amount(amount)
        .cardNumber(cardNumber)
        .expiryMonth(now.getMonthValue())
        .expiryYear(now.getYear())
        .currency(currency)
        .cvv(cvv)
        .build();

    var response = given()
        .contentType(ContentType.JSON)
        .body(request)
        .header("Idempotency-Key", idempotencyKey)
        .when()
        .post("/payments")
        .then()
        .log().all()
        .statusCode(201)
        .extract()
        .as(PostPaymentResponse.class);

    assertNotNull(response.getId());
    assertEquals(DECLINED.getName(), response.getStatus().getName());
    assertEquals(Utils.getCardNumberLastFour(cardNumber), response.getCardNumberLastFour());
    assertEquals(now.getMonthValue(), response.getExpiryMonth());
    assertEquals(now.getYear(), response.getExpiryYear());
    assertEquals(currency, response.getCurrency());
    assertEquals(amount, response.getAmount());

    var idempotencyStoreEntry = idempotencyStoreRepository.get(idempotencyKey);

    assertNotNull(idempotencyStoreEntry);
    assertEquals(request, idempotencyStoreEntry.getRequest());
    assertEquals(PaymentRequestStatus.SUCCESS, idempotencyStoreEntry.getStatus());

    var payment = paymentsRepository.get(response.getId()).orElseThrow();
    assertNotNull(payment);
    assertEquals(DECLINED.getName(), payment.getStatus().getName());
    assertEquals(Utils.getCardNumberLastFour(cardNumber), payment.getCardNumberLastFour());
    assertEquals(now.getMonthValue(), payment.getExpiryMonth());
    assertEquals(now.getYear(), payment.getExpiryYear());
    assertEquals(currency, payment.getCurrency());
    assertEquals(amount, payment.getAmount());
  }

  @Test
  void shouldRecordAttemptWhenBankCallFail() throws Exception {
    String currency = "EUR";
    int amount = 10025;
    String cvv = "123";
    String cardNumber = CardNumberGenerator.generateCardNumber(14);
    YearMonth now = YearMonth.now();
    DateTimeFormatter expiryDateFormatter = DateTimeFormatter.ofPattern("MM/yyyy");
    var idempotencyKey = UUID.randomUUID();

    wireMock.stubFor(
        WireMock.post(urlMatching("/payments"))
            .withRequestBody(WireMock.equalToJson(objectMapper.writeValueAsString(com.checkout.payment.gateway.client.model.PostPaymentRequest.builder()
                .expiryDate(now.format(expiryDateFormatter))
                .amount(amount)
                .cardNumber(cardNumber)
                .currency(currency)
                .cvv(cvv)
                .build())))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withStatus(503)
            )
    );

    var request = PostPaymentRequest.builder()
        .amount(amount)
        .cardNumber(cardNumber)
        .expiryMonth(now.getMonthValue())
        .expiryYear(now.getYear())
        .currency(currency)
        .cvv(cvv)
        .build();

    given()
        .contentType(ContentType.JSON)
        .body(request)
        .header("Idempotency-Key", idempotencyKey)
        .when()
        .post("/payments")
        .then()
        .log().all()
        .statusCode(503);

    var idempotencyStoreEntry = idempotencyStoreRepository.get(idempotencyKey);

    assertNotNull(idempotencyStoreEntry);
    assertEquals(request, idempotencyStoreEntry.getRequest());
    assertEquals(PaymentRequestStatus.FAILED, idempotencyStoreEntry.getStatus());

    assertEquals(0, paymentsRepository.size());
  }

  @Test
  void whenPaymentExistsThen200IsReturned() {
    var paymentId = UUID.randomUUID();
    var amount = 10025;
    var currency = "EUR";
    var expiryMonth = 12;
    var expiryYear = 2025;
    var cardNumber = CardNumberGenerator.generateCardNumber(14);
    var cardNumberLastFour = Utils.getCardNumberLastFour(cardNumber);

    var payment = Payment.builder()
        .id(paymentId)
        .amount(amount)
        .status(AUTHORIZED)
        .currency(currency)
        .cardNumberLastFour(cardNumberLastFour)
        .expiryMonth(expiryMonth)
        .expiryYear(expiryYear)
        .build();

    paymentsRepository.add(payment);

    given()
        .contentType(ContentType.JSON)
        .when()
        .get("/payments/{id}", paymentId)
        .then()
        .statusCode(200)
        .body("id", equalTo(paymentId.toString()))
        .body("expiryYear", equalTo(expiryYear))
        .body("expiryMonth", equalTo(expiryMonth))
        .body("amount", equalTo(amount))
        .body("cardNumberLastFour", equalTo(cardNumberLastFour))
        .body("currency", equalTo(currency))
        .body("status", equalTo(AUTHORIZED.getName()));
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    given()
        .contentType(ContentType.JSON)
        .when()
        .get("/payments/{id}", UUID.randomUUID())
        .then()
        .log().all()
        .statusCode(404)
        .body("message", is("Payment not found"));
  }
}

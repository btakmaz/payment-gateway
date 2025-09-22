package com.checkout.payment.gateway.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.domain.PaymentStatus;
import com.checkout.payment.gateway.dto.GetPaymentResponse;
import com.checkout.payment.gateway.dto.PostPaymentResponse;
import com.checkout.payment.gateway.exception.ExpiredCardException;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(PaymentGatewayController.class)
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;
  
  @MockBean
  private PaymentGatewayService paymentGatewayService;

  @BeforeEach
  void setUp() {
  }

  @ParameterizedTest
  @EnumSource(value = PaymentStatus.class, names = {"AUTHORIZED", "DECLINED"})
  void whenPaymentsExistThenCorrectDetailsAreReturned(PaymentStatus status) throws Exception {
    String expectedStatus = status.getName();
    int amount = 10;
    String currency = "USD";
    int expiryMonth = 12;
    int expiryYear = 2028;
    String lastFour = "4321";

    UUID paymentId = UUID.randomUUID();
    GetPaymentResponse response = new GetPaymentResponse(
        paymentId,
        status,
        lastFour,
        expiryMonth,
        expiryYear,
        currency,
        amount
    );

    when(paymentGatewayService.getPaymentById(paymentId)).thenReturn(response);

    mvc.perform(MockMvcRequestBuilders.get("/payments/" + paymentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(paymentId.toString()))
        .andExpect(jsonPath("$.status").value(expectedStatus))
        .andExpect(jsonPath("$.cardNumberLastFour").value(lastFour))
        .andExpect(jsonPath("$.expiryMonth").value(expiryMonth))
        .andExpect(jsonPath("$.expiryYear").value(expiryYear))
        .andExpect(jsonPath("$.currency").value(currency))
        .andExpect(jsonPath("$.amount").value(amount));
  }

  @Test
  void whenPaymentMissingThenReturnsNotFound() throws Exception {
    UUID nonExistentId = UUID.randomUUID();
    
    when(paymentGatewayService.getPaymentById(nonExistentId))
        .thenThrow(new PaymentNotFoundException("Invalid ID"));

    mvc.perform(MockMvcRequestBuilders.get("/payments/" + nonExistentId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Payment not found"));
  }

  @Test
  void whenPostPaymentWithInvalidJsonThenReturnsRejected() throws Exception {
    String invalidJson = """
        {
          "cardNumber": "invalid",
          "expiryMonth": "invalid"
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Invalid JSON format"))
        .andExpect(jsonPath("$.code").value("INVALID_JSON"))
        .andExpect(jsonPath("$.status").value("Rejected"));
  }

  @Test
  void whenPostPaymentWithInvalidExpiryMonthThenReturnsRejected() throws Exception {
    String invalidExpiryJson = """
        {
          "cardNumber": "1234567890123451",
          "expiryMonth": 13,
          "expiryYear": 2028,
          "currency": "USD",
          "amount": 1500,
          "cvv": "123"
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidExpiryJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("Validation failed")))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.status").value("Rejected"));
  }

  @Test
  void whenPostPaymentWithPastExpiryDateThenReturnsRejected() throws Exception {
    YearMonth pastDate = YearMonth.now().minusMonths(1);
    String pastExpiryJson = String.format("""
        {
          "cardNumber": "1234567890123451",
          "expiryMonth": %d,
          "expiryYear": %d,
          "currency": "USD",
          "amount": 1500,
          "cvv": "123"
        }
        """, pastDate.getMonthValue(), pastDate.getYear());

    when(paymentGatewayService.processPayment(any()))
        .thenThrow(new ExpiredCardException("Card expiry date must be in the future"));

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(pastExpiryJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Card expiry date must be in the future"))
        .andExpect(jsonPath("$.code").value("EXPIRED_CARD"))
        .andExpect(jsonPath("$.status").value("Rejected"));
  }

  @Test
  void whenPostPaymentWithInvalidCardNumberThenReturnsRejected() throws Exception {
    String invalidCardJson = """
        {
          "cardNumber": "123",
          "expiryMonth": 12,
          "expiryYear": 2028,
          "currency": "USD",
          "amount": 1500,
          "cvv": "123"
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidCardJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("Validation failed")))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.status").value("Rejected"));
  }

  @Test
  void whenPostPaymentWithInvalidCurrencyThenReturnsRejected() throws Exception {
    String invalidCurrencyJson = """
        {
          "cardNumber": "1234567890123451",
          "expiryMonth": 12,
          "expiryYear": 2028,
          "currency": "JPY",
          "amount": 1500,
          "cvv": "123"
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidCurrencyJson))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message").value(containsString("Currency must be one of: USD, EUR, GBP")))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.status").value("Rejected"));
  }

  @Test
  void whenPostPaymentWithValidCurrenciesThenReturnsSuccess() throws Exception {
    String[] validCurrencies = {"USD", "EUR", "GBP"};

    for (String currency : validCurrencies) {
      String validCurrencyJson = String.format("""
          {
            "cardNumber": "1234567890123451",
            "expiryMonth": 12,
            "expiryYear": 2028,
            "currency": "%s",
            "amount": 1500,
            "cvv": "123"
          }
          """, currency);

      UUID paymentId = UUID.randomUUID();
      PostPaymentResponse response = new PostPaymentResponse(
          paymentId,
          PaymentStatus.AUTHORIZED,
          "3451",
          12,
          2028,
          currency,
          1500
      );

      when(paymentGatewayService.processPayment(any())).thenReturn(response);

      mvc.perform(MockMvcRequestBuilders.post("/payments")
              .contentType(MediaType.APPLICATION_JSON)
              .content(validCurrencyJson))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.status").value("Authorized"))
          .andExpect(jsonPath("$.currency").value(currency));
    }
  }

  @Test
  void whenPostPaymentWithInvalidAmountThenReturnsRejected() throws Exception {
    String invalidAmountJson = """
        {
          "cardNumber": "1234567890123451",
          "expiryMonth": 12,
          "expiryYear": 2028,
          "currency": "USD",
          "amount": -1,
          "cvv": "123"
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidAmountJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("Validation failed")))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.status").value("Rejected"));
  }

  @Test
  void whenPostPaymentWithInvalidCvvThenReturnsRejected() throws Exception {
    String invalidCvvJson = """
        {
          "cardNumber": "1234567890123451",
          "expiryMonth": 12,
          "expiryYear": 2028,
          "currency": "USD",
          "amount": 1500,
          "cvv": "12"
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidCvvJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("Validation failed")))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.status").value("Rejected"));
  }

  @Test
  void whenPostPaymentWithMissingRequiredFieldsThenReturnsRejected() throws Exception {
    String incompleteJson = """
        {
          "cardNumber": "1234567890123451",
          "expiryMonth": 12
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(incompleteJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("Validation failed")))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.status").value("Rejected"));
  }
}
package com.checkout.payment.gateway.controller;


import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import org.junit.Test;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import simulators.BankSimulator;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;
  @Autowired
  PaymentsRepository paymentsRepository;

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

  @Test
  void testProcessPaymentValid() {
    PaymentsRepository storage = new PaymentsRepository();
    BankSimulator simulator = new BankSimulator();
    PostPaymentResponse response = new PostPaymentResponse();
    PaymentGatewayService gateway = new PaymentGatewayService(storage, simulator, response);

    PostPaymentRequest payment = new PostPaymentRequest(8877, 04, 2025, "USD", 100, 123);
    PostPaymentResponse processedPayment = gateway.processPayment(payment);

    assertEquals("Authorized", processedPayment.getStatus());
  }

  @Test
  void testProcessPaymentInvalidCard() {
    PaymentsRepository storage = new PaymentsRepository();
    BankSimulator simulator = new BankSimulator();
    PostPaymentResponse response = new PostPaymentResponse();
    PaymentGatewayService gateway = new PaymentGatewayService(storage, simulator, response);

    PostPaymentRequest payment = new PostPaymentRequest(8880, 04, 2025, "USD", 100, 123);
    PostPaymentResponse processedPayment = gateway.processPayment(payment);

    assertEquals("Declined", processedPayment.getStatus());


  }
}

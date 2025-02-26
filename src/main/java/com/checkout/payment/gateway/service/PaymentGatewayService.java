package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import simulators.BankSimulator;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final BankSimulator bankSimulator;
  private final PostPaymentResponse response;

  public PaymentGatewayService(PaymentsRepository paymentsRepository, BankSimulator bankSimulator,
      PostPaymentResponse response) {
    this.paymentsRepository = paymentsRepository;
    this.bankSimulator = bankSimulator;
    this.response = response;
  }

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    return paymentsRepository.get(id).orElseThrow(() -> new EventProcessingException("Invalid ID"));
  }

  public PostPaymentResponse processPayment(PostPaymentRequest paymentRequest) {
    // Validate the payment details
    if (validatePayment(paymentRequest)) {
      String status = bankSimulator.processPayment(paymentRequest);
      response.setStatus(PaymentStatus.valueOf(status));
      response.setId(UUID.fromString(UUID.randomUUID().toString()));
      paymentsRepository.add(response);
      return response;
    } else {
      PostPaymentResponse rejectedPayment = new PostPaymentResponse(paymentRequest.getCardNumberLastFour(), paymentRequest.getExpiryMonth(), paymentRequest.getExpiryYear(), paymentRequest.getCurrency(), paymentRequest.getAmount(), paymentRequest.getCvv());
      response.setStatus(PaymentStatus.valueOf("Rejected"));
      return rejectedPayment;
    }
  }

  private boolean validatePayment(PostPaymentRequest payment) {
    // Card Number must be numeric and between 14 and 19 digits
    if (String.valueOf(payment.getCardNumberLastFour()).matches("\\d{14,19}")) return false;

    // Expiry Date Validation: Should be in the future
    if (payment.getExpiryYear() < 2025 || payment.getExpiryMonth() < 1 || payment.getExpiryMonth() > 12) return false;

    // Currency Validation
    if (!"USD".equals(payment.getCurrency()) && !"GBP".equals(payment.getCurrency()) && !"EUR".equals(payment.getCurrency())) return false;

    // Amount should be positive integer
    return payment.getAmount() > 0;
  }

}


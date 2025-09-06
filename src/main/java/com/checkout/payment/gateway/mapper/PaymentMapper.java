package com.checkout.payment.gateway.mapper;

import com.checkout.payment.gateway.model.Payment;
import com.checkout.payment.gateway.model.PostPaymentResponse;

public class PaymentMapper {
  public static PostPaymentResponse toResponse(Payment payment) {
    if (payment == null) {
      return null;
    }
    return PostPaymentResponse.builder()
        .id(payment.getId())
        .cardNumberLastFour(payment.getCardNumberLastFour())
        .amount(payment.getAmount())
        .currency(payment.getCurrency())
        .expiryMonth(payment.getExpiryMonth())
        .expiryYear(payment.getExpiryYear())
        .status(payment.getStatus())
        .build();
  }
}
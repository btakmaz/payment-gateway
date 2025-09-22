package com.checkout.payment.gateway.dto;

import com.checkout.payment.gateway.domain.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PostPaymentResponse(
    UUID id,
    PaymentStatus status,
    String cardNumberLastFour,
    Integer expiryMonth,
    Integer expiryYear,
    String currency,
    Integer amount
) {}

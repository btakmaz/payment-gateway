package com.checkout.payment.gateway.dto;

import com.checkout.payment.gateway.domain.PaymentStatus;
import java.util.UUID;

public record GetPaymentResponse(
    UUID id,
    PaymentStatus status,
    String cardNumberLastFour,
    int expiryMonth,
    int expiryYear,
    String currency,
    int amount
) {}

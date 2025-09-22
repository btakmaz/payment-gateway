package com.checkout.payment.gateway.dto;

import com.checkout.payment.gateway.domain.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    String message,
    String code,
    String status
) {
    public ErrorResponse(String message) {
        this(message, null, null);
    }
    
    public static ErrorResponse rejected(String message, String code) {
        return new ErrorResponse(message, code, PaymentStatus.REJECTED.getName());
    }
}
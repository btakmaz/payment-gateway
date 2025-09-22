package com.checkout.payment.gateway.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;

public record PostPaymentRequest(
    @NotNull(message = "Card number is required")
    @Pattern(regexp = "^\\d{14,19}$", message = "Card number must be 14-19 digits long and contain only numeric characters")
    String cardNumber,
    @NotNull(message = "Expiry month is required")
    @Min(value = 1, message = "Expiry month must be between 1 and 12")
    @Max(value = 12, message = "Expiry month must be between 1 and 12")
    Integer expiryMonth,
    @NotNull(message = "Expiry year is required")
    Integer expiryYear,
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^(USD|EUR|GBP)$", message = "Currency must be one of: USD, EUR, GBP")
    String currency,
    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be a positive integer")
    Integer amount,
    @NotNull(message = "CVV is required")
    @Pattern(regexp = "^\\d{3,4}$", message = "CVV must be 3-4 digits long and contain only numeric characters")
    String cvv
) implements Serializable {}
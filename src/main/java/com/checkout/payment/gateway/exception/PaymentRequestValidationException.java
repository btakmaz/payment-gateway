package com.checkout.payment.gateway.exception;

public class PaymentRequestValidationException extends RuntimeException{
  public PaymentRequestValidationException(String message) {
    super(message);
  }
}

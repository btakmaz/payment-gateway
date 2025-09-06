package com.checkout.payment.gateway.exception;

public class PaymentRequestValiationException extends RuntimeException{
  public PaymentRequestValiationException(String message) {
    super(message);
  }
}

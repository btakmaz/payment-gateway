package com.checkout.payment.gateway.exception;

public class BankServiceException extends RuntimeException {
  public BankServiceException(String message) {
    super(message);
  }
}

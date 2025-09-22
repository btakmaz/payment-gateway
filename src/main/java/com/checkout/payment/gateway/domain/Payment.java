package com.checkout.payment.gateway.domain;

import com.checkout.payment.gateway.exception.ExpiredCardException;
import java.time.YearMonth;
import java.util.UUID;

public class Payment {

  private final UUID id;
  private final String cardNumberLastFour;
  private final int expiryMonth;
  private final int expiryYear;
  private final String currency;
  private final int amount;
  private PaymentStatus status;

  public Payment(UUID id, PaymentStatus status, String fullCardNumber,
      int expiryMonth, int expiryYear, String currency, int amount) {
    this.id = id;
    this.status = status;
    this.cardNumberLastFour = fullCardNumber.substring(fullCardNumber.length() - 4);
    this.expiryMonth = expiryMonth;
    this.expiryYear = expiryYear;
    this.currency = currency;
    this.amount = amount;

    if (isExpired()) {
      throw new ExpiredCardException("Card expiry date must be in the future");
    }
  }

  public UUID id() {
    return id;
  }

  public PaymentStatus status() {
    return status;
  }

  public String cardNumberLastFour() {
    return cardNumberLastFour;
  }

  public int expiryMonth() {
    return expiryMonth;
  }

  public int expiryYear() {
    return expiryYear;
  }

  public String currency() {
    return currency;
  }

  public int amount() {
    return amount;
  }

  public void processBankResponse(boolean isAuthorized) {
    if (isAuthorized) {
      this.status = PaymentStatus.AUTHORIZED;
    } else {
      this.status = PaymentStatus.DECLINED;
    }
  }

  public boolean isExpired() {
    YearMonth now = YearMonth.now();
    YearMonth expiry = YearMonth.of(expiryYear, expiryMonth);
    return !expiry.isAfter(now);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Payment payment)) {
      return false;
    }
    return id.equals(payment.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public String toString() {
    return "Payment{" +
        "id=" + id +
        ", status=" + status +
        ", cardNumberLastFour='" + cardNumberLastFour + '\'' +
        ", expiryMonth=" + expiryMonth +
        ", expiryYear=" + expiryYear +
        ", currency='" + currency + '\'' +
        ", amount=" + amount +
        '}';
  }
}
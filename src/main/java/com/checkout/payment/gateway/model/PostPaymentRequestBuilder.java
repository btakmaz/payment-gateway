package com.checkout.payment.gateway.model;

public class PostPaymentRequestBuilder {
  private String cardNumber;
  private Integer expiryMonth;
  private Integer expiryYear;
  private String currency;
  private Integer amount;
  private String cvv;

  public static PostPaymentRequestBuilder builder() {
    return new PostPaymentRequestBuilder();
  }

  public PostPaymentRequestBuilder cardNumber(String cardNumberLastFour) {
    this.cardNumber = cardNumberLastFour;
    return this;
  }

  public PostPaymentRequestBuilder expiryMonth(Integer expiryMonth) {
    this.expiryMonth = expiryMonth;
    return this;
  }

  public PostPaymentRequestBuilder expiryYear(Integer expiryYear) {
    this.expiryYear = expiryYear;
    return this;
  }

  public PostPaymentRequestBuilder currency(String currency) {
    this.currency = currency;
    return this;
  }

  public PostPaymentRequestBuilder amount(Integer amount) {
    this.amount = amount;
    return this;
  }

  public PostPaymentRequestBuilder cvv(String cvv) {
    this.cvv = cvv;
    return this;
  }

  public PostPaymentRequest build() {
    PostPaymentRequest request = new PostPaymentRequest();
    if (this.cardNumber != null) request.setCardNumber(this.cardNumber);
    if (this.expiryMonth != null) request.setExpiryMonth(this.expiryMonth);
    if (this.expiryYear != null) request.setExpiryYear(this.expiryYear);
    if (this.currency != null) request.setCurrency(this.currency);
    if (this.amount != null) request.setAmount(this.amount);
    if (this.cvv != null) request.setCvv(this.cvv);
    return request;
  }
}

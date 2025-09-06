package com.checkout.payment.gateway.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.io.Serializable;

@Data
@Builder
public class PostPaymentRequest implements Serializable {

  @JsonProperty("card_number")
  private int cardNumber;
  @JsonProperty("expiry_date")
  private String expiryDate;
  private String currency;
  private int amount;
  private int cvv;

//  public int getCardNumber() {
//    return cardNumber;
//  }
//
//  public void setCardNumber(int cardNumber) {
//    this.cardNumber = cardNumber;
//  }
//
//  public String getExpiryDate() {
//    return expiryDate;
//  }
//
//  public void setExpiryDate(String expiryDate) {
//    this.expiryDate = expiryDate;
//  }
//
//
//  public String getCurrency() {
//    return currency;
//  }
//
//  public void setCurrency(String currency) {
//    this.currency = currency;
//  }
//
//  public int getAmount() {
//    return amount;
//  }
//
//  public void setAmount(int amount) {
//    this.amount = amount;
//  }
//
//  public int getCvv() {
//    return cvv;
//  }
//
//  public void setCvv(int cvv) {
//    this.cvv = cvv;
//  }
//
//  @Override
//  public String toString() {
//    return "PostPaymentRequest{" +
//        "cardNumber=" + cardNumber +
//        ", expiryDate=" + expiryDate +
//        ", currency='" + currency + '\'' +
//        ", amount=" + amount +
//        ", cvv=" + cvv +
//        '}';
//  }
}


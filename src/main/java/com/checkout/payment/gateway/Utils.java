package com.checkout.payment.gateway;

public class Utils {
  public static String getCardNumberLastFour(String cardNumber) {
    return cardNumber.substring(cardNumber.length() - 4);
  }
}

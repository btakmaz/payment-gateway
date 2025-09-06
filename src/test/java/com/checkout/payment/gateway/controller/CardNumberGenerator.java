package com.checkout.payment.gateway.controller;

import java.util.Random;

public class CardNumberGenerator {
  private static final Random random = new Random();

  public static String generateCardNumber(int length) {
    if (length <= 0) {
      throw new IllegalArgumentException("Length must be positive");
    }

    StringBuilder cardNumber = new StringBuilder(length);

    for (int i = 0; i < length; i++) {
      int digit = random.nextInt(10); // 0-9
      cardNumber.append(digit);
    }

    return cardNumber.toString();
  }
}

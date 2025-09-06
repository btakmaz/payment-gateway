package com.checkout.payment.gateway.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.io.Serializable;

@Data
@Builder
public class PostPaymentRequest implements Serializable {
  @JsonProperty("card_number")
  private String cardNumber;
  @JsonProperty("expiry_date")
  private String expiryDate;
  private String currency;
  private int amount;
  private String cvv;
}


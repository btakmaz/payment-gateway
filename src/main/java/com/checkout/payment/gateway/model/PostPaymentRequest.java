package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import java.io.Serializable;

@Data
@Builder
public class PostPaymentRequest implements Serializable {

  @JsonProperty("card_number")
  @NotNull(message = "Card number is required")
  @Size(min = 14, max = 19, message = "Card number must be between 14 and 19 digits")
  @Pattern(regexp = "\\d+", message = "Card number must only contain numeric characters")
  private String cardNumber;

  @JsonProperty("expiry_month")
  @NotNull(message = "Expiry month is required")
  @Min(value = 1, message = "Expiry month must be between 1 and 12")
  @Max(value = 12, message = "Expiry month must be between 1 and 12")
  private int expiryMonth;

  @JsonProperty("expiry_year")
  @NotNull(message = "Expiry year is required")
  private int expiryYear;

  @NotNull(message = "Currency is required")
  @Size(min = 3, max = 3, message = "Currency must be 3 characters")
  private String currency;

  @NotNull(message = "Amount is required")
  @Min(value = 1, message = "Amount must be greater than 0")
  private int amount;

  @NotNull(message = "CVV is required")
  @Size(min = 3, max = 4, message = "CVV must be 3 or 4 digits")
  @Pattern(regexp = "\\d+", message = "CVV must only contain numeric characters")
  private String cvv;
}

package com.checkout.payment.gateway.client.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostPaymentResponse {
  private Boolean authorized;
  private String authorizationCode;
}

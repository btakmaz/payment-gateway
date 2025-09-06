package com.checkout.payment.gateway.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ErrorResponse {
  private List<String> errors;
}
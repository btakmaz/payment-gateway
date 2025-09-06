package com.checkout.payment.gateway.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ErrorsResponse {
  private List<String> errors;
}
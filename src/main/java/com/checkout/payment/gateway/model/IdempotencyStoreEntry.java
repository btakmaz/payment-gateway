package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentRequestStatus;
import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class IdempotencyStoreEntry {
  private UUID idempotencyKey;
  private PostPaymentRequest request;
  private PostPaymentResponse response;
  private PaymentRequestStatus status;
}

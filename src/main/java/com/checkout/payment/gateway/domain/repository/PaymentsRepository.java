package com.checkout.payment.gateway.domain.repository;

import com.checkout.payment.gateway.domain.Payment;
import java.util.Optional;
import java.util.UUID;

public interface PaymentsRepository {
  void add(Payment payment);
  Optional<Payment> get(UUID id);
}




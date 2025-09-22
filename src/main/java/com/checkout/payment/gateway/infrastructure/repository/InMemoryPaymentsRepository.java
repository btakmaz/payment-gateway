package com.checkout.payment.gateway.infrastructure.repository;

import com.checkout.payment.gateway.domain.Payment;
import com.checkout.payment.gateway.domain.repository.PaymentsRepository;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryPaymentsRepository implements PaymentsRepository {

  private final HashMap<UUID, Payment> payments = new HashMap<>();

  @Override
  public void add(Payment payment) {
    payments.put(payment.id(), payment);
  }

  @Override
  public Optional<Payment> get(UUID id) {
    return Optional.ofNullable(payments.get(id));
  }

}

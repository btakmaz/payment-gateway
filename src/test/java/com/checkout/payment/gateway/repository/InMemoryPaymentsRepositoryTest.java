package com.checkout.payment.gateway.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.checkout.payment.gateway.domain.Payment;
import com.checkout.payment.gateway.domain.repository.PaymentsRepository;
import com.checkout.payment.gateway.domain.PaymentStatus;
import java.util.Optional;
import java.util.UUID;
import com.checkout.payment.gateway.infrastructure.repository.InMemoryPaymentsRepository;
import org.junit.jupiter.api.Test;

class InMemoryPaymentsRepositoryTest {

  @Test
  void addAndGetByIdReturnsSamePayment() {
    PaymentsRepository repo = new InMemoryPaymentsRepository();
    UUID id = UUID.randomUUID();
    Payment expected = new Payment(id, PaymentStatus.AUTHORIZED, "1234", 8, 2030, "USD", 100);

    repo.add(expected);

    Optional<Payment> found = repo.get(id);
    assertTrue(found.isPresent());
    assertEquals(expected.id(), found.get().id());
    assertEquals(expected.status(), found.get().status());
    assertEquals(expected.cardNumberLastFour(), found.get().cardNumberLastFour());
    assertEquals(expected.expiryMonth(), found.get().expiryMonth());
    assertEquals(expected.expiryYear(), found.get().expiryYear());
    assertEquals(expected.currency(), found.get().currency());
    assertEquals(expected.amount(), found.get().amount());
  }

  @Test
  void getUnknownIdReturnsEmpty() {
    PaymentsRepository repo = new InMemoryPaymentsRepository();
    Optional<Payment> found = repo.get(UUID.randomUUID());
    assertTrue(found.isEmpty());
  }
}




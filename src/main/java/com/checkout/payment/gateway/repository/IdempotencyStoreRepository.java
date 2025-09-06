package com.checkout.payment.gateway.repository;

import com.checkout.payment.gateway.model.IdempotencyStoreEntry;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class IdempotencyStoreRepository {

  private final ConcurrentHashMap<UUID, IdempotencyStoreEntry> idempotencyStore = new ConcurrentHashMap<>();

  public void add(IdempotencyStoreEntry idempotencyStoreEntry) {
    idempotencyStore.put(idempotencyStoreEntry.getIdempotencyKey(), idempotencyStoreEntry);
  }

  public IdempotencyStoreEntry get(UUID idempotencyKey) {
    return idempotencyStore.get(idempotencyKey);
  }
}

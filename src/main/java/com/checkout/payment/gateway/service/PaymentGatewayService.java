package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.Utils;
import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.mapper.PaymentMapper;
import com.checkout.payment.gateway.model.IdempotencyStoreEntry;
import com.checkout.payment.gateway.model.Payment;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.IdempotencyStoreRepository;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.server.ResponseStatusException;

import static com.checkout.payment.gateway.model.PaymentRequestStatus.FAILED;
import static com.checkout.payment.gateway.model.PaymentRequestStatus.IN_PROGRESS;
import static com.checkout.payment.gateway.model.PaymentRequestStatus.SUCCESS;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final IdempotencyStoreRepository idempotencyStoreRepository;
  private final BankClient bankClient;

  private final ConcurrentHashMap<UUID, ReentrantLock> idempotencyLocks = new ConcurrentHashMap<>();

  public PaymentGatewayService(PaymentsRepository paymentsRepository, IdempotencyStoreRepository idempotencyStoreRepository, BankClient bankClient) {
    this.paymentsRepository = paymentsRepository;
    this.idempotencyStoreRepository = idempotencyStoreRepository;
    this.bankClient = bankClient;
  }

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    return paymentsRepository.get(id)
        .map(PaymentMapper::toResponse)
        .orElseThrow(() -> new PaymentNotFoundException("Invalid ID"));
  }

  public PostPaymentResponse processPayment(
      UUID idempotencyKey,
      PostPaymentRequest paymentRequest) {

    var idempotencyLock = idempotencyLocks.computeIfAbsent(idempotencyKey,
        k -> new ReentrantLock());

    idempotencyLock.lock();

    try {
      var existingIdempotencyStoreEntry = idempotencyStoreRepository.get(idempotencyKey);

      if (existingIdempotencyStoreEntry != null) {
        if (!existingIdempotencyStoreEntry.getRequest().equals(paymentRequest)) {
          throw new ResponseStatusException(
              HttpStatus.UNPROCESSABLE_ENTITY,
              "Idempotency key already used with different request"
          );
        }

        if (existingIdempotencyStoreEntry.getStatus() == SUCCESS) {
          return existingIdempotencyStoreEntry.getResponse();
        }

        if (existingIdempotencyStoreEntry.getStatus() == IN_PROGRESS) {
          throw new ResponseStatusException(
              HttpStatus.CONFLICT,
              "Request is already in progress"
          );
        }
      }

      var idempotencyStoreEntryBuilder = IdempotencyStoreEntry.builder()
          .idempotencyKey(idempotencyKey)
          .request(paymentRequest);

      idempotencyStoreRepository.add(idempotencyStoreEntryBuilder
          .status(IN_PROGRESS)
          .build());

      UUID paymentId = UUID.randomUUID();

      YearMonth expiryDate = YearMonth.of(
          paymentRequest.getExpiryYear(),
          paymentRequest.getExpiryMonth()
      );
      DateTimeFormatter expiryDateFormatter = DateTimeFormatter.ofPattern("MM/yyyy");

      try {
        var postPaymentBankResponse = bankClient.postPayment(
            com.checkout.payment.gateway.client.model.PostPaymentRequest.builder()
                .cardNumber(paymentRequest.getCardNumber())
                .amount(paymentRequest.getAmount())
                .expiryDate(expiryDate.format(expiryDateFormatter))
                .cvv(paymentRequest.getCvv())
                .currency(paymentRequest.getCurrency())
                .build());

        LOG.info("Payment processed successfully {}", postPaymentBankResponse);

        var payment = Payment.builder()
            .cardNumberLastFour(Utils.getCardNumberLastFour(paymentRequest.getCardNumber()))
            .id(paymentId)
            .amount(paymentRequest.getAmount())
            .currency(paymentRequest.getCurrency())
            .expiryMonth(paymentRequest.getExpiryMonth())
            .expiryYear(paymentRequest.getExpiryYear())
            .status(postPaymentBankResponse.getAuthorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED)
            .build();

        var idempotencyStoreEntry = idempotencyStoreEntryBuilder
            .status(SUCCESS)
            .response(PaymentMapper.toResponse(payment))
            .build();

        paymentsRepository.add(payment);
        idempotencyStoreRepository.add(idempotencyStoreEntry);

        return idempotencyStoreEntry.getResponse();

      } catch (HttpClientErrorException | HttpServerErrorException ex) {
        idempotencyStoreRepository.add(idempotencyStoreEntryBuilder
            .status(FAILED)
            .build());
        return null;
      }

    } finally {
      idempotencyLock.unlock();
    }
  }
}

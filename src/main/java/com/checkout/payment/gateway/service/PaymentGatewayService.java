package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.mapper.PaymentMapper;
import com.checkout.payment.gateway.model.Payment;
import com.checkout.payment.gateway.model.PaymentRequestStatus;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static com.checkout.payment.gateway.model.PaymentRequestStatus.FAILED;
import static com.checkout.payment.gateway.model.PaymentRequestStatus.IN_PROGRESS;
import static com.checkout.payment.gateway.model.PaymentRequestStatus.SUCCESS;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final BankClient bankClient;
  private final ConcurrentHashMap<UUID, Pair<UUID, PaymentRequestStatus>> idempotencyKeys = new ConcurrentHashMap<>();

  public PaymentGatewayService(PaymentsRepository paymentsRepository, BankClient bankClient) {
    this.paymentsRepository = paymentsRepository;
    this.bankClient = bankClient;
  }

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    return paymentsRepository.get(id)
        .map(PaymentMapper::toResponse)
        .orElseThrow(() -> new PaymentNotFoundException("Invalid ID"));
  }

  public PostPaymentResponse processPayment(UUID idempotencyKey, PostPaymentRequest paymentRequest) {
    var result = idempotencyKeys.compute(idempotencyKey, (key, existing) -> {
      // TODO: handle here
      if (existing != null) {
        switch (existing.getValue()) {
          case SUCCESS -> {
            return existing;
          }
          case IN_PROGRESS -> throw new IllegalStateException("Already in progress");
          case FAILED -> {
          }
        }
      }

      UUID paymentId = UUID.randomUUID();

      YearMonth expiryDate = YearMonth.of(
          paymentRequest.getExpiryYear(),
          paymentRequest.getExpiryMonth()
      );
      DateTimeFormatter expiryDateFormatter = DateTimeFormatter.ofPattern("MM/yyyy");

      LOG.info("Calling bank");
      var postPaymentBankResponse = bankClient.postPayment(
          com.checkout.payment.gateway.client.model.PostPaymentRequest.builder()
              .amount(paymentRequest.getAmount())
              .expiryDate(expiryDate.format(expiryDateFormatter))
              .cvv(paymentRequest.getCvv())
              .currency(paymentRequest.getCurrency())
              .build());

      LOG.info("Payment processed successfully {}", postPaymentBankResponse);

      paymentsRepository.add(Payment.builder()
          .cardNumberLastFour(paymentRequest.getCardNumber())
          .id(paymentId)
          .amount(paymentRequest.getAmount())
          .currency(paymentRequest.getCurrency())
          .expiryMonth(paymentRequest.getExpiryMonth())
          .expiryYear(paymentRequest.getExpiryYear())
          .status(PaymentStatus.AUTHORIZED)
          .build()
      );

      return Pair.of(paymentId, SUCCESS);
    });

    return paymentsRepository.get(result.getKey())
        .map(PaymentMapper::toResponse)
        .orElseThrow(() -> new PaymentNotFoundException("Invalid ID"));

  }
}

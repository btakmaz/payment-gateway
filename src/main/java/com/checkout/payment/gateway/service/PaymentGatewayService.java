package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.domain.Payment;
import com.checkout.payment.gateway.infrastructure.external.BankClient;
import com.checkout.payment.gateway.infrastructure.external.dto.BankPaymentResponse;
import com.checkout.payment.gateway.dto.GetPaymentResponse;
import com.checkout.payment.gateway.dto.PostPaymentRequest;
import com.checkout.payment.gateway.dto.PostPaymentResponse;
import com.checkout.payment.gateway.domain.repository.PaymentsRepository;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.domain.PaymentStatus;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final BankClient bankClient;

  public PaymentGatewayService(PaymentsRepository paymentsRepository, BankClient bankClient) {
    this.paymentsRepository = paymentsRepository;
    this.bankClient = bankClient;
  }

  public GetPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to payment with ID {}", id);
    Payment payment = paymentsRepository.get(id)
        .orElseThrow(() -> new PaymentNotFoundException("Invalid ID"));
    return toGetPaymentResponse(payment);
  }

  public PostPaymentResponse processPayment(PostPaymentRequest paymentRequest) {
    UUID paymentId = UUID.randomUUID();
    
    Payment payment = new Payment(
        paymentId,
        PaymentStatus.AUTHORIZED,
        paymentRequest.cardNumber(),
        paymentRequest.expiryMonth(),
        paymentRequest.expiryYear(),
        paymentRequest.currency(),
        paymentRequest.amount()
    );
    
    BankPaymentResponse bankResponse = bankClient.processPayment(paymentRequest);

    payment.processBankResponse(bankResponse.authorized());
    
    paymentsRepository.add(payment);
    
    return toPostPaymentResponse(payment);
  }

  private GetPaymentResponse toGetPaymentResponse(Payment payment) {
    return new GetPaymentResponse(
        payment.id(),
        payment.status(),
        payment.cardNumberLastFour(),
        payment.expiryMonth(),
        payment.expiryYear(),
        payment.currency(),
        payment.amount()
    );
  }

  private PostPaymentResponse toPostPaymentResponse(Payment payment) {
    return new PostPaymentResponse(
        payment.id(),
        payment.status(),
        payment.cardNumberLastFour(),
        payment.expiryMonth(),
        payment.expiryYear(),
        payment.currency(),
        payment.amount()
    );
  }
}
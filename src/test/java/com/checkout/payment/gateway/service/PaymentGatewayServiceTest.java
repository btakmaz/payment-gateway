package com.checkout.payment.gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.domain.Payment;
import com.checkout.payment.gateway.domain.PaymentStatus;
import com.checkout.payment.gateway.domain.repository.PaymentsRepository;
import com.checkout.payment.gateway.dto.GetPaymentResponse;
import com.checkout.payment.gateway.dto.PostPaymentRequest;
import com.checkout.payment.gateway.dto.PostPaymentResponse;
import com.checkout.payment.gateway.exception.BankServiceException;
import com.checkout.payment.gateway.exception.ExpiredCardException;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.infrastructure.external.BankClient;
import com.checkout.payment.gateway.infrastructure.external.dto.BankPaymentResponse;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {

  @Mock
  private PaymentsRepository paymentsRepository;
  @Mock
  private BankClient bankClient;

  private PaymentGatewayService service;

  @BeforeEach
  void setUp() {
    service = new PaymentGatewayService(paymentsRepository, bankClient);
  }

  @Test
  void whenPaymentExistsThenReturnsPayment() {
    UUID id = UUID.randomUUID();
    Payment payment = createTestPayment(id, PaymentStatus.AUTHORIZED);
    when(paymentsRepository.get(id)).thenReturn(Optional.of(payment));

    GetPaymentResponse resp = service.getPaymentById(id);

    assertEquals(id, resp.id());
    assertEquals(PaymentStatus.AUTHORIZED, resp.status());
    assertEquals("3457", resp.cardNumberLastFour());
    assertEquals(12, resp.expiryMonth());
    assertEquals(2028, resp.expiryYear());
    assertEquals("USD", resp.currency());
    assertEquals(1500, resp.amount());
  }

  @Test
  void whenPaymentMissingThenThrowsPaymentNotFoundException() {
    UUID id = UUID.randomUUID();
    when(paymentsRepository.get(id)).thenReturn(Optional.empty());

    assertThrows(PaymentNotFoundException.class, () -> service.getPaymentById(id));
  }

  @Test
  void whenProcessPaymentWithAuthorizedBankResponseThenReturnsAuthorizedPayment() {
    PostPaymentRequest paymentRequest = createTestPaymentRequest();
    BankPaymentResponse bankResponse = createAuthorizedBankResponse();

    when(bankClient.processPayment(any(PostPaymentRequest.class))).thenReturn(bankResponse);

    PostPaymentResponse response = service.processPayment(paymentRequest);

    assertEquals(PaymentStatus.AUTHORIZED, response.status());
    assertEquals("3457", response.cardNumberLastFour());
    assertEquals(paymentRequest.expiryMonth(), response.expiryMonth());
    assertEquals(paymentRequest.expiryYear(), response.expiryYear());
    assertEquals(paymentRequest.currency(), response.currency());
    assertEquals(paymentRequest.amount(), response.amount());
    verify(paymentsRepository).add(any(Payment.class));
  }

  @Test
  void whenProcessPaymentWithDeclinedBankResponseThenReturnsDeclinedPayment() {
    PostPaymentRequest paymentRequest = createTestPaymentRequest();
    BankPaymentResponse bankResponse = createDeclinedBankResponse();

    when(bankClient.processPayment(any(PostPaymentRequest.class))).thenReturn(bankResponse);

    PostPaymentResponse response = service.processPayment(paymentRequest);

    assertEquals(PaymentStatus.DECLINED, response.status());
    assertEquals("3457", response.cardNumberLastFour());
    verify(paymentsRepository).add(any(Payment.class));
  }

  @Test
  void whenProcessPaymentWithExpiredCardThenThrowsExpiredCardException() {
    PostPaymentRequest expiredPaymentRequest = createExpiredPaymentRequest();

    assertThrows(ExpiredCardException.class, () -> service.processPayment(expiredPaymentRequest));
    verify(paymentsRepository, never()).add(any(Payment.class));
  }

  @Test
  void whenProcessPaymentWithBankServiceExceptionThenThrowsBankServiceException() {
    PostPaymentRequest paymentRequest = createTestPaymentRequest();
    when(bankClient.processPayment(any(PostPaymentRequest.class)))
        .thenThrow(new BankServiceException("Bank service unavailable"));

    assertThrows(BankServiceException.class, () -> service.processPayment(paymentRequest));
    verify(paymentsRepository, never()).add(any(Payment.class));
  }

  @Test
  void whenProcessPaymentThenPaymentIsStoredWithCorrectData() {
    PostPaymentRequest paymentRequest = createTestPaymentRequest();
    BankPaymentResponse bankResponse = createAuthorizedBankResponse();

    when(bankClient.processPayment(any(PostPaymentRequest.class))).thenReturn(bankResponse);
    ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

    service.processPayment(paymentRequest);

    verify(paymentsRepository).add(paymentCaptor.capture());
    Payment capturedPayment = paymentCaptor.getValue();

    assertEquals(PaymentStatus.AUTHORIZED, capturedPayment.status());
    assertEquals("3457", capturedPayment.cardNumberLastFour());
    assertEquals(paymentRequest.expiryMonth(), capturedPayment.expiryMonth());
    assertEquals(paymentRequest.expiryYear(), capturedPayment.expiryYear());
    assertEquals(paymentRequest.currency(), capturedPayment.currency());
    assertEquals(paymentRequest.amount(), capturedPayment.amount());
  }

  private PostPaymentRequest createTestPaymentRequest() {
    return new PostPaymentRequest(
        "1234567890123457",
        12,
        2028,
        "USD",
        1500,
        "123"
    );
  }

  private PostPaymentRequest createExpiredPaymentRequest() {
    YearMonth pastDate = YearMonth.now().minusMonths(1);
    return new PostPaymentRequest(
        "1234567890123456",
        pastDate.getMonthValue(),
        pastDate.getYear(),
        "USD",
        1500,
        "123"
    );
  }

  private BankPaymentResponse createAuthorizedBankResponse() {
    return new BankPaymentResponse(true, UUID.randomUUID().toString());
  }

  private BankPaymentResponse createDeclinedBankResponse() {
    return new BankPaymentResponse(false, UUID.randomUUID().toString());
  }

  private Payment createTestPayment(UUID id, PaymentStatus status) {
    return new Payment(
        id,
        status,
        "1234567890123457",
        12,
        2028,
        "USD",
        1500
    );
  }
}
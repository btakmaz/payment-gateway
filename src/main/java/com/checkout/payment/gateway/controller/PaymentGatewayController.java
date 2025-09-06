package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.exception.PaymentRequestValidationException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentGatewayController {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayController.class);
  private static final List<String> ALLOWED_CURRENCIES = Arrays.asList("USD", "EUR", "GBP");

  private final PaymentGatewayService paymentGatewayService;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService) {
    this.paymentGatewayService = paymentGatewayService;
  }

  @GetMapping("/{id}")
  public ResponseEntity<PostPaymentResponse> getPaymentById(@PathVariable UUID id) {
    return new ResponseEntity<>(paymentGatewayService.getPaymentById(id), HttpStatus.OK);
  }

  @PostMapping
  // TODO: validate idempotency key header
  public ResponseEntity<PostPaymentResponse> postPayment(
      @Valid @RequestHeader("Idempotency-Key") UUID idempotencyKey,
      @Valid @RequestBody PostPaymentRequest request) {
    LOG.debug("Received payment request {}", request);

    if (!ALLOWED_CURRENCIES.contains(request.getCurrency())) {
      throw new PaymentRequestValidationException("Currency must be one of: " + ALLOWED_CURRENCIES);
    }

    YearMonth expiry = YearMonth.of(request.getExpiryYear(), request.getExpiryMonth());
    if (expiry.isBefore(YearMonth.now())) {
      throw new PaymentRequestValidationException("Card expiry date must be in the future");
    }

    PostPaymentResponse response = paymentGatewayService.processPayment(idempotencyKey, request);
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }
}

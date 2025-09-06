package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.service.PaymentGatewayService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentGatewayController {
  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayController.class);

  private final PaymentGatewayService paymentGatewayService;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService) {
    this.paymentGatewayService = paymentGatewayService;
  }

  @GetMapping("/{id}")
  public ResponseEntity<PostPaymentResponse> getPaymentById(@PathVariable UUID id) {
    return new ResponseEntity<>(paymentGatewayService.getPaymentById(id), HttpStatus.OK);
  }

  @PostMapping
  public ResponseEntity<PostPaymentResponse> postPayment(@Valid @RequestBody PostPaymentRequest request) {
    LOG.info("Received payment request {}", request);
    PostPaymentResponse response = paymentGatewayService.processPayment(request);
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }
}

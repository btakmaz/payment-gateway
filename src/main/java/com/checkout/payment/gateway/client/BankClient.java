package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.client.model.PostPaymentRequest;
import com.checkout.payment.gateway.client.model.PostPaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class BankClient {
  private final String baseUrl;
  private final RestTemplate restTemplate;

  BankClient(
      @Value("${bank.base-url}") String baseUrl,
      RestTemplateBuilder builder
  ) {
    this.baseUrl = baseUrl;
    this.restTemplate = builder.build();
  }

  PostPaymentResponse postPayment(PostPaymentRequest request) {
    return restTemplate.postForObject(
        baseUrl + "/payments",
        request,
        PostPaymentResponse.class
    );
  }
}

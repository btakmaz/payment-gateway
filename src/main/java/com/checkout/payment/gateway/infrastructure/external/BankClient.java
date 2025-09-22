package com.checkout.payment.gateway.infrastructure.external;

import com.checkout.payment.gateway.dto.PostPaymentRequest;
import com.checkout.payment.gateway.exception.BankServiceException;
import com.checkout.payment.gateway.infrastructure.external.dto.BankPaymentRequest;
import com.checkout.payment.gateway.infrastructure.external.dto.BankPaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class BankClient {
  private final RestTemplate restTemplate;
  private final String bankApiUrl;

  public BankClient(RestTemplate restTemplate, @Value("${bank.api.url:http://localhost:8080/payments}") String bankApiUrl) {
    this.restTemplate = restTemplate;
    this.bankApiUrl = bankApiUrl;
  }

  public BankPaymentResponse processPayment(PostPaymentRequest request) {
    try {
      BankPaymentRequest bankRequest = createBankRequest(request);
      
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      HttpEntity<BankPaymentRequest> entity = new HttpEntity<>(bankRequest, headers);
      ResponseEntity<BankPaymentResponse> response = restTemplate.exchange(
          bankApiUrl,
          HttpMethod.POST,
          entity,
          BankPaymentResponse.class
      );

      if (response.getStatusCode() == HttpStatus.OK) {
        return response.getBody();
      } else if (response.getStatusCode() == HttpStatus.BAD_REQUEST) {
        throw new BankServiceException("Bad request from the client");
      } else if (response.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
        throw new BankServiceException("Bank service is unavailable");
      } else {
        throw new BankServiceException("Unexpected response code: " + response.getStatusCode());
      }
    } catch (Exception e) {
      throw new BankServiceException("Bank service error");
    }
  }

  private BankPaymentRequest createBankRequest(PostPaymentRequest request) {
    return new BankPaymentRequest(
        request.cardNumber(),
        formatExpiryDate(request.expiryMonth(), request.expiryYear()),
        request.currency(),
        request.amount(),
        request.cvv()
    );
  }

  private String formatExpiryDate(int month, int year) {
    return String.format("%02d/%02d", month, year % 100);
  }
}
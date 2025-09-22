package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.dto.ErrorResponse;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  @ExceptionHandler(PaymentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handlePaymentNotFound(PaymentNotFoundException ex) {
    LOG.error("Payment not found: {}", ex.getMessage(), ex);
    return new ResponseEntity<>(new ErrorResponse("Payment not found"), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(BankServiceException.class)
  public ResponseEntity<ErrorResponse> handleBankServiceException(BankServiceException ex) {
    LOG.error("Bank service error: {}", ex.getMessage(), ex);
    return new ResponseEntity<>(new ErrorResponse("Payment gateway is unavailable"),
        HttpStatus.SERVICE_UNAVAILABLE);
  }

  @ExceptionHandler(ExpiredCardException.class)
  public ResponseEntity<ErrorResponse> handleExpiredCard(ExpiredCardException ex) {
    LOG.warn("Expired card: {}", ex.getMessage());
    return new ResponseEntity<>(
        ErrorResponse.rejected("Card expiry date must be in the future", "EXPIRED_CARD"),
        HttpStatus.BAD_REQUEST
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex) {
    LOG.warn("Validation failed: {}", ex.getMessage());

    String errorMessage = buildValidationErrorMessage(ex);
    return new ResponseEntity<>(
        ErrorResponse.rejected("Validation failed: " + errorMessage, "VALIDATION_ERROR"),
        HttpStatus.BAD_REQUEST
    );
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleInvalidJson(HttpMessageNotReadableException ex) {
    LOG.warn("Invalid JSON format: {}", ex.getMessage());
    return new ResponseEntity<>(
        ErrorResponse.rejected("Invalid JSON format", "INVALID_JSON"),
        HttpStatus.BAD_REQUEST
    );
  }

  private String buildValidationErrorMessage(MethodArgumentNotValidException ex) {
    return ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .collect(Collectors.joining(", "));
  }
}
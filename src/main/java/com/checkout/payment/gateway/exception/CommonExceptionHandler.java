package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.List;

@ControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  @ExceptionHandler(PaymentRequestValiationException.class)
  public ResponseEntity<ErrorResponse> handleResponseStatusException(PaymentRequestValiationException ex) {
    var errorResponse = ErrorResponse.builder()
        .errors(List.of(ex.getMessage()))
        .build();
    return ResponseEntity
        .status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(errorResponse);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handle(MethodArgumentNotValidException ex) {

    List<String> errors = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(FieldError::getDefaultMessage)
        .toList();

    var errorResponse = ErrorResponse.builder()
        .errors(errors)
        .build();

    HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;

    return ResponseEntity.status(status).body(errorResponse);
  }
}

package com.marcusprado02.commons.adapters.web.spring.exception;

import com.marcusprado02.commons.adapters.web.problem.HttpProblemMapper;
import com.marcusprado02.commons.adapters.web.problem.HttpProblemResponse;
import com.marcusprado02.commons.kernel.errors.DomainException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global exception handler that maps exceptions to RFC 7807 Problem Detail responses.
 *
 * <p>Handles:
 *
 * <ul>
 *   <li>Domain exceptions via {@link DomainException}
 *   <li>Validation errors via {@link MethodArgumentNotValidException}
 *   <li>HTTP protocol errors (method not allowed, not found, etc.)
 *   <li>Unexpected errors with generic 500 response
 * </ul>
 */
@RestControllerAdvice
public class ProblemExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ProblemExceptionHandler.class);

  private final HttpProblemMapper mapper;

  public ProblemExceptionHandler(HttpProblemMapper mapper) {
    this.mapper = mapper;
  }

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<HttpProblemResponse> handle(DomainException ex) {
    var response = mapper.map(ex.problem());
    return ResponseEntity.status(response.status()).body(response);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<HttpProblemResponse> handle(MethodArgumentNotValidException ex) {
    Map<String, Object> details = new HashMap<>();
    Map<String, String> fieldErrors = new HashMap<>();

    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      fieldErrors.put(error.getField(), error.getDefaultMessage());
    }

    details.put("errors", fieldErrors);

    var response =
        new HttpProblemResponse(
            HttpStatus.BAD_REQUEST.value(),
            "VALIDATION_ERROR",
            "Validation failed for one or more fields",
            details,
            null);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<HttpProblemResponse> handle(HttpMessageNotReadableException ex) {
    var response =
        HttpProblemResponse.of(
            HttpStatus.BAD_REQUEST.value(), "MALFORMED_REQUEST", "Request body is malformed");

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<HttpProblemResponse> handle(HttpRequestMethodNotSupportedException ex) {
    Map<String, Object> details = new HashMap<>();
    if (ex.getSupportedHttpMethods() != null && !ex.getSupportedHttpMethods().isEmpty()) {
      details.put("supportedMethods", ex.getSupportedHttpMethods());
    }

    var response =
        new HttpProblemResponse(
            HttpStatus.METHOD_NOT_ALLOWED.value(),
            "METHOD_NOT_ALLOWED",
            "HTTP method not supported",
            details,
            null);

    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<HttpProblemResponse> handle(NoResourceFoundException ex) {
    var response =
        HttpProblemResponse.of(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", "Resource not found");

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<HttpProblemResponse> handle(IllegalArgumentException ex) {
    var response =
        HttpProblemResponse.of(HttpStatus.BAD_REQUEST.value(), "INVALID_ARGUMENT", ex.getMessage());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<HttpProblemResponse> handle(Exception ex) {
    log.error("Unhandled exception", ex);

    var response =
        HttpProblemResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "INTERNAL_ERROR",
            "An unexpected error occurred");

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }
}

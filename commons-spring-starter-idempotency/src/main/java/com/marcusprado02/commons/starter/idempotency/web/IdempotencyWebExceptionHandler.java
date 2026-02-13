package com.marcusprado02.commons.starter.idempotency.web;

import com.marcusprado02.commons.starter.idempotency.exception.DuplicateIdempotencyKeyException;
import com.marcusprado02.commons.starter.idempotency.exception.IdempotencyInProgressException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class IdempotencyWebExceptionHandler {

  @ExceptionHandler(DuplicateIdempotencyKeyException.class)
  public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateIdempotencyKeyException ex) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("error", "DUPLICATE_IDEMPOTENCY_KEY");
    body.put("key", ex.getKey());
    if (ex.getResultRef() != null) {
      body.put("resultRef", ex.getResultRef());
    }

    ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.CONFLICT);
    if (ex.getResultRef() != null) {
      response.header(IdempotencyHandlerInterceptor.HEADER_RESULT_REF, ex.getResultRef());
    }
    return response.body(body);
  }

  @ExceptionHandler(IdempotencyInProgressException.class)
  public ResponseEntity<Map<String, Object>> handleInProgress(IdempotencyInProgressException ex) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("error", "IDEMPOTENCY_IN_PROGRESS");
    body.put("key", ex.getKey());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
  }
}

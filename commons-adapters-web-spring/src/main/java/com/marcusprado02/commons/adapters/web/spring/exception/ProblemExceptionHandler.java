package com.marcusprado02.commons.adapters.web.spring.exception;

import com.marcusprado02.commons.adapters.web.problem.HttpProblemMapper;
import com.marcusprado02.commons.kernel.errors.DomainException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProblemExceptionHandler {

  private final HttpProblemMapper mapper;

  public ProblemExceptionHandler(HttpProblemMapper mapper) {
    this.mapper = mapper;
  }

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<?> handle(DomainException ex) {
    var response = mapper.map(ex.problem());
    return ResponseEntity.status(response.status()).body(response);
  }
}

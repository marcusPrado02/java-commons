package com.marcusprado02.commons.adapters.web.spring.error;

import com.marcusprado02.commons.adapters.web.ApiErrorResponse;
import com.marcusprado02.commons.app.observability.ContextKeys;
import com.marcusprado02.commons.app.observability.RequestContext;
import com.marcusprado02.commons.kernel.errors.BusinessException;
import com.marcusprado02.commons.kernel.errors.ConflictException;
import com.marcusprado02.commons.kernel.errors.DomainException;
import com.marcusprado02.commons.kernel.errors.NotFoundException;
import com.marcusprado02.commons.kernel.errors.TechnicalException;
import com.marcusprado02.commons.kernel.errors.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public final class GlobalExceptionHandlerAdvice {

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(ValidationException ex) {
    return build(HttpStatus.BAD_REQUEST, DefaultErrorCodes.VALIDATION_ERROR, ex);
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex) {
    return build(HttpStatus.NOT_FOUND, DefaultErrorCodes.NOT_FOUND, ex);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex) {
    return build(HttpStatus.CONFLICT, DefaultErrorCodes.CONFLICT, ex);
  }

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex) {
    return build(HttpStatus.UNPROCESSABLE_ENTITY, DefaultErrorCodes.BUSINESS_ERROR, ex);
  }

  @ExceptionHandler(TechnicalException.class)
  public ResponseEntity<ApiErrorResponse> handleTechnical(TechnicalException ex) {
    return build(HttpStatus.INTERNAL_SERVER_ERROR, DefaultErrorCodes.TECHNICAL_ERROR, ex);
  }

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ApiErrorResponse> handleDomain(DomainException ex) {
    return build(HttpStatus.BAD_REQUEST, DefaultErrorCodes.BUSINESS_ERROR, ex);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
    return build(HttpStatus.INTERNAL_SERVER_ERROR, DefaultErrorCodes.UNEXPECTED_ERROR, ex);
  }

  private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String code, Exception ex) {
    String correlationId = RequestContext.get(ContextKeys.CORRELATION_ID);
    ApiErrorResponse payload = ApiErrorResponse.of(correlationId, code, safeMessage(ex));
    return ResponseEntity.status(status).body(payload);
  }

  private String safeMessage(Exception ex) {
    // Em produção você pode trocar por mensagem genérica para "Exception.class".
    String msg = ex.getMessage();
    return (msg == null || msg.isBlank()) ? ex.getClass().getSimpleName() : msg;
  }
}

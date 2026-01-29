package com.marcusprado02.commons.adapters.web;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
    Instant timestamp,
    String correlationId,
    String code,
    String message,
    List<ApiErrorDetail> details) {

  public static ApiErrorResponse of(String correlationId, String code, String message) {
    return new ApiErrorResponse(Instant.now(), correlationId, code, message, List.of());
  }

  public static ApiErrorResponse of(
      String correlationId, String code, String message, List<ApiErrorDetail> details) {
    return new ApiErrorResponse(Instant.now(), correlationId, code, message, details);
  }
}

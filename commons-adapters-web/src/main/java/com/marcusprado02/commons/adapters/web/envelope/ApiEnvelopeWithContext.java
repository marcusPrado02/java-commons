package com.marcusprado02.commons.adapters.web.envelope;

import java.time.Instant;
import java.util.Map;

/**
 * API envelope that includes context information such as correlation ID, tenant ID, actor ID,
 * timestamp, and metadata.
 *
 * @param <T> the type of the successful response data
 */
public sealed interface ApiEnvelopeWithContext
    permits ApiEnvelopeWithContext.Success, ApiEnvelopeWithContext.Failure {

  String correlationId();

  String tenantId();

  String actorId();

  Instant timestamp();

  Map<String, Object> meta();

  record Success<T>(
      T data,
      String correlationId,
      String tenantId,
      String actorId,
      Instant timestamp,
      Map<String, Object> meta)
      implements ApiEnvelopeWithContext {

    public Success {
      meta = meta == null ? Map.of() : Map.copyOf(meta);
    }

    public static <T> Success<T> of(T data, String correlationId, String tenantId, String actorId) {
      return new Success<>(data, correlationId, tenantId, actorId, Instant.now(), Map.of());
    }
  }

  record Failure(
      Object error,
      String correlationId,
      String tenantId,
      String actorId,
      Instant timestamp,
      Map<String, Object> meta)
      implements ApiEnvelopeWithContext {

    public Failure {
      meta = meta == null ? Map.of() : Map.copyOf(meta);
    }

    public static Failure of(Object error, String correlationId, String tenantId, String actorId) {
      return new Failure(error, correlationId, tenantId, actorId, Instant.now(), Map.of());
    }
  }
}

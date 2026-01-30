package com.marcusprado02.commons.adapters.web.envelope;

import java.util.Map;

public sealed interface ApiEnvelope permits ApiEnvelope.Success, ApiEnvelope.Failure {

  record Success<T>(T data, Map<String, Object> meta) implements ApiEnvelope {
    public Success {
      meta = meta == null ? Map.of() : Map.copyOf(meta);
    }

    public static <T> Success<T> of(T data) {
      return new Success<>(data, Map.of());
    }
  }

  record Failure(Object error, Map<String, Object> meta) implements ApiEnvelope {
    public Failure {
      meta = meta == null ? Map.of() : Map.copyOf(meta);
    }

    public static Failure of(Object error) {
      return new Failure(error, Map.of());
    }
  }
}

package com.marcusprado02.commons.adapters.web.envelope;

public sealed interface ApiEnvelope permits ApiEnvelope.Success, ApiEnvelope.Failure {

  ApiMeta meta();

  record Success<T>(T data, ApiMeta meta) implements ApiEnvelope {

    public Success {
      meta = meta == null ? ApiMeta.empty() : meta;
    }

    public static <T> Success<T> of(T data) {
      return new Success<>(data, ApiMeta.empty());
    }

    public static <T> Success<T> of(T data, ApiMeta meta) {
      return new Success<>(data, meta);
    }
  }

  record Failure(Object error, ApiMeta meta) implements ApiEnvelope {

    public Failure {
      meta = meta == null ? ApiMeta.empty() : meta;
    }

    public static Failure of(Object error) {
      return new Failure(error, ApiMeta.empty());
    }

    public static Failure of(Object error, ApiMeta meta) {
      return new Failure(error, meta);
    }
  }
}

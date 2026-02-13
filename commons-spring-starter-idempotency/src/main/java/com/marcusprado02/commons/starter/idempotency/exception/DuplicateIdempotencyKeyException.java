package com.marcusprado02.commons.starter.idempotency.exception;

public class DuplicateIdempotencyKeyException extends RuntimeException {

  private final String key;
  private final String resultRef;

  public DuplicateIdempotencyKeyException(String key, String resultRef) {
    super("Duplicate idempotency key: " + key);
    this.key = key;
    this.resultRef = resultRef;
  }

  public String getKey() {
    return key;
  }

  public String getResultRef() {
    return resultRef;
  }
}

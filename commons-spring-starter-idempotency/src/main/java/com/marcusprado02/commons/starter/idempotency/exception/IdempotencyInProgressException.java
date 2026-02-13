package com.marcusprado02.commons.starter.idempotency.exception;

public class IdempotencyInProgressException extends RuntimeException {

  private final String key;

  public IdempotencyInProgressException(String key) {
    super("Idempotency key is in progress: " + key);
    this.key = key;
  }

  public String getKey() {
    return key;
  }
}

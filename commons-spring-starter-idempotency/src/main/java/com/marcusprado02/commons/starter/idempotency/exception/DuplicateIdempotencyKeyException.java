package com.marcusprado02.commons.starter.idempotency.exception;

/** Thrown when an idempotency key has already been processed. */
public class DuplicateIdempotencyKeyException extends RuntimeException {

  private final String key;
  private final String resultRef;

  /**
   * Creates a new exception for the given duplicate key and optional result reference.
   *
   * @param key the duplicate idempotency key
   * @param resultRef the result reference URL, or null
   */
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

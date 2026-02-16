package com.marcusprado02.commons.app.configuration.encryption;

/** Exception thrown when encryption or decryption operations fail. */
public class EncryptionException extends Exception {

  public EncryptionException(String message) {
    super(message);
  }

  public EncryptionException(String message, Throwable cause) {
    super(message, cause);
  }
}

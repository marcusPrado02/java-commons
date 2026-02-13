package com.marcusprado02.commons.ports.secrets;

import java.util.Objects;

/**
 * Chave para identificar um secret no store.
 *
 * <p>Formato: "path/to/secret" ou "secret-name"
 */
public record SecretKey(String value) {

  public SecretKey {
    Objects.requireNonNull(value, "Secret key cannot be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException("Secret key cannot be blank");
    }
  }

  public static SecretKey of(String value) {
    return new SecretKey(value);
  }

  @Override
  public String toString() {
    return value;
  }
}

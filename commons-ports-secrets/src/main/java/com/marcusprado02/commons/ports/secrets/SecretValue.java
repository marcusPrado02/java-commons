package com.marcusprado02.commons.ports.secrets;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Valor de um secret com metadados.
 *
 * <p>Implementa AutoCloseable para permitir zeroing de dados sensíveis.
 */
public final class SecretValue implements AutoCloseable {

  private final byte[] data;
  private final String version;
  private final Instant createdAt;
  private final Instant expiresAt;
  private boolean destroyed;

  private SecretValue(byte[] data, String version, Instant createdAt, Instant expiresAt) {
    this.data = Objects.requireNonNull(data, "Secret data cannot be null");
    this.version = version;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
    this.destroyed = false;
  }

  public static SecretValue of(String value) {
    return new SecretValue(value.getBytes(StandardCharsets.UTF_8), null, Instant.now(), null);
  }

  public static SecretValue of(byte[] data) {
    return new SecretValue(Arrays.copyOf(data, data.length), null, Instant.now(), null);
  }

  public static SecretValue of(String value, String version) {
    return new SecretValue(value.getBytes(StandardCharsets.UTF_8), version, Instant.now(), null);
  }

  public static SecretValue of(byte[] data, String version, Instant createdAt, Instant expiresAt) {
    return new SecretValue(Arrays.copyOf(data, data.length), version, createdAt, expiresAt);
  }

  public String asString() {
    ensureNotDestroyed();
    return new String(data, StandardCharsets.UTF_8);
  }

  public byte[] asBytes() {
    ensureNotDestroyed();
    return Arrays.copyOf(data, data.length);
  }

  public Optional<String> version() {
    return Optional.ofNullable(version);
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Optional<Instant> expiresAt() {
    return Optional.ofNullable(expiresAt);
  }

  public boolean isExpired() {
    return expiresAt != null && Instant.now().isAfter(expiresAt);
  }

  @Override
  public void close() {
    if (!destroyed) {
      Arrays.fill(data, (byte) 0);
      destroyed = true;
    }
  }

  private void ensureNotDestroyed() {
    if (destroyed) {
      throw new IllegalStateException("Secret value has been destroyed");
    }
  }
}

package com.marcusprado02.commons.adapters.persistence.jpa.idempotency;

import com.marcusprado02.commons.app.idempotency.model.IdempotencyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "commons_idempotency")
public class IdempotencyRecordEntity {

  @Id
  @Column(name = "idem_key", length = 160, nullable = false)
  private String key;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20, nullable = false)
  private IdempotencyStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "result_ref", length = 200)
  private String resultRef;

  @Column(name = "last_error", length = 500)
  private String lastError;

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public IdempotencyStatus getStatus() {
    return status;
  }

  public void setStatus(IdempotencyStatus status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public String getResultRef() {
    return resultRef;
  }

  public void setResultRef(String resultRef) {
    this.resultRef = resultRef;
  }

  public String getLastError() {
    return lastError;
  }

  public void setLastError(String lastError) {
    this.lastError = lastError;
  }
}

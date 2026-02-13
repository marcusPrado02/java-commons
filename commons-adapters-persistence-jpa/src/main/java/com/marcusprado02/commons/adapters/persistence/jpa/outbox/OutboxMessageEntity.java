package com.marcusprado02.commons.adapters.persistence.jpa.outbox;

import com.marcusprado02.commons.app.outbox.model.OutboxStatus;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(
    name = "commons_outbox",
    indexes = {
      @Index(name = "idx_outbox_status", columnList = "status"),
      @Index(name = "idx_outbox_occurred_at", columnList = "occurredAt")
    })
public class OutboxMessageEntity {

  @Id
  @Column(name = "id", length = 64, nullable = false)
  private String id;

  @Column(name = "aggregate_type", length = 120, nullable = false)
  private String aggregateType;

  @Column(name = "aggregate_id", length = 120, nullable = false)
  private String aggregateId;

  @Column(name = "event_type", length = 180, nullable = false)
  private String eventType;

  @Column(name = "topic", length = 180, nullable = false)
  private String topic;

  @Column(name = "content_type", length = 120, nullable = false)
  private String contentType;

  @Lob
  @Basic(optional = false)
  @Column(name = "payload", nullable = false)
  private byte[] payload;

  @Lob
  @Column(name = "headers_json")
  private String headersJson;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20, nullable = false)
  private OutboxStatus status;

  @Column(name = "attempts", nullable = false)
  private int attempts;

  @Column(name = "processing_at")
  private Instant processingAt;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "last_error", length = 500)
  private String lastError;

  // getters/setters (mínimo)
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getAggregateType() {
    return aggregateType;
  }

  public void setAggregateType(String aggregateType) {
    this.aggregateType = aggregateType;
  }

  public String getAggregateId() {
    return aggregateId;
  }

  public void setAggregateId(String aggregateId) {
    this.aggregateId = aggregateId;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public byte[] getPayload() {
    return payload;
  }

  public void setPayload(byte[] payload) {
    this.payload = payload;
  }

  public String getHeadersJson() {
    return headersJson;
  }

  public void setHeadersJson(String headersJson) {
    this.headersJson = headersJson;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(Instant occurredAt) {
    this.occurredAt = occurredAt;
  }

  public OutboxStatus getStatus() {
    return status;
  }

  public void setStatus(OutboxStatus status) {
    this.status = status;
  }

  public int getAttempts() {
    return attempts;
  }

  public void setAttempts(int attempts) {
    this.attempts = attempts;
  }

  public Instant getProcessingAt() {
    return processingAt;
  }

  public void setProcessingAt(Instant processingAt) {
    this.processingAt = processingAt;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }

  public void setPublishedAt(Instant publishedAt) {
    this.publishedAt = publishedAt;
  }

  public String getLastError() {
    return lastError;
  }

  public void setLastError(String lastError) {
    this.lastError = lastError;
  }
}

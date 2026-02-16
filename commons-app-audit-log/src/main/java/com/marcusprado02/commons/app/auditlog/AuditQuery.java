package com.marcusprado02.commons.app.auditlog;

import java.time.Instant;
import java.util.Map;

/**
 * Query criteria for searching audit events.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * AuditQuery query = AuditQuery.builder()
 *     .actor("user123")
 *     .eventType("USER_UPDATED")
 *     .from(Instant.now().minus(Duration.ofDays(7)))
 *     .to(Instant.now())
 *     .limit(100)
 *     .build();
 *
 * Result<List<AuditEvent>> events = repository.query(query);
 * }</pre>
 */
public final class AuditQuery {

  private final String actor;
  private final String eventType;
  private final String action;
  private final String resourceType;
  private final String resourceId;
  private final Instant from;
  private final Instant to;
  private final String result;
  private final Map<String, Object> metadata;
  private final int limit;
  private final int offset;

  private AuditQuery(Builder builder) {
    this.actor = builder.actor;
    this.eventType = builder.eventType;
    this.action = builder.action;
    this.resourceType = builder.resourceType;
    this.resourceId = builder.resourceId;
    this.from = builder.from;
    this.to = builder.to;
    this.result = builder.result;
    this.metadata = builder.metadata != null ? Map.copyOf(builder.metadata) : Map.of();
    this.limit = builder.limit > 0 ? builder.limit : 100;
    this.offset = builder.offset >= 0 ? builder.offset : 0;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getActor() {
    return actor;
  }

  public String getEventType() {
    return eventType;
  }

  public String getAction() {
    return action;
  }

  public String getResourceType() {
    return resourceType;
  }

  public String getResourceId() {
    return resourceId;
  }

  public Instant getFrom() {
    return from;
  }

  public Instant getTo() {
    return to;
  }

  public String getResult() {
    return result;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public int getLimit() {
    return limit;
  }

  public int getOffset() {
    return offset;
  }

  public static final class Builder {
    private String actor;
    private String eventType;
    private String action;
    private String resourceType;
    private String resourceId;
    private Instant from;
    private Instant to;
    private String result;
    private Map<String, Object> metadata;
    private int limit = 100;
    private int offset = 0;

    private Builder() {}

    public Builder actor(String actor) {
      this.actor = actor;
      return this;
    }

    public Builder eventType(String eventType) {
      this.eventType = eventType;
      return this;
    }

    public Builder action(String action) {
      this.action = action;
      return this;
    }

    public Builder resourceType(String resourceType) {
      this.resourceType = resourceType;
      return this;
    }

    public Builder resourceId(String resourceId) {
      this.resourceId = resourceId;
      return this;
    }

    public Builder from(Instant from) {
      this.from = from;
      return this;
    }

    public Builder to(Instant to) {
      this.to = to;
      return this;
    }

    public Builder result(String result) {
      this.result = result;
      return this;
    }

    public Builder metadata(Map<String, Object> metadata) {
      this.metadata = metadata;
      return this;
    }

    public Builder limit(int limit) {
      this.limit = limit;
      return this;
    }

    public Builder offset(int offset) {
      this.offset = offset;
      return this;
    }

    public AuditQuery build() {
      return new AuditQuery(this);
    }
  }
}

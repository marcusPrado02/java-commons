package com.marcusprado02.commons.app.auditlog;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an audit event in the system.
 *
 * <p>Records who did what, when, and where.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * AuditEvent event = AuditEvent.builder()
 *     .eventType("USER_CREATED")
 *     .actor("user123")
 *     .action("create")
 *     .resourceType("User")
 *     .resourceId("456")
 *     .metadata(Map.of("email", "user@example.com"))
 *     .build();
 * }</pre>
 */
public final class AuditEvent {

  private final String id;
  private final String eventType;
  private final String actor;
  private final String action;
  private final String resourceType;
  private final String resourceId;
  private final Instant timestamp;
  private final String ipAddress;
  private final String userAgent;
  private final Map<String, Object> metadata;
  private final String result;
  private final String errorMessage;

  private AuditEvent(Builder builder) {
    this.id = builder.id;
    this.eventType = Objects.requireNonNull(builder.eventType, "eventType cannot be null");
    this.actor = Objects.requireNonNull(builder.actor, "actor cannot be null");
    this.action = Objects.requireNonNull(builder.action, "action cannot be null");
    this.resourceType = builder.resourceType;
    this.resourceId = builder.resourceId;
    this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
    this.ipAddress = builder.ipAddress;
    this.userAgent = builder.userAgent;
    this.metadata = builder.metadata != null ? Map.copyOf(builder.metadata) : Map.of();
    this.result = builder.result != null ? builder.result : "SUCCESS";
    this.errorMessage = builder.errorMessage;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getId() {
    return id;
  }

  public String getEventType() {
    return eventType;
  }

  public String getActor() {
    return actor;
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

  public Instant getTimestamp() {
    return timestamp;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public String getResult() {
    return result;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  @Override
  public String toString() {
    return String.format(
        "AuditEvent{eventType='%s', actor='%s', action='%s', resource='%s/%s', result='%s'}",
        eventType, actor, action, resourceType, resourceId, result);
  }

  public static final class Builder {
    private String id;
    private String eventType;
    private String actor;
    private String action;
    private String resourceType;
    private String resourceId;
    private Instant timestamp;
    private String ipAddress;
    private String userAgent;
    private Map<String, Object> metadata;
    private String result;
    private String errorMessage;

    private Builder() {}

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder eventType(String eventType) {
      this.eventType = eventType;
      return this;
    }

    public Builder actor(String actor) {
      this.actor = actor;
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

    public Builder timestamp(Instant timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder ipAddress(String ipAddress) {
      this.ipAddress = ipAddress;
      return this;
    }

    public Builder userAgent(String userAgent) {
      this.userAgent = userAgent;
      return this;
    }

    public Builder metadata(Map<String, Object> metadata) {
      this.metadata = metadata;
      return this;
    }

    public Builder result(String result) {
      this.result = result;
      return this;
    }

    public Builder errorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    public AuditEvent build() {
      return new AuditEvent(this);
    }
  }
}

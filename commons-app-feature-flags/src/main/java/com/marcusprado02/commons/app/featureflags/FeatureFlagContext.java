package com.marcusprado02.commons.app.featureflags;

import java.util.Map;
import java.util.Optional;

/**
 * Context for feature flag evaluation.
 *
 * <p>Contains user identification and custom attributes for targeting and segmentation.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * FeatureFlagContext context = FeatureFlagContext.builder()
 *     .userId("user123")
 *     .attribute("email", "user@example.com")
 *     .attribute("plan", "premium")
 *     .build();
 * }</pre>
 */
public record FeatureFlagContext(
    String userId, String sessionId, Map<String, Object> attributes) {

  public FeatureFlagContext {
    attributes = attributes != null ? Map.copyOf(attributes) : Map.of();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static FeatureFlagContext anonymous() {
    return new FeatureFlagContext(null, null, Map.of());
  }

  public static FeatureFlagContext forUser(String userId) {
    return new FeatureFlagContext(userId, null, Map.of());
  }

  public Optional<String> getUserId() {
    return Optional.ofNullable(userId);
  }

  public Optional<String> getSessionId() {
    return Optional.ofNullable(sessionId);
  }

  public Optional<Object> getAttribute(String key) {
    return Optional.ofNullable(attributes.get(key));
  }

  public static class Builder {
    private String userId;
    private String sessionId;
    private Map<String, Object> attributes = Map.of();

    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

    public Builder sessionId(String sessionId) {
      this.sessionId = sessionId;
      return this;
    }

    public Builder attributes(Map<String, Object> attributes) {
      this.attributes = attributes;
      return this;
    }

    public Builder attribute(String key, Object value) {
      this.attributes = new java.util.HashMap<>(attributes);
      this.attributes.put(key, value);
      return this;
    }

    public FeatureFlagContext build() {
      return new FeatureFlagContext(userId, sessionId, attributes);
    }
  }
}

package com.marcusprado02.commons.app.validation;

import java.util.HashMap;
import java.util.Map;

/**
 * Context for passing metadata during validation.
 *
 * <p>Allows validators to access additional information beyond the validated object.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * ValidationContext context = ValidationContext.builder()
 *     .put("userId", "123")
 *     .put("action", "update")
 *     .build();
 *
 * public class UniqueEmailValidator implements ContextualValidator<User> {
 *     @Override
 *     public ValidationResult validate(User user, ValidationContext context) {
 *         String userId = context.get("userId", String.class);
 *         // Check if email is unique for other users
 *         return ValidationResult.valid();
 *     }
 * }
 * }</pre>
 */
public final class ValidationContext {

  private final Map<String, Object> attributes;

  private ValidationContext(Map<String, Object> attributes) {
    this.attributes = Map.copyOf(attributes);
  }

  public static ValidationContext empty() {
    return new ValidationContext(Map.of());
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Gets an attribute value.
   *
   * @param key the attribute key
   * @return the value, or null if not found
   */
  public Object get(String key) {
    return attributes.get(key);
  }

  /**
   * Gets an attribute value with type casting.
   *
   * @param key the attribute key
   * @param type the expected type
   * @return the value, or null if not found
   */
  @SuppressWarnings("unchecked")
  public <T> T get(String key, Class<T> type) {
    Object value = attributes.get(key);
    if (value == null || !type.isInstance(value)) {
      return null;
    }
    return (T) value;
  }

  /**
   * Checks if an attribute exists.
   *
   * @param key the attribute key
   * @return true if exists
   */
  public boolean has(String key) {
    return attributes.containsKey(key);
  }

  /**
   * Gets all attributes.
   *
   * @return unmodifiable map of attributes
   */
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public static final class Builder {
    private final Map<String, Object> attributes = new HashMap<>();

    private Builder() {}

    public Builder put(String key, Object value) {
      attributes.put(key, value);
      return this;
    }

    public Builder putAll(Map<String, Object> attributes) {
      this.attributes.putAll(attributes);
      return this;
    }

    public ValidationContext build() {
      return new ValidationContext(attributes);
    }
  }
}

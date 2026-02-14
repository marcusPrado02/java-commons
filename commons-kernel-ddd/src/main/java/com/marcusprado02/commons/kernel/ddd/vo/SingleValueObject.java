package com.marcusprado02.commons.kernel.ddd.vo;

import java.util.Objects;

/**
 * Base abstract class for single-value Value Objects.
 *
 * <p>Provides default equals, hashCode, and toString implementations based on the wrapped value.
 *
 * <p><strong>Usage:</strong>
 *
 * <pre>{@code
 * public final class Email extends SingleValueObject<String> {
 *     public Email(String value) {
 *         super(validate(value));
 *     }
 *
 *     private static String validate(String value) {
 *         if (value == null || !value.contains("@")) {
 *             throw new IllegalArgumentException("Invalid email");
 *         }
 *         return value.trim().toLowerCase();
 *     }
 * }
 * }</pre>
 *
 * @param <T> the type of the wrapped value
 */
public abstract class SingleValueObject<T> implements ValueObject {

  private final T value;

  protected SingleValueObject(T value) {
    this.value = Objects.requireNonNull(value, "value must not be null");
  }

  public final T value() {
    return value;
  }

  @Override
  public final boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    SingleValueObject<?> that = (SingleValueObject<?>) obj;
    return Objects.equals(value, that.value);
  }

  @Override
  public final int hashCode() {
    return Objects.hashCode(value);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + value + "]";
  }
}

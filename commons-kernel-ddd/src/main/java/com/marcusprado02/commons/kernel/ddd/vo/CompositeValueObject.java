package com.marcusprado02.commons.kernel.ddd.vo;

import java.util.Objects;

/**
 * Base abstract class for composite Value Objects with multiple properties.
 *
 * <p>Provides default equals and hashCode implementations based on field reflection.
 *
 * <p><strong>Note:</strong> For most cases, prefer using Java records which are immutable by
 * default and implement equals/hashCode automatically.
 *
 * <p><strong>Usage:</strong>
 *
 * <pre>{@code
 * public final class Address extends CompositeValueObject {
 *     private final String street;
 *     private final String city;
 *     private final String zipCode;
 *
 *     public Address(String street, String city, String zipCode) {
 *         this.street = Objects.requireNonNull(street);
 *         this.city = Objects.requireNonNull(city);
 *         this.zipCode = Objects.requireNonNull(zipCode);
 *     }
 *
 *     // getters...
 * }
 * }</pre>
 *
 * <p><strong>Recommended:</strong> Use Java records instead:
 *
 * <pre>{@code
 * public record Address(String street, String city, String zipCode) implements ValueObject {
 *     public Address {
 *         Objects.requireNonNull(street, "street");
 *         Objects.requireNonNull(city, "city");
 *         Objects.requireNonNull(zipCode, "zipCode");
 *     }
 * }
 * }</pre>
 */
public abstract class CompositeValueObject implements ValueObject {

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    // Use reflection to compare all fields
    try {
      java.lang.reflect.Field[] fields = getClass().getDeclaredFields();
      for (java.lang.reflect.Field field : fields) {
        field.setAccessible(true);
        Object thisValue = field.get(this);
        Object thatValue = field.get(obj);
        if (!Objects.equals(thisValue, thatValue)) {
          return false;
        }
      }
      return true;
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Failed to compare value objects", e);
    }
  }

  @Override
  public int hashCode() {
    try {
      java.lang.reflect.Field[] fields = getClass().getDeclaredFields();
      Object[] values = new Object[fields.length];
      for (int i = 0; i < fields.length; i++) {
        fields[i].setAccessible(true);
        values[i] = fields[i].get(this);
      }
      return Objects.hash(values);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Failed to compute hash code", e);
    }
  }
}

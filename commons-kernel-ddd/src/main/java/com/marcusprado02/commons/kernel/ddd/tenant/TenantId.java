package com.marcusprado02.commons.kernel.ddd.tenant;

import com.marcusprado02.commons.kernel.ddd.id.Identifier;
import java.io.Serial;
import java.util.Objects;

/**
 * Multi-tenancy first-class. Represents the identifier of a tenant in a multi-tenant system. A
 * TenantId is a simple wrapper around a String value. It ensures that the value is neither null nor
 * blank.
 *
 * @param value the identifier value
 */
public record TenantId(String value) implements Identifier<String> {
  @Serial private static final long serialVersionUID = 1L;

  /** Validates the tenant ID value. */
  public TenantId {
    Objects.requireNonNull(value, "value");
    if (value.isBlank()) {
      throw new IllegalArgumentException("TenantId cannot be blank");
    }
  }

  public static TenantId of(String value) {
    return new TenantId(value);
  }
}

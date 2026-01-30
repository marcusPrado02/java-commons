package com.marcusprado02.commons.kernel.ddd.id;

import java.io.Serializable;

/**
 * Base contract for typed identifiers.
 *
 * <p>Use it to prevent mixing identifiers across aggregates (e.g., UserId vs OrderId), even if both
 * are backed by UUID or String.
 */
public interface Identifier<T> extends Serializable {

  T value();

  default String asString() {
    return String.valueOf(value());
  }
}

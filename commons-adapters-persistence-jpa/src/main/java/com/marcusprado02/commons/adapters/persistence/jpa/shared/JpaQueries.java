package com.marcusprado02.commons.adapters.persistence.jpa.shared;

public final class JpaQueries {

  private JpaQueries() {}

  public static int safeLimit(int value, int fallback) {
    return (value <= 0) ? fallback : value;
  }
}

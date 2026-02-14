package com.marcusprado02.commons.ports.email;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Email address value object.
 *
 * <p>Represents a validated email address.
 */
public record EmailAddress(String value) {

  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$");

  public EmailAddress {
    Objects.requireNonNull(value, "email address must not be null");
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("email address must not be blank");
    }
    if (trimmed.length() > 320) {
      throw new IllegalArgumentException("email address too long (max 320 characters)");
    }
    if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
      throw new IllegalArgumentException("invalid email address format: " + trimmed);
    }
    value = trimmed.toLowerCase();
  }

  public static EmailAddress of(String value) {
    return new EmailAddress(value);
  }

  public String localPart() {
    return value.substring(0, value.indexOf('@'));
  }

  public String domain() {
    return value.substring(value.indexOf('@') + 1);
  }
}

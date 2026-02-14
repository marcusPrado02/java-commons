package com.marcusprado02.commons.ports.email;

import java.util.Objects;

/**
 * Email subject value object.
 *
 * <p>Represents the subject line of an email.
 */
public record EmailSubject(String value) {

  public EmailSubject {
    Objects.requireNonNull(value, "subject must not be null");
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("subject must not be blank");
    }
    if (trimmed.length() > 998) {
      throw new IllegalArgumentException("subject too long (max 998 characters per RFC 5322)");
    }
    value = trimmed;
  }

  public static EmailSubject of(String value) {
    return new EmailSubject(value);
  }
}

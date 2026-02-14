package com.marcusprado02.commons.kernel.ddd.vo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SingleValueObjectTest {

  @Test
  void shouldCreateValueObject() {
    Email email = new Email("test@example.com");
    assertEquals("test@example.com", email.value());
  }

  @Test
  void shouldRejectNullValue() {
    // The validation method checks for null first and throws IllegalArgumentException
    assertThrows(IllegalArgumentException.class, () -> new Email(null));
  }

  @Test
  void shouldRejectInvalidEmail() {
    assertThrows(IllegalArgumentException.class, () -> new Email("invalid"));
  }

  @Test
  void shouldNormalizeValue() {
    Email email = new Email("  TEST@EXAMPLE.COM  ");
    assertEquals("test@example.com", email.value());
  }

  @Test
  void shouldHaveValueEquality() {
    Email email1 = new Email("test@example.com");
    Email email2 = new Email("test@example.com");
    Email email3 = new Email("other@example.com");

    assertEquals(email1, email2);
    assertNotEquals(email1, email3);
    assertEquals(email1.hashCode(), email2.hashCode());
  }

  @Test
  void shouldHaveToString() {
    Email email = new Email("test@example.com");
    assertEquals("Email[test@example.com]", email.toString());
  }

  // Example implementation
  static final class Email extends SingleValueObject<String> {
    public Email(String value) {
      super(validate(value));
    }

    private static String validate(String value) {
      if (value == null || !value.contains("@")) {
        throw new IllegalArgumentException("Invalid email");
      }
      return value.trim().toLowerCase();
    }
  }
}

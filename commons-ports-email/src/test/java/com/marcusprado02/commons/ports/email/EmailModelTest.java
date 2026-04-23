package com.marcusprado02.commons.ports.email;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EmailModelTest {

  @Test
  void emailAddress_of_valid_normalizes_to_lowercase() {
    EmailAddress a = EmailAddress.of("User@Example.COM");
    assertEquals("user@example.com", a.value());
    assertEquals("user", a.localPart());
    assertEquals("example.com", a.domain());
  }

  @Test
  void emailAddress_of_valid_with_subdomains() {
    EmailAddress a = EmailAddress.of("test.user+tag@mail.example.org");
    assertNotNull(a);
    assertEquals("mail.example.org", a.domain());
  }

  @Test
  void emailAddress_rejects_null() {
    assertThrows(NullPointerException.class, () -> EmailAddress.of(null));
  }

  @Test
  void emailAddress_rejects_blank() {
    assertThrows(IllegalArgumentException.class, () -> EmailAddress.of("   "));
  }

  @Test
  void emailAddress_rejects_invalid_format() {
    assertThrows(IllegalArgumentException.class, () -> EmailAddress.of("not-an-email"));
  }

  @Test
  void emailAddress_rejects_too_long() {
    String longEmail = "a".repeat(310) + "@x.com";
    assertThrows(IllegalArgumentException.class, () -> EmailAddress.of(longEmail));
  }

  @Test
  void emailSubject_of_valid_trims_whitespace() {
    EmailSubject s = EmailSubject.of("  Hello World  ");
    assertEquals("Hello World", s.value());
  }

  @Test
  void emailSubject_rejects_null() {
    assertThrows(NullPointerException.class, () -> EmailSubject.of(null));
  }

  @Test
  void emailSubject_rejects_blank() {
    assertThrows(IllegalArgumentException.class, () -> EmailSubject.of("   "));
  }

  @Test
  void emailSubject_rejects_too_long() {
    String longSubject = "a".repeat(999);
    assertThrows(IllegalArgumentException.class, () -> EmailSubject.of(longSubject));
  }
}

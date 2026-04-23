package com.marcusprado02.commons.ports.sms;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PhoneNumberTest {

  @Test
  void phoneNumber_us_e164_format() {
    PhoneNumber p = PhoneNumber.of("+15551234567");
    assertEquals("+15551234567", p.toE164());
    assertEquals("+15551234567", p.toString());
    assertEquals("1", p.countryCode());
    assertEquals("5551234567", p.nationalNumber());
  }

  @Test
  void phoneNumber_brazil_e164_format() {
    PhoneNumber p = PhoneNumber.of("+5511987654321");
    assertEquals("+5511987654321", p.toE164());
    assertEquals("55", p.countryCode());
    assertEquals("11987654321", p.nationalNumber());
  }

  @Test
  void phoneNumber_uk_format() {
    PhoneNumber p = PhoneNumber.of("+441234567890");
    assertEquals("44", p.countryCode());
  }

  @Test
  void phoneNumber_explicit_country_and_national() {
    PhoneNumber p = PhoneNumber.of("1", "5551234567");
    assertEquals("+15551234567", p.toE164());
    assertEquals("1", p.countryCode());
  }

  @Test
  void phoneNumber_removes_formatting_chars() {
    PhoneNumber p = PhoneNumber.of("+1 (555) 123-4567");
    assertEquals("+15551234567", p.toE164());
  }

  @Test
  void phoneNumber_rejects_null() {
    assertThrows(NullPointerException.class, () -> PhoneNumber.of(null));
  }

  @Test
  void phoneNumber_rejects_blank() {
    assertThrows(IllegalArgumentException.class, () -> PhoneNumber.of("   "));
  }

  @Test
  void phoneNumber_rejects_too_short() {
    assertThrows(IllegalArgumentException.class, () -> PhoneNumber.of("123"));
  }

  @Test
  void phoneNumber_toDisplayFormat_us_10_digit() {
    PhoneNumber p = PhoneNumber.of("+15551234567");
    String display = p.toDisplayFormat();
    assertNotNull(display);
    assertTrue(display.contains("1"));
  }

  @Test
  void phoneNumber_toDisplayFormat_brazil() {
    PhoneNumber p = PhoneNumber.of("+5511987654321");
    String display = p.toDisplayFormat();
    assertNotNull(display);
    assertTrue(display.contains("55"));
  }

  @Test
  void phoneNumber_toDisplayFormat_default() {
    PhoneNumber p = PhoneNumber.of("+4912345678901");
    String display = p.toDisplayFormat();
    assertNotNull(display);
  }
}

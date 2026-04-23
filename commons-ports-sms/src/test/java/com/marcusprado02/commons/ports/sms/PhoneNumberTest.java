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

  @Test
  void phoneNumber_without_plus_prefix_gets_normalized() {
    // covers normalizePhoneNumber: !startsWith("+") true AND length >= 10 true
    PhoneNumber p = PhoneNumber.of("15551234567");
    assertEquals("+15551234567", p.toE164());
  }

  @Test
  void phoneNumber_france_uses_default_two_digit_country_code() {
    // covers parseE164 default branch (not 1, 55, 44, 49)
    PhoneNumber p = PhoneNumber.of("+33123456789");
    assertEquals("33", p.countryCode());
  }

  @Test
  void phoneNumber_toDisplayFormat_us_non_10digit_national() {
    // covers toDisplayFormat: countryCode=="1" but nationalNumber.length != 10
    PhoneNumber p = PhoneNumber.of("+1555123456");
    String display = p.toDisplayFormat();
    assertTrue(display.startsWith("+1"));
  }

  @Test
  void phoneNumber_toDisplayFormat_brazil_short_national() {
    // covers toDisplayFormat: countryCode=="55" but nationalNumber.length < 10
    PhoneNumber p = PhoneNumber.of("+55119123456");
    String display = p.toDisplayFormat();
    assertTrue(display.startsWith("+55"));
  }

  @Test
  void phoneNumber_toDisplayFormat_brazil_number_not_9digits() {
    // covers toDisplayFormat: nationalNumber >= 10 but number part length != 9
    PhoneNumber p = PhoneNumber.of("+55111234567890");
    String display = p.toDisplayFormat();
    assertTrue(display.startsWith("+55"));
  }
}

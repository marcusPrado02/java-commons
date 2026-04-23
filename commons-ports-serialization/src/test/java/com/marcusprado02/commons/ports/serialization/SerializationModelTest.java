package com.marcusprado02.commons.ports.serialization;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class SerializationModelTest {

  @Test
  void valid_result_is_valid_with_no_errors() {
    ValidationResult r = ValidationResult.valid();
    assertTrue(r.isValid());
    assertFalse(r.hasErrors());
    assertFalse(r.hasWarnings());
    assertTrue(r.getErrors().isEmpty());
    assertTrue(r.getWarnings().isEmpty());
  }

  @Test
  void validWithWarnings_is_valid_with_warnings() {
    ValidationResult r = ValidationResult.validWithWarnings(List.of("warn1"));
    assertTrue(r.isValid());
    assertFalse(r.hasErrors());
    assertTrue(r.hasWarnings());
    assertEquals(1, r.getWarnings().size());
  }

  @Test
  void invalid_result_is_not_valid_with_errors() {
    ValidationResult r = ValidationResult.invalid(List.of("err1", "err2"));
    assertFalse(r.isValid());
    assertTrue(r.hasErrors());
    assertFalse(r.hasWarnings());
    assertEquals(2, r.getErrors().size());
  }

  @Test
  void invalid_with_errors_and_warnings() {
    ValidationResult r = ValidationResult.invalid(List.of("e"), List.of("w"));
    assertFalse(r.isValid());
    assertTrue(r.hasErrors());
    assertTrue(r.hasWarnings());
  }

  @Test
  void toString_contains_error_and_warning_counts() {
    String s = ValidationResult.invalid(List.of("e"), List.of("w")).toString();
    assertTrue(s.contains("errors=1"));
    assertTrue(s.contains("warnings=1"));
  }

  @Test
  void equals_and_hashCode() {
    ValidationResult a = ValidationResult.valid();
    ValidationResult b = ValidationResult.valid();
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertEquals(a, a);
    assertNotEquals(a, null);
    assertNotEquals(a, ValidationResult.invalid(List.of("e")));
    assertNotEquals(a, "other");
  }
}

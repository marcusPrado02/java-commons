package com.marcusprado02.commons.kernel.result;

import static org.junit.jupiter.api.Assertions.*;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class OptionTest {

  @Test
  void some_should_be_present() {
    Option<String> opt = Option.some("hello");
    assertTrue(opt.isPresent());
    assertFalse(opt.isEmpty());
    assertEquals("hello", opt.get());
  }

  @Test
  void none_should_not_be_present() {
    Option<String> opt = Option.none();
    assertFalse(opt.isPresent());
    assertTrue(opt.isEmpty());
  }

  @Test
  void none_get_should_throw() {
    assertThrows(NoSuchElementException.class, () -> Option.none().get());
  }

  @Test
  void some_requires_non_null_value() {
    assertThrows(NullPointerException.class, () -> Option.some(null));
  }

  @Test
  void ofNullable_with_value_should_return_some() {
    Option<String> opt = Option.ofNullable("value");
    assertTrue(opt.isPresent());
    assertEquals("value", opt.get());
  }

  @Test
  void ofNullable_with_null_should_return_none() {
    Option<String> opt = Option.ofNullable(null);
    assertFalse(opt.isPresent());
  }

  @Test
  void orElse_should_return_value_when_present() {
    assertEquals("hello", Option.some("hello").orElse("fallback"));
  }

  @Test
  void orElse_should_return_fallback_when_empty() {
    assertEquals("fallback", Option.<String>none().orElse("fallback"));
  }

  @Test
  void orElseGet_should_invoke_supplier_when_empty() {
    String result = Option.<String>none().orElseGet(() -> "computed");
    assertEquals("computed", result);
  }

  @Test
  void map_should_transform_value_when_present() {
    Option<Integer> result = Option.some("hello").map(String::length);
    assertTrue(result.isPresent());
    assertEquals(5, result.get());
  }

  @Test
  void map_should_return_none_when_empty() {
    Option<Integer> result = Option.<String>none().map(String::length);
    assertFalse(result.isPresent());
  }

  @Test
  void map_returning_null_should_give_none() {
    Option<String> result = Option.some("hello").map(s -> null);
    assertFalse(result.isPresent());
  }
}

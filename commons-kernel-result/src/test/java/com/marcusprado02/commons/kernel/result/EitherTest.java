package com.marcusprado02.commons.kernel.result;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EitherTest {

  @Test
  void left_should_be_left_and_not_right() {
    Either<String, Integer> e = Either.left("error");
    assertTrue(e.isLeft());
    assertFalse(e.isRight());
    assertEquals("error", e.left());
  }

  @Test
  void left_right_should_throw() {
    Either<String, Integer> e = Either.left("error");
    assertThrows(IllegalStateException.class, e::right);
  }

  @Test
  void right_should_be_right_and_not_left() {
    Either<String, Integer> e = Either.right(42);
    assertFalse(e.isLeft());
    assertTrue(e.isRight());
    assertEquals(42, e.right());
  }

  @Test
  void right_left_should_throw() {
    Either<String, Integer> e = Either.right(42);
    assertThrows(IllegalStateException.class, e::left);
  }

  @Test
  void mapRight_should_transform_right_value() {
    Either<String, Integer> e = Either.right(5);
    Either<String, String> mapped = e.mapRight(n -> "val-" + n);
    assertTrue(mapped.isRight());
    assertEquals("val-5", mapped.right());
  }

  @Test
  void mapRight_should_pass_through_left() {
    Either<String, Integer> e = Either.left("err");
    Either<String, String> mapped = e.mapRight(n -> "val-" + n);
    assertTrue(mapped.isLeft());
    assertEquals("err", mapped.left());
  }

  @Test
  void mapLeft_should_transform_left_value() {
    Either<String, Integer> e = Either.left("err");
    Either<Integer, Integer> mapped = e.mapLeft(String::length);
    assertTrue(mapped.isLeft());
    assertEquals(3, mapped.left());
  }

  @Test
  void mapLeft_should_pass_through_right() {
    Either<String, Integer> e = Either.right(42);
    Either<Integer, Integer> mapped = e.mapLeft(String::length);
    assertTrue(mapped.isRight());
    assertEquals(42, mapped.right());
  }

  @Test
  void left_requires_non_null() {
    assertThrows(NullPointerException.class, () -> Either.left(null));
  }

  @Test
  void right_requires_non_null() {
    assertThrows(NullPointerException.class, () -> Either.right(null));
  }
}

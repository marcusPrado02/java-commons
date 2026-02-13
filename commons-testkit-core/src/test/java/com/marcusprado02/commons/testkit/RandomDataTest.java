package com.marcusprado02.commons.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RandomDataTest {

  @Test
  void shouldGenerateRandomInt() {
    int value = RandomData.randomInt(1, 10);
    assertThat(value).isBetween(1, 9);
  }

  @Test
  void shouldGenerateRandomLong() {
    long value = RandomData.randomLong(1L, 100L);
    assertThat(value).isBetween(1L, 99L);
  }

  @Test
  void shouldGenerateRandomDouble() {
    double value = RandomData.randomDouble();
    assertThat(value).isBetween(0.0, 1.0);
  }

  @Test
  void shouldGenerateRandomBoolean() {
    boolean value = RandomData.randomBoolean();
    assertThat(value).isIn(true, false);
  }

  @Test
  void shouldGenerateRandomString() {
    String value = RandomData.randomString(5);
    assertThat(value).hasSize(5);
    assertThat(value).matches("[A-Za-z0-9]+");
  }

  @Test
  void shouldGenerateRandomEmail() {
    String email = RandomData.randomEmail();
    assertThat(email).contains("@test.com");
    assertThat(email).matches("[a-z0-9]+@test\\.com");
  }

  @Test
  void shouldGenerateRandomUuid() {
    String uuid = RandomData.randomUuid();
    assertThat(uuid).hasSize(36);
    assertThat(uuid).contains("-");
  }

  @Test
  void shouldPickRandomFromArray() {
    String[] values = {"a", "b", "c"};
    String picked = RandomData.randomFrom(values);
    assertThat(picked).isIn((Object[]) values);
  }

  enum TestEnum {
    A,
    B,
    C
  }

  @Test
  void shouldPickRandomEnum() {
    TestEnum value = RandomData.randomEnum(TestEnum.class);
    assertThat(value).isIn(TestEnum.A, TestEnum.B, TestEnum.C);
  }
}

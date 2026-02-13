package com.marcusprado02.commons.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TestIdsTest {

  @AfterEach
  void tearDown() {
    TestIds.reset();
  }

  @Test
  void shouldGenerateSequentialIds() {
    assertThat(TestIds.nextId()).isEqualTo("1");
    assertThat(TestIds.nextId()).isEqualTo("2");
    assertThat(TestIds.nextId()).isEqualTo("3");
  }

  @Test
  void shouldGenerateSequentialIdsWithPrefix() {
    assertThat(TestIds.nextId("user")).isEqualTo("user-1");
    assertThat(TestIds.nextId("user")).isEqualTo("user-2");
    assertThat(TestIds.nextId("order")).isEqualTo("order-3");
  }

  @Test
  void shouldResetSequence() {
    TestIds.nextId();
    TestIds.nextId();
    TestIds.reset();
    assertThat(TestIds.nextId()).isEqualTo("1");
  }

  @Test
  void shouldGenerateRandomIds() {
    String id1 = TestIds.randomId();
    String id2 = TestIds.randomId();
    assertThat(id1).isNotEqualTo(id2);
    assertThat(id1).hasSize(36); // UUID length
  }

  @Test
  void shouldGenerateTimestampIds() {
    String id1 = TestIds.timestampId();
    String id2 = TestIds.timestampId();
    assertThat(id1).isNotEqualTo(id2);
    assertThat(id1).contains("-");
  }
}

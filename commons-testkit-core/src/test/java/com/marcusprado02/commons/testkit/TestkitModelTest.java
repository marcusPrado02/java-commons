package com.marcusprado02.commons.testkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcusprado02.commons.kernel.ddd.audit.ActorId;
import com.marcusprado02.commons.kernel.ddd.audit.AuditStamp;
import com.marcusprado02.commons.kernel.ddd.entity.AggregateRoot;
import com.marcusprado02.commons.kernel.ddd.event.DomainEvent;
import com.marcusprado02.commons.kernel.ddd.event.EventId;
import com.marcusprado02.commons.kernel.ddd.event.EventMetadata;
import com.marcusprado02.commons.kernel.ddd.tenant.TenantId;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.testkit.matchers.AggregateRootAssert;
import com.marcusprado02.commons.testkit.matchers.ResultAssert;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TestkitModelTest {

  // ── ResultAssert ──────────────────────────────────────────────────────────

  @Test
  void resultAssertIsSuccess_passes() {
    Result<String> ok = Result.ok("hello");
    ResultAssert.assertThat(ok).isSuccess();
  }

  @Test
  void resultAssertIsSuccess_fails_whenFailure() {
    Problem problem =
        Problem.of(ErrorCode.of("TEST.ERR"), ErrorCategory.TECHNICAL, Severity.ERROR, "oops");
    Result<String> fail = Result.fail(problem);
    assertThatThrownBy(() -> ResultAssert.assertThat(fail).isSuccess())
        .isInstanceOf(AssertionError.class);
  }

  @Test
  void resultAssertIsFailure_passes() {
    Problem problem =
        Problem.of(ErrorCode.of("TEST.ERR"), ErrorCategory.TECHNICAL, Severity.ERROR, "oops");
    Result<String> fail = Result.fail(problem);
    ResultAssert.assertThat(fail).isFailure();
  }

  @Test
  void resultAssertIsFailure_fails_whenSuccess() {
    Result<String> ok = Result.ok("hello");
    assertThatThrownBy(() -> ResultAssert.assertThat(ok).isFailure())
        .isInstanceOf(AssertionError.class);
  }

  @Test
  void resultAssertHasValue_passesConsumer() {
    Result<String> ok = Result.ok("world");
    ResultAssert.assertThat(ok).hasValue(v -> assertThat(v).isEqualTo("world"));
  }

  @Test
  void resultAssertHasError_passesConsumer() {
    Problem problem =
        Problem.of(ErrorCode.of("TEST.ERR"), ErrorCategory.TECHNICAL, Severity.ERROR, "fail");
    Result<String> fail = Result.fail(problem);
    ResultAssert.assertThat(fail).hasError(p -> assertThat(p.message()).isEqualTo("fail"));
  }

  // ── AggregateRootAssert ───────────────────────────────────────────────────

  static final class OrderId {
    final String value;

    OrderId(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }
  }

  record TestEvent(
      EventId eventId,
      Instant occurredAt,
      String aggregateType,
      String aggregateId,
      long aggregateVersion,
      EventMetadata metadata)
      implements DomainEvent {}

  static final class TestAggregate extends AggregateRoot<OrderId> {
    TestAggregate() {
      super(
          new OrderId("order-1"),
          TenantId.of("tenant"),
          AuditStamp.of(Instant.parse("2024-01-01T00:00:00Z"), ActorId.system()));
    }

    void fireEvent() {
      recordEvent(
          new TestEvent(
              EventId.newId(),
              Instant.now(),
              "TestAggregate",
              "order-1",
              1L,
              EventMetadata.empty()));
    }
  }

  @Test
  void aggregateAssertHasNoDomainEvents() {
    TestAggregate agg = new TestAggregate();
    AggregateRootAssert.assertThat(agg).hasNoDomainEvents();
  }

  @Test
  void aggregateAssertHasDomainEvents_passes() {
    TestAggregate agg = new TestAggregate();
    agg.fireEvent();
    AggregateRootAssert.assertThat(agg).hasDomainEvents(1);
  }

  @Test
  void aggregateAssertHasDomainEvents_fails_wrongCount() {
    TestAggregate agg = new TestAggregate();
    assertThatThrownBy(() -> AggregateRootAssert.assertThat(agg).hasDomainEvents(1))
        .isInstanceOf(AssertionError.class);
  }

  @Test
  void aggregateAssertHasDomainEventOfType_passes() {
    TestAggregate agg = new TestAggregate();
    agg.fireEvent();
    AggregateRootAssert.assertThat(agg).hasDomainEventOfType(TestEvent.class);
  }

  @Test
  void aggregateAssertHasDomainEventOfType_fails_missing() {
    TestAggregate agg = new TestAggregate();
    assertThatThrownBy(
            () -> AggregateRootAssert.assertThat(agg).hasDomainEventOfType(TestEvent.class))
        .isInstanceOf(AssertionError.class);
  }

  @Test
  void aggregateAssertDoesNotHaveDomainEventOfType_passes() {
    TestAggregate agg = new TestAggregate();
    AggregateRootAssert.assertThat(agg).doesNotHaveDomainEventOfType(TestEvent.class);
  }

  @Test
  void aggregateAssertDoesNotHaveDomainEventOfType_fails_present() {
    TestAggregate agg = new TestAggregate();
    agg.fireEvent();
    assertThatThrownBy(
            () -> AggregateRootAssert.assertThat(agg).doesNotHaveDomainEventOfType(TestEvent.class))
        .isInstanceOf(AssertionError.class);
  }

  // ── Fixtures ──────────────────────────────────────────────────────────────

  @Test
  void fixturesClockAtInstant() {
    Instant instant = Instant.parse("2024-06-01T12:00:00Z");
    assertThat(Fixtures.clockAt(instant).now()).isEqualTo(instant);
  }

  @Test
  void fixturesClockAtDateParts() {
    assertThat(Fixtures.clockAt(2025, 3, 15, 9, 0).now())
        .isEqualTo(Instant.parse("2025-03-15T09:00:00Z"));
  }

  @Test
  void fixturesDefaultConstants() {
    assertThat(Fixtures.DEFAULT_INSTANT).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
    assertThat(Fixtures.DEFAULT_TENANT_ID).isEqualTo("test-tenant");
    assertThat(Fixtures.DEFAULT_CORRELATION_ID).isEqualTo("test-correlation-id");
    assertThat(Fixtures.DEFAULT_ACTOR_ID).isEqualTo("test-user");
    assertThat(Fixtures.DEFAULT_CLOCK.now()).isEqualTo(Fixtures.DEFAULT_INSTANT);
  }

  // ── TestDataBuilder ───────────────────────────────────────────────────────

  static final class StringBuilder extends TestDataBuilder<String> {
    private String value = "default";

    StringBuilder withValue(String v) {
      this.value = v;
      return this;
    }

    @Override
    public String build() {
      return value;
    }
  }

  @Test
  void testDataBuilderBuild() {
    assertThat(new StringBuilder().build()).isEqualTo("default");
  }

  @Test
  void testDataBuilderGet() {
    assertThat(new StringBuilder().withValue("hello").get()).isEqualTo("hello");
  }

  @Test
  void testDataBuilderCreate() {
    assertThat(new StringBuilder().withValue("world").create()).isEqualTo("world");
  }

  @Test
  void testDataBuilderBut_returnsSelf() {
    StringBuilder builder = new StringBuilder();
    assertThat(builder.but()).isSameAs(builder);
  }

  // ── TestClock.fixedClock ──────────────────────────────────────────────────

  @Test
  void testClockFixedClock_returnsJavaTimeClock() {
    Instant now = Instant.parse("2024-05-01T08:00:00Z");
    Clock clock = TestClock.fixedClock(now);
    assertThat(clock.instant()).isEqualTo(now);
  }
}

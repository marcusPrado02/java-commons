package com.marcusprado02.commons.starter.outbox.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.marcusprado02.commons.app.outbox.model.OutboxStatus;
import com.marcusprado02.commons.app.outbox.port.OutboxRepositoryPort;
import com.marcusprado02.commons.starter.outbox.OutboxProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

class OutboxHealthIndicatorTest {

  private final OutboxRepositoryPort repository = mock(OutboxRepositoryPort.class);

  @Test
  void shouldReportUpWhenBelowThresholds() {
    var properties =
        new OutboxProperties(
            new OutboxProperties.Processing(10, false),
            new OutboxProperties.Scheduling(true, Duration.ofSeconds(60)),
            new OutboxProperties.Retry(5, Duration.ofMillis(100), Duration.ofSeconds(30), 2.0),
            new OutboxProperties.Health(true, 100, 500));

    when(repository.countByStatus(OutboxStatus.PENDING)).thenReturn(10L);
    when(repository.countByStatus(OutboxStatus.PROCESSING)).thenReturn(2L);
    when(repository.countByStatus(OutboxStatus.PUBLISHED)).thenReturn(1000L);
    when(repository.countByStatus(OutboxStatus.FAILED)).thenReturn(5L);
    when(repository.countByStatus(OutboxStatus.DEAD)).thenReturn(1L);

    var indicator = new OutboxHealthIndicator(repository, properties);
    Health health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails().get("pending")).isEqualTo(10L);
    assertThat(health.getDetails().get("failed")).isEqualTo(5L);
  }

  @Test
  void shouldReportDegradedWhenAboveWarningThreshold() {
    var properties =
        new OutboxProperties(
            new OutboxProperties.Processing(10, false),
            new OutboxProperties.Scheduling(true, Duration.ofSeconds(60)),
            new OutboxProperties.Retry(5, Duration.ofMillis(100), Duration.ofSeconds(30), 2.0),
            new OutboxProperties.Health(true, 100, 500));

    when(repository.countByStatus(OutboxStatus.PENDING)).thenReturn(150L);
    when(repository.countByStatus(OutboxStatus.PROCESSING)).thenReturn(0L);
    when(repository.countByStatus(OutboxStatus.PUBLISHED)).thenReturn(0L);
    when(repository.countByStatus(OutboxStatus.FAILED)).thenReturn(0L);
    when(repository.countByStatus(OutboxStatus.DEAD)).thenReturn(0L);

    var indicator = new OutboxHealthIndicator(repository, properties);
    Health health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(new Status("DEGRADED"));
  }

  @Test
  void shouldReportDownWhenFailedAboveErrorThreshold() {
    var properties =
        new OutboxProperties(
            new OutboxProperties.Processing(10, false),
            new OutboxProperties.Scheduling(true, Duration.ofSeconds(60)),
            new OutboxProperties.Retry(5, Duration.ofMillis(100), Duration.ofSeconds(30), 2.0),
            new OutboxProperties.Health(true, 100, 500));

    when(repository.countByStatus(OutboxStatus.PENDING)).thenReturn(0L);
    when(repository.countByStatus(OutboxStatus.PROCESSING)).thenReturn(0L);
    when(repository.countByStatus(OutboxStatus.PUBLISHED)).thenReturn(0L);
    when(repository.countByStatus(OutboxStatus.FAILED)).thenReturn(600L);
    when(repository.countByStatus(OutboxStatus.DEAD)).thenReturn(0L);

    var indicator = new OutboxHealthIndicator(repository, properties);
    Health health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  void shouldReportOutOfServiceWhenPendingAboveErrorThreshold() {
    var properties =
        new OutboxProperties(
            new OutboxProperties.Processing(10, false),
            new OutboxProperties.Scheduling(true, Duration.ofSeconds(60)),
            new OutboxProperties.Retry(5, Duration.ofMillis(100), Duration.ofSeconds(30), 2.0),
            new OutboxProperties.Health(true, 100, 500));

    when(repository.countByStatus(OutboxStatus.PENDING)).thenReturn(600L);
    when(repository.countByStatus(OutboxStatus.PROCESSING)).thenReturn(0L);
    when(repository.countByStatus(OutboxStatus.PUBLISHED)).thenReturn(0L);
    when(repository.countByStatus(OutboxStatus.FAILED)).thenReturn(0L);
    when(repository.countByStatus(OutboxStatus.DEAD)).thenReturn(0L);

    var indicator = new OutboxHealthIndicator(repository, properties);
    Health health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
  }
}

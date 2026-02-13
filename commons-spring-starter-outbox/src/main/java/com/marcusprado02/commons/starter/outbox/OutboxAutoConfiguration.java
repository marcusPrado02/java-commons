package com.marcusprado02.commons.starter.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcusprado02.commons.adapters.persistence.jpa.outbox.JpaOutboxRepositoryAdapter;
import com.marcusprado02.commons.app.outbox.*;
import com.marcusprado02.commons.app.outbox.config.OutboxProcessorConfig;
import com.marcusprado02.commons.app.outbox.metrics.OutboxMetrics;
import com.marcusprado02.commons.app.outbox.port.OutboxRepositoryPort;
import com.marcusprado02.commons.kernel.ddd.context.ActorProvider;
import com.marcusprado02.commons.kernel.ddd.context.CorrelationProvider;
import com.marcusprado02.commons.kernel.ddd.context.TenantProvider;
import com.marcusprado02.commons.kernel.ddd.time.ClockProvider;
import com.marcusprado02.commons.starter.outbox.health.OutboxHealthIndicator;
import com.marcusprado02.commons.starter.outbox.metrics.MicrometerOutboxMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Consolidated auto-configuration for Outbox pattern.
 *
 * <p>Configures:
 *
 * <ul>
 *   <li>OutboxRepositoryPort (JPA adapter)
 *   <li>OutboxSerializer (Jackson)
 *   <li>OutboxMetadataEnricher
 *   <li>OutboxPublisher
 *   <li>OutboxProcessor (with metrics and retry)
 *   <li>OutboxMetrics (Micrometer)
 *   <li>OutboxHealthIndicator (Actuator)
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass(EntityManager.class)
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(OutboxRepositoryPort.class)
  public OutboxRepositoryPort outboxRepositoryPort(EntityManager entityManager) {
    return new JpaOutboxRepositoryAdapter(entityManager);
  }

  @Bean
  @ConditionalOnMissingBean(OutboxSerializer.class)
  public OutboxSerializer outboxSerializer(ObjectMapper mapper) {
    return new JacksonOutboxSerializer(mapper);
  }

  @Bean
  @ConditionalOnMissingBean(OutboxMetadataEnricher.class)
  public OutboxMetadataEnricher outboxMetadataEnricher(
      TenantProvider tenantProvider,
      CorrelationProvider correlationProvider,
      ActorProvider actorProvider) {
    return new OutboxMetadataEnricher(tenantProvider, correlationProvider, actorProvider);
  }

  @Bean
  @ConditionalOnMissingBean(OutboxPublisher.class)
  public OutboxPublisher outboxPublisher(
      OutboxRepositoryPort repository,
      OutboxSerializer serializer,
      OutboxMetadataEnricher enricher,
      ClockProvider clock) {
    return new DefaultOutboxPublisher(repository, serializer, enricher, clock);
  }

  @Bean
  @ConditionalOnMissingBean(OutboxSupport.class)
  public OutboxSupport outboxSupport(OutboxPublisher publisher) {
    return new OutboxSupport(publisher);
  }

  @Bean
  @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
  @ConditionalOnBean(MeterRegistry.class)
  @ConditionalOnMissingBean(OutboxMetrics.class)
  public OutboxMetrics outboxMetrics(MeterRegistry registry) {
    return new MicrometerOutboxMetrics(registry);
  }

  @Bean
  @ConditionalOnMissingBean(OutboxProcessorConfig.class)
  public OutboxProcessorConfig outboxProcessorConfig(OutboxProperties properties) {
    return new OutboxProcessorConfig(
        properties.processing().batchSize(),
        properties.retry().maxAttempts(),
        properties.retry().initialBackoff(),
        properties.retry().maxBackoff(),
        properties.retry().backoffMultiplier(),
        properties.processing().useCircuitBreaker());
  }

  @Bean
  @ConditionalOnBean(OutboundPublisher.class)
  @ConditionalOnMissingBean(OutboxProcessor.class)
  public OutboxProcessor outboxProcessor(
      OutboxRepositoryPort repository,
      OutboundPublisher publisher,
      OutboxProcessorConfig config,
      OutboxMetrics metrics) {
    return new DefaultOutboxProcessor(repository, publisher, config, metrics);
  }

  @Bean
  @ConditionalOnClass(name = "org.springframework.boot.actuate.health.HealthIndicator")
  @ConditionalOnProperty(
      prefix = "commons.outbox.health",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  @ConditionalOnMissingBean(name = "outboxHealthIndicator")
  public HealthIndicator outboxHealthIndicator(
      OutboxRepositoryPort repository, OutboxProperties properties) {
    return new OutboxHealthIndicator(repository, properties);
  }
}

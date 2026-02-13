package com.marcusprado02.commons.app.outbox;

import com.marcusprado02.commons.app.outbox.model.*;
import com.marcusprado02.commons.app.outbox.port.OutboxRepositoryPort;
import com.marcusprado02.commons.kernel.ddd.entity.AggregateRoot;
import com.marcusprado02.commons.kernel.ddd.event.DomainEvent;
import com.marcusprado02.commons.kernel.ddd.time.ClockProvider;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class DefaultOutboxPublisher implements OutboxPublisher {

  private final OutboxRepositoryPort repository;
  private final OutboxSerializer serializer;
  private final OutboxMetadataEnricher enricher;
  private final ClockProvider clock;

  public DefaultOutboxPublisher(
      OutboxRepositoryPort repository,
      OutboxSerializer serializer,
      OutboxMetadataEnricher enricher,
      ClockProvider clock) {
    this.repository = Objects.requireNonNull(repository, "repository");
    this.serializer = Objects.requireNonNull(serializer, "serializer");
    this.enricher = Objects.requireNonNull(enricher, "enricher");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public void publishFrom(AggregateRoot<?> aggregate) {
    Objects.requireNonNull(aggregate, "aggregate");

    Instant now = clock.now();

    // metadata enriched from tenant/correlation/actor
    Map<String, Object> enrichmentMap = enricher.enrich();

    // Convert enrichment metadata to string headers
    Map<String, String> headers = new LinkedHashMap<>();
    enrichmentMap.forEach(
        (k, v) -> {
          if (v != null) {
            headers.put(k, String.valueOf(v));
          }
        });

    for (DomainEvent event : aggregate.pullDomainEvents()) {
      // serialize event to JSON string
      String json = serializer.serialize(event);
      byte[] body = json.getBytes(StandardCharsets.UTF_8);

      // determine topic name (default: fully qualified class name)
      String topic = event.getClass().getName();

      OutboxPayload payload = new OutboxPayload("application/json", body);

      OutboxMessage msg =
          new OutboxMessage(
              new OutboxMessageId(UUID.randomUUID().toString()),
              aggregate.getClass().getSimpleName(),
              aggregate.id().toString(),
              event.getClass().getSimpleName(),
              topic,
              payload,
              Map.copyOf(headers),
              now,
              OutboxStatus.PENDING,
              0);

      repository.append(msg);
    }
  }
}

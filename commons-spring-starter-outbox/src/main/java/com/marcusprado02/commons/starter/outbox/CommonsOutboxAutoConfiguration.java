package com.marcusprado02.commons.starter.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcusprado02.commons.adapters.persistence.jpa.outbox.JpaOutboxRepositoryAdapter;
import com.marcusprado02.commons.app.outbox.*;
import com.marcusprado02.commons.app.outbox.model.OutboxMessage;
import com.marcusprado02.commons.app.outbox.model.OutboxMessageId;
import com.marcusprado02.commons.app.outbox.model.OutboxStatus;
import com.marcusprado02.commons.app.outbox.port.OutboxRepositoryPort;
import com.marcusprado02.commons.kernel.ddd.context.ActorProvider;
import com.marcusprado02.commons.kernel.ddd.context.CorrelationProvider;
import com.marcusprado02.commons.kernel.ddd.context.TenantProvider;
import com.marcusprado02.commons.kernel.ddd.time.ClockProvider;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(EntityManager.class)
public class CommonsOutboxAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(OutboxRepositoryPort.class)
  OutboxRepositoryPort outboxRepositoryPort(EntityManager entityManager) {
    return new JpaOutboxRepositoryAdapter(entityManager);
  }

  /** Adapter que bridge OutboxRepositoryPort -> OutboxStore. */
  @Bean
  @ConditionalOnMissingBean(OutboxStore.class)
  OutboxStore outboxStore(OutboxRepositoryPort repositoryPort) {
    return new OutboxStore() {
      @Override
      public List<OutboxMessage> fetchByStatus(OutboxStatus status) {
        // Usa limite padrão de 100 mensagens
        return repositoryPort.fetchBatch(status, 100);
      }

      @Override
      public void updateStatus(OutboxMessageId id, OutboxStatus newStatus) {
        updateStatus(id, newStatus, 0);
      }

      @Override
      public void updateStatus(OutboxMessageId id, OutboxStatus newStatus, int attempts) {
        // Mapeia para os métodos específicos do repositoryPort baseado no status
        switch (newStatus) {
          case PUBLISHED -> repositoryPort.markPublished(id, java.time.Instant.now());
          case FAILED -> repositoryPort.markFailed(id, "Processing failed", attempts);
          case PENDING, PROCESSING -> {
            // TODO: OutboxRepositoryPort precisa de métodos para PENDING e PROCESSING
            // Por ora, ignora essas transições de status
          }
        }
      }

      @Override
      public void append(OutboxMessage message) {
        repositoryPort.append(message);
      }
    };
  }

  @Bean
  OutboxSerializer outboxSerializer(ObjectMapper mapper) {
    return new JacksonOutboxSerializer(mapper);
  }

  @Bean
  OutboxMetadataEnricher outboxMetadataEnricher(
      TenantProvider tenantProvider,
      CorrelationProvider correlationProvider,
      ActorProvider actorProvider) {
    return new OutboxMetadataEnricher(tenantProvider, correlationProvider, actorProvider);
  }

  @Bean
  @ConditionalOnBean(OutboxStore.class)
  OutboxPublisher outboxPublisher(
      OutboxStore store,
      OutboxSerializer serializer,
      OutboxMetadataEnricher enricher,
      ClockProvider clock) {
    return new DefaultOutboxPublisher(store, serializer, enricher, clock);
  }
}

package com.marcusprado02.commons.starter.outbox;

import com.marcusprado02.commons.adapters.persistence.jpa.outbox.JpaOutboxRepositoryAdapter;
import com.marcusprado02.commons.app.outbox.*;
import com.marcusprado02.commons.app.outbox.model.OutboxMessage;
import com.marcusprado02.commons.app.outbox.model.OutboxMessageId;
import com.marcusprado02.commons.app.outbox.model.OutboxStatus;
import com.marcusprado02.commons.app.outbox.port.OutboxPublisherPort;
import com.marcusprado02.commons.app.outbox.port.OutboxRepositoryPort;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(EntityManager.class)
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(OutboxRepositoryPort.class)
  public OutboxRepositoryPort outboxRepositoryPort(EntityManager entityManager) {
    return new JpaOutboxRepositoryAdapter(entityManager);
  }

  /** Adapter que bridge OutboxRepositoryPort -> OutboxStore para DefaultOutboxProcessor. */
  @Bean
  @ConditionalOnMissingBean(OutboxStore.class)
  public OutboxStore outboxStore(OutboxRepositoryPort repositoryPort) {
    return new OutboxStore() {
      @Override
      public List<OutboxMessage> fetchByStatus(OutboxStatus status) {
        // TODO: implementar limite configurável
        return repositoryPort.fetchBatch(status, 100);
      }

      @Override
      public void updateStatus(OutboxMessageId id, OutboxStatus newStatus) {
        // TODO: mapear para os métodos corretos do repositoryPort baseado no status
        updateStatus(id, newStatus, 0);
      }

      @Override
      public void updateStatus(OutboxMessageId id, OutboxStatus newStatus, int attempts) {
        // TODO: mapear para markPublished, markFailed ou markDead baseado no status
        switch (newStatus) {
          case PUBLISHED -> repositoryPort.markPublished(id, java.time.Instant.now());
          case FAILED -> repositoryPort.markFailed(id, "Processing failed", attempts);
          default -> {
            // Para PENDING e PROCESSING, não há método específico no port ainda
            // TODO: adicionar métodos apropriados no OutboxRepositoryPort
          }
        }
      }

      @Override
      public void append(OutboxMessage message) {
        repositoryPort.append(message);
      }
    };
  }

  /** Adapter que bridge OutboxPublisherPort -> OutboundPublisher para DefaultOutboxProcessor. */
  @Bean
  @ConditionalOnMissingBean(OutboundPublisher.class)
  @ConditionalOnBean(OutboxPublisherPort.class)
  public OutboundPublisher outboundPublisher(OutboxPublisherPort publisherPort) {
    return (topic, body, headers) -> {
      // Adapter simplificado: OutboundPublisher espera mensagem já serializada,
      // mas OutboxPublisherPort.publish() espera OutboxMessage completo.
      // TODO: Refatorar DefaultOutboxProcessor para usar OutboxPublisherPort diretamente
      // ou criar um OutboxPublisherPort que funcione com payloads já serializados.
      throw new UnsupportedOperationException(
          "OutboundPublisher bridge not fully implemented yet. "
              + "DefaultOutboxProcessor needs refactoring to use OutboxPublisherPort directly.");
    };
  }

  /** Só cria o processor se existir um publisher real (Kafka, Rabbit, etc). */
  @Bean
  @ConditionalOnBean(OutboxPublisherPort.class)
  @ConditionalOnMissingBean(OutboxProcessor.class)
  public OutboxProcessor outboxProcessor(
      OutboxStore store, OutboundPublisher publisher, OutboxProperties properties) {
    // TODO: adicionar maxAttempts nas properties
    int maxAttempts = 5; // valor padrão por enquanto
    return new DefaultOutboxProcessor(store, publisher, maxAttempts);
  }
}

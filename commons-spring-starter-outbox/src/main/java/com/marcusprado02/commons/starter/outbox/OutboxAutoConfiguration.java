package com.marcusprado02.commons.starter.outbox;

import com.marcusprado02.commons.adapters.persistence.jpa.outbox.JpaOutboxRepositoryAdapter;
import com.marcusprado02.commons.app.outbox.port.OutboxPublisherPort;
import com.marcusprado02.commons.app.outbox.port.OutboxRepositoryPort;
import com.marcusprado02.commons.app.outbox.service.DefaultOutboxProcessor;
import com.marcusprado02.commons.app.outbox.service.OutboxProcessor;
import jakarta.persistence.EntityManager;
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

    /**
     * Só cria o processor se existir um publisher real (Kafka, Rabbit, etc).
     */
    @Bean
    @ConditionalOnBean(OutboxPublisherPort.class)
    @ConditionalOnMissingBean(OutboxProcessor.class)
    public OutboxProcessor outboxProcessor(
            OutboxRepositoryPort repositoryPort,
            OutboxPublisherPort publisherPort
    ) {
        return new DefaultOutboxProcessor(repositoryPort, publisherPort);
    }
}

package com.marcusprado02.commons.starter.idempotency;

import com.marcusprado02.commons.adapters.persistence.jpa.idempotency.JpaIdempotencyStoreAdapter;
import com.marcusprado02.commons.app.idempotency.port.IdempotencyStorePort;
import com.marcusprado02.commons.app.idempotency.service.DefaultIdempotentExecutor;
import com.marcusprado02.commons.app.idempotency.service.IdempotentExecutor;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(EntityManager.class)
@EnableConfigurationProperties(IdempotencyProperties.class)
public class IdempotencyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IdempotencyStorePort.class)
    public IdempotencyStorePort idempotencyStorePort(EntityManager entityManager) {
        return new JpaIdempotencyStoreAdapter(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean(IdempotentExecutor.class)
    public IdempotentExecutor idempotentExecutor(IdempotencyStorePort storePort) {
        return new DefaultIdempotentExecutor(storePort);
    }
}

package com.marcusprado02.commons.spring.starter.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcusprado02.commons.app.outbox.*;
import com.marcusprado02.commons.kernel.ddd.context.ActorProvider;
import com.marcusprado02.commons.kernel.ddd.context.CorrelationProvider;
import com.marcusprado02.commons.kernel.ddd.context.TenantProvider;
import com.marcusprado02.commons.kernel.ddd.time.ClockProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonsOutboxAutoConfiguration {

  @Bean
  OutboxSerializer outboxSerializer(ObjectMapper mapper) {
    return new JacksonOutboxSerializer(mapper);
  }

  @Bean
  OutboxMetadataEnricher outboxMetadataEnricher(
      TenantProvider tenantProvider,
      CorrelationProvider correlationProvider,
      ActorProvider actorProvider
  ) {
    return new OutboxMetadataEnricher(tenantProvider, correlationProvider, actorProvider);
  }

  @Bean
  @ConditionalOnBean(OutboxStore.class)
  OutboxPublisher outboxPublisher(
      OutboxStore store,
      OutboxSerializer serializer,
      OutboxMetadataEnricher enricher,
      ClockProvider clock
  ) {
    return new DefaultOutboxPublisher(store, serializer, enricher, clock);
  }
}

package com.marcusprado02.commons.starter.outbox;

import com.marcusprado02.commons.app.outbox.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonsOutboxProcessorAutoConfiguration {

  @Bean
  @ConditionalOnBean({OutboundPublisher.class, OutboxStore.class})
  OutboxProcessor outboxProcessor(OutboxStore store, OutboundPublisher outboundPublisher) {
    return new DefaultOutboxProcessor(store, outboundPublisher, 5);
  }
}

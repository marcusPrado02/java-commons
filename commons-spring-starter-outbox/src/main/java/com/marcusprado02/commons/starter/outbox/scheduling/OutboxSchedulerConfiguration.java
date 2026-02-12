package com.marcusprado02.commons.starter.outbox.scheduling;

import com.marcusprado02.commons.app.outbox.OutboxProcessor;
import com.marcusprado02.commons.starter.outbox.OutboxProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@ConditionalOnProperty(prefix = "commons.outbox.scheduling", name = "enabled", havingValue = "true")
@EnableScheduling
public class OutboxSchedulerConfiguration {

  @Bean
  @ConditionalOnBean(OutboxProcessor.class)
  public OutboxScheduledJob outboxScheduledJob(OutboxProcessor processor, OutboxProperties props) {
    return new OutboxScheduledJob(processor, props);
  }
}

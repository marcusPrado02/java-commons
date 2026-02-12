package com.marcusprado02.commons.starter.outbox.scheduling;

import com.marcusprado02.commons.app.outbox.OutboxProcessor;
import com.marcusprado02.commons.starter.outbox.OutboxProperties;
import java.util.Objects;
import org.springframework.scheduling.annotation.Scheduled;

public final class OutboxScheduledJob {

  private final OutboxProcessor processor;
  private final OutboxProperties props;

  public OutboxScheduledJob(OutboxProcessor processor, OutboxProperties props) {
    this.processor = Objects.requireNonNull(processor);
    this.props = Objects.requireNonNull(props);
  }

  @Scheduled(fixedDelayString = "#{@outboxProperties.scheduling().fixedDelay().toMillis()}")
  public void tick() {
    // TODO: enhance OutboxProcessor interface to support batch size
    processor.processAll();
  }
}

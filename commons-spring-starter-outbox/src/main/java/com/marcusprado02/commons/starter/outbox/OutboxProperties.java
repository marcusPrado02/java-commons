package com.marcusprado02.commons.starter.outbox;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "commons.outbox")
public record OutboxProperties(
        int batchSize,
        Scheduling scheduling
) {

    public OutboxProperties {
        if (batchSize <= 0) {
            batchSize = 100;
        }
        if (scheduling == null) {
            scheduling = new Scheduling(false, Duration.ofSeconds(2));
        }
    }

    public record Scheduling(boolean enabled, Duration fixedDelay) {
        public Scheduling {
            if (fixedDelay == null) {
                fixedDelay = Duration.ofSeconds(2);
            }
        }
    }
}

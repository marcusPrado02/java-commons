package com.marcusprado02.commons.starter.outbox;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class OutboxPropertiesBeanConfiguration {

    @Bean(name = "outboxProperties")
    public OutboxProperties outboxProperties(OutboxProperties props) {
        return props;
    }
}

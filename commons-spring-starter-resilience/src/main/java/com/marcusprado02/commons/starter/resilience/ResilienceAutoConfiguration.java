package com.marcusprado02.commons.starter.resilience;

import com.marcusprado02.commons.adapters.resilience4j.Resilience4jExecutor;
import com.marcusprado02.commons.app.resilience.NoopResilienceExecutor;
import com.marcusprado02.commons.app.resilience.ResilienceExecutor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class ResilienceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ResilienceExecutor.class)
    @ConditionalOnClass(name = "io.github.resilience4j.retry.Retry")
    public ResilienceExecutor resilienceExecutor() {
        return new Resilience4jExecutor();
    }

    @Bean
    @ConditionalOnMissingBean(ResilienceExecutor.class)
    public ResilienceExecutor noopResilienceExecutor() {
        return new NoopResilienceExecutor();
    }
}

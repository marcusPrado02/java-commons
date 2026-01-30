package com.marcusprado02.commons.adapters.web.spring.context;

import com.marcusprado02.commons.kernel.ddd.context.ActorProvider;
import com.marcusprado02.commons.kernel.ddd.context.CorrelationProvider;
import com.marcusprado02.commons.kernel.ddd.context.TenantProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonsKernelContextBridgeAutoConfiguration {

  @Bean
  ActorProvider actorProvider() {
    return new HolderBackedActorProvider();
  }

  @Bean
  TenantProvider tenantProvider() {
    return new HolderBackedTenantProvider();
  }

  @Bean
  CorrelationProvider correlationProvider() {
    return new HolderBackedCorrelationProvider();
  }
}

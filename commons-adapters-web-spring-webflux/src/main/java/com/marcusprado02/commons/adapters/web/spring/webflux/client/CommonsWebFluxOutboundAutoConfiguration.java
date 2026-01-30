package com.marcusprado02.commons.adapters.web.spring.webflux.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class CommonsWebFluxOutboundAutoConfiguration {

  @Bean
  WebClient.Builder commonsWebClientBuilder() {
    return WebClient.builder()
        .filter(WebClientContextExchangeFilter.propagateContextHeaders());
  }
}

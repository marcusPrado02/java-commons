package com.marcusprado02.commons.adapters.web.spring.context;

import com.marcusprado02.commons.platform.http.ContextHeaderWriter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CommonsWebContextProperties.class)
public class CommonsWebContextAutoConfiguration {

  @Bean
  SpringActorResolver springActorResolver() {
    return new DefaultSpringActorResolver();
  }

  @Bean
  SpringRequestContextResolver springRequestContextResolver(
      CommonsWebContextProperties props, SpringActorResolver actorResolver) {
    return new SpringRequestContextResolver(props, actorResolver);
  }

  @Bean
  SpringRequestContextFilter springRequestContextFilter(
      SpringRequestContextResolver resolver, ContextHeaderWriter contextHeaderWriter) {
    return new SpringRequestContextFilter(resolver, contextHeaderWriter);
  }
}

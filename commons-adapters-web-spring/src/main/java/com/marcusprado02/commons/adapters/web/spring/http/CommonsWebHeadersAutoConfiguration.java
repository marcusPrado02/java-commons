package com.marcusprado02.commons.adapters.web.spring.http;

import com.marcusprado02.commons.platform.http.ContextHeaderWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonsWebHeadersAutoConfiguration {

  @Bean
  ContextHeaderWriter contextHeaderWriter() {
    return new SpringContextHeaderWriter();
  }
}

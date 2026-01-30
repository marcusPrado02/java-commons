package com.marcusprado02.commons.adapters.web.spring.config;

import com.marcusprado02.commons.adapters.web.spring.result.SpringHttpResultMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonsWebResultAutoConfiguration {

  @Bean
  SpringHttpResultMapper springHttpResultMapper() {
    return new SpringHttpResultMapper();
  }
}

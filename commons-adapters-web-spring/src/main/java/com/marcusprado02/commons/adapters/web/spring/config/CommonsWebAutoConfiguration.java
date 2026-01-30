package com.marcusprado02.commons.adapters.web.spring.config;

import com.marcusprado02.commons.adapters.web.problem.HttpProblemMapper;
import com.marcusprado02.commons.adapters.web.spring.problem.SpringHttpProblemMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonsWebAutoConfiguration {

  @Bean
  HttpProblemMapper httpProblemMapper() {
    return new SpringHttpProblemMapper();
  }
}

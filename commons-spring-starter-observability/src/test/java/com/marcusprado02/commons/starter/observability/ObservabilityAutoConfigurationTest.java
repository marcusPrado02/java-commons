package com.marcusprado02.commons.starter.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.adapters.web.spring.filter.CorrelationIdFilter;
import com.marcusprado02.commons.adapters.web.spring.filter.RequestContextFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

class ObservabilityAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ObservabilityAutoConfiguration.class));

  @Test
  void shouldRegisterCorrelationIdFilterWithOrderZero() {
    contextRunner.run(
        ctx -> {
          FilterRegistrationBean<?> bean =
              ctx.getBean("correlationIdFilter", FilterRegistrationBean.class);
          assertThat(bean.getFilter()).isInstanceOf(CorrelationIdFilter.class);
          assertThat(bean.getOrder()).isEqualTo(0);
        });
  }

  @Test
  void shouldRegisterRequestContextFilterWithOrderOne() {
    contextRunner.run(
        ctx -> {
          FilterRegistrationBean<?> bean =
              ctx.getBean("requestContextFilter", FilterRegistrationBean.class);
          assertThat(bean.getFilter()).isInstanceOf(RequestContextFilter.class);
          assertThat(bean.getOrder()).isEqualTo(1);
        });
  }
}

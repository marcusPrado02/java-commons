package com.marcusprado02.commons.starter.idempotency;

import com.marcusprado02.commons.app.idempotency.port.IdempotencyStorePort;
import java.lang.reflect.Constructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(name = {
    "jakarta.persistence.EntityManager",
    "com.marcusprado02.commons.adapters.persistence.jpa.idempotency.JpaIdempotencyStoreAdapter"
})
public class IdempotencyJpaStoreAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(IdempotencyStorePort.class)
  public IdempotencyStorePort idempotencyStorePort(ApplicationContext applicationContext) {
    try {
      Class<?> entityManagerClass = Class.forName("jakarta.persistence.EntityManager");
      Object entityManager = applicationContext.getBean(entityManagerClass);

      Class<?> adapterClass =
          Class.forName(
              "com.marcusprado02.commons.adapters.persistence.jpa.idempotency.JpaIdempotencyStoreAdapter");
      Constructor<?> ctor = adapterClass.getConstructor(entityManagerClass);
      return (IdempotencyStorePort) ctor.newInstance(entityManager);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to wire JPA IdempotencyStorePort", ex);
    }
  }
}

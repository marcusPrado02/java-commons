package com.marcusprado02.commons.starter.idempotency;

import com.marcusprado02.commons.app.idempotency.port.IdempotencyStorePort;
import java.lang.reflect.Constructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(
    name = {
      "jakarta.persistence.EntityManager",
      "com.marcusprado02.commons.adapters.persistence.jpa.idempotency.JpaIdempotencyStoreAdapter"
    })
public class IdempotencyJpaStoreAutoConfiguration {

  /**
   * Creates the JPA-backed {@link IdempotencyStorePort} bean via reflection when available.
   *
   * @param applicationContext the Spring application context
   * @return the idempotency store port
   */
  @Bean
  @ConditionalOnMissingBean(IdempotencyStorePort.class)
  public IdempotencyStorePort idempotencyStorePort(ApplicationContext applicationContext) {
    try {
      Class<?> entityManagerClass = Class.forName("jakarta.persistence.EntityManager");
      Object entityManager = applicationContext.getBean(entityManagerClass);

      String adapterClassName =
          "com.marcusprado02.commons.adapters.persistence.jpa.idempotency"
              + ".JpaIdempotencyStoreAdapter";
      Class<?> adapterClass = Class.forName(adapterClassName);
      Constructor<?> ctor = adapterClass.getConstructor(entityManagerClass);
      return (IdempotencyStorePort) ctor.newInstance(entityManager);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to wire JPA IdempotencyStorePort", ex);
    }
  }
}

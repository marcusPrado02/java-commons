package com.marcusprado02.commons.testkit.containers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

class ContainerFactoryTest {

  @Test
  void testPostgres_default_container_is_not_null() {
    assertNotNull(TestPostgres.container());
  }

  @Test
  void testPostgres_versioned_container_is_not_null() {
    assertNotNull(TestPostgres.container("15-alpine"));
  }

  @Test
  void testPostgres_custom_container_is_not_null() {
    assertNotNull(TestPostgres.container("postgres:16-alpine", "mydb", "user", "pass"));
  }

  @Test
  void testKafka_default_container_is_not_null() {
    assertNotNull(TestKafka.container());
  }

  @Test
  void testKafka_versioned_container_is_not_null() {
    assertNotNull(TestKafka.container("7.4.0"));
  }

  @Test
  void testKafka_custom_image_container_is_not_null() {
    assertNotNull(TestKafka.container(DockerImageName.parse("confluentinc/cp-kafka:7.5.0")));
  }
}

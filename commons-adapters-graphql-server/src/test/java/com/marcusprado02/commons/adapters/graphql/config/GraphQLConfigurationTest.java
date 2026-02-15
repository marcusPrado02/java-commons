package com.marcusprado02.commons.adapters.graphql.config;

import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLScalarType;
import graphql.schema.idl.RuntimeWiring;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import static org.junit.jupiter.api.Assertions.*;

class GraphQLConfigurationTest {

  @Test
  void shouldCreateRuntimeWiringConfigurer() {
    GraphQLConfiguration config = new GraphQLConfiguration();
    RuntimeWiringConfigurer configurer = config.runtimeWiringConfigurer();

    assertNotNull(configurer);
  }

  @Test
  void shouldRegisterExtendedScalars() {
    GraphQLConfiguration config = new GraphQLConfiguration();
    RuntimeWiringConfigurer configurer = config.runtimeWiringConfigurer();

    RuntimeWiring.Builder builder = RuntimeWiring.newRuntimeWiring();
    configurer.configure(builder);
    RuntimeWiring wiring = builder.build();

    // Verify extended scalars are registered
    assertNotNull(wiring.getScalars().get("DateTime"));
    assertNotNull(wiring.getScalars().get("Date"));
    assertNotNull(wiring.getScalars().get("Time"));
    assertNotNull(wiring.getScalars().get("LocalTime"));
    assertNotNull(wiring.getScalars().get("Url"));
    assertNotNull(wiring.getScalars().get("UUID"));
    assertNotNull(wiring.getScalars().get("JSON"));
    assertNotNull(wiring.getScalars().get("Object"));
    assertNotNull(wiring.getScalars().get("PositiveInt"));
    assertNotNull(wiring.getScalars().get("NegativeInt"));
    assertNotNull(wiring.getScalars().get("Long"));
    assertNotNull(wiring.getScalars().get("BigDecimal"));
    assertNotNull(wiring.getScalars().get("BigInteger"));
    assertNotNull(wiring.getScalars().get("Char"));
    assertNotNull(wiring.getScalars().get("Locale"));
  }

  @Test
  void shouldHaveCorrectScalarTypes() {
    GraphQLConfiguration config = new GraphQLConfiguration();
    RuntimeWiringConfigurer configurer = config.runtimeWiringConfigurer();

    RuntimeWiring.Builder builder = RuntimeWiring.newRuntimeWiring();
    configurer.configure(builder);
    RuntimeWiring wiring = builder.build();

    GraphQLScalarType dateTime = wiring.getScalars().get("DateTime");
    assertEquals(ExtendedScalars.DateTime.getName(), dateTime.getName());

    GraphQLScalarType uuid = wiring.getScalars().get("UUID");
 assertEquals(ExtendedScalars.UUID.getName(), uuid.getName());
  }
}

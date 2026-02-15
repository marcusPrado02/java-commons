package com.marcusprado02.commons.adapters.graphql.config;

import graphql.scalars.ExtendedScalars;
import graphql.validation.rules.OnValidationErrorStrategy;
import graphql.validation.rules.ValidationRules;
import graphql.validation.schemawiring.ValidationSchemaWiring;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * GraphQL configuration with extended scalars and validation.
 *
 * <p>Provides:
 *
 * <ul>
 *   <li>Extended scalars (DateTime, Date, Time, URL, UUID, etc.)
 *   <li>Validation rules (@Size, @Pattern, @Range, etc.)
 *   <li>Custom error handling
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Configuration
 * @Import(GraphQLConfiguration.class)
 * public class AppConfig {
 *     // Your beans here
 * }
 * }</pre>
 */
@Configuration
public class GraphQLConfiguration {

  /**
   * Configures GraphQL runtime wiring with extended scalars and validation.
   *
   * @return runtime wiring configurer
   */
  @Bean
  public RuntimeWiringConfigurer runtimeWiringConfigurer() {
    ValidationRules validationRules =
        ValidationRules.newValidationRules()
            .onValidationErrorStrategy(OnValidationErrorStrategy.RETURN_NULL)
            .build();

    ValidationSchemaWiring validationSchemaWiring = new ValidationSchemaWiring(validationRules);

    return wiringBuilder ->
        wiringBuilder
            // Extended scalars
            .scalar(ExtendedScalars.DateTime)
            .scalar(ExtendedScalars.Date)
            .scalar(ExtendedScalars.Time)
            .scalar(ExtendedScalars.LocalTime)
            .scalar(ExtendedScalars.Url)
            .scalar(ExtendedScalars.UUID)
            .scalar(ExtendedScalars.Json)
            .scalar(ExtendedScalars.Object)
            .scalar(ExtendedScalars.PositiveInt)
            .scalar(ExtendedScalars.NegativeInt)
            .scalar(ExtendedScalars.NonPositiveInt)
            .scalar(ExtendedScalars.NonNegativeInt)
            .scalar(ExtendedScalars.PositiveFloat)
            .scalar(ExtendedScalars.NegativeFloat)
            .scalar(ExtendedScalars.NonPositiveFloat)
            .scalar(ExtendedScalars.NonNegativeFloat)
            .scalar(ExtendedScalars.GraphQLLong)
            .scalar(ExtendedScalars.GraphQLBigDecimal)
            .scalar(ExtendedScalars.GraphQLBigInteger)
            .scalar(ExtendedScalars.GraphQLChar)
            .scalar(ExtendedScalars.Locale)
            // Validation
            .directiveWiring(validationSchemaWiring);
  }
}

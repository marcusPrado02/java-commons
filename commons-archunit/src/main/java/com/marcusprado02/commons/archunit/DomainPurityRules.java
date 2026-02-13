package com.marcusprado02.commons.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.lang.ArchRule;

/**
 * ArchUnit rules for Domain-Driven Design (DDD) purity.
 *
 * <p>These rules ensure DDD tactical patterns are correctly implemented in the domain layer.
 */
public final class DomainPurityRules {

  private DomainPurityRules() {}

  /**
   * Entities should reside in domain package.
   */
  public static final ArchRule ENTITIES_SHOULD_RESIDE_IN_DOMAIN_PACKAGE =
      classes()
          .that()
          .haveSimpleNameEndingWith("Entity")
          .or()
          .areAnnotatedWith("jakarta.persistence.Entity")
          .should()
          .resideInAPackage("..domain..")
          .because("Entities are domain concepts and should reside in domain package");

  /**
   * Value Objects should be immutable (final class with final fields).
   */
  public static final ArchRule VALUE_OBJECTS_SHOULD_BE_IMMUTABLE =
      classes()
          .that()
          .haveSimpleNameEndingWith("VO")
          .or()
          .haveSimpleNameEndingWith("ValueObject")
          .should()
          .haveOnlyFinalFields()
          .because("Value Objects must be immutable");

  /**
   * Aggregates should not be @Entity directly (only AggregateRoot should).
   */
  public static final ArchRule AGGREGATES_SHOULD_BE_AGGREGATE_ROOTS =
      noClasses()
          .that()
          .haveSimpleNameEndingWith("Aggregate")
          .and()
          .haveSimpleNameNotEndingWith("AggregateRoot")
          .should()
          .beAnnotatedWith("jakarta.persistence.Entity")
          .because("Only AggregateRoot should be annotated with @Entity");

  /**
   * Domain Services should end with 'Service' or 'DomainService'.
   */
  public static final ArchRule DOMAIN_SERVICES_SHOULD_FOLLOW_NAMING =
      classes()
          .that()
          .resideInAPackage("..domain.service..")
          .should()
          .haveSimpleNameEndingWith("Service")
          .orShould()
          .haveSimpleNameEndingWith("DomainService")
          .because("Domain Services should follow naming conventions");

  /**
   * Repositories should be interfaces in domain layer.
   */
  public static final ArchRule REPOSITORIES_SHOULD_BE_INTERFACES =
      classes()
          .that()
          .haveSimpleNameEndingWith("Repository")
          .and()
          .resideInAPackage("..domain..")
          .should()
          .beInterfaces()
          .because("Repositories in domain should be interfaces (ports)");

  /**
   * Domain Events should be immutable.
   */
  public static final ArchRule DOMAIN_EVENTS_SHOULD_BE_IMMUTABLE =
      classes()
          .that()
          .haveSimpleNameEndingWith("Event")
          .or()
          .haveSimpleNameEndingWith("DomainEvent")
          .and()
          .resideInAPackage("..domain..")
          .should()
          .haveOnlyFinalFields()
          .because("Domain Events must be immutable");
}

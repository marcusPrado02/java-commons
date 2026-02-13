package com.marcusprado02.commons.testkit.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Reusable naming convention rules for DDD components.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.mycompany.myapp")
 * public class NamingConventionTest {
 *
 *   @ArchTest
 *   public static final ArchRule repositories_should_be_named_properly =
 *       NamingConventionRules.repositoriesShouldHaveProperNames();
 *
 *   @ArchTest
 *   public static final ArchRule value_objects_should_be_records =
 *       NamingConventionRules.valueObjectsShouldBeRecords();
 * }
 * }</pre>
 */
public final class NamingConventionRules {

  private NamingConventionRules() {}

  /**
   * Repository interfaces should be named *Repository.
   *
   * @return ArchRule
   */
  public static ArchRule repositoriesShouldHaveProperNames() {
    return classes()
        .that()
        .resideInAPackage("..ports.persistence..")
        .and()
        .areInterfaces()
        .and()
        .areNotNestedClasses()
        .should()
        .haveSimpleNameEndingWith("Repository")
        .as("Repository interfaces should be named *Repository");
  }

  /**
   * Port interfaces should be named *Port.
   *
   * @return ArchRule
   */
  public static ArchRule portsShouldHaveProperNames() {
    return classes()
        .that()
        .resideInAPackage("..ports..")
        .and()
        .areInterfaces()
        .and()
        .areNotNestedClasses()
        .should()
        .haveSimpleNameEndingWith("Port")
        .orShould()
        .haveSimpleNameEndingWith("Repository")
        .orShould()
        .haveSimpleNameEndingWith("Specification")
        .as("Port interfaces should be named *Port, *Repository, or *Specification");
  }

  /**
   * Adapter implementations should be named *Adapter.
   *
   * @return ArchRule
   */
  public static ArchRule adaptersShouldHaveProperNames() {
    return classes()
        .that()
        .resideInAPackage("..adapters..")
        .and()
        .areNotInterfaces()
        .and()
        .areNotNestedClasses()
        .and()
        .areTopLevelClasses()
        .should()
        .haveSimpleNameEndingWith("Adapter")
        .orShould()
        .haveSimpleNameEndingWith("Configuration")
        .orShould()
        .haveSimpleNameEndingWith("Controller")
        .as("Adapter classes should be named *Adapter, *Configuration, or *Controller");
  }

  /**
   * Use cases/services in app layer should be named *UseCase or *Service.
   *
   * @return ArchRule
   */
  public static ArchRule useCasesShouldHaveProperNames() {
    return classes()
        .that()
        .resideInAPackage("..app..")
        .and()
        .areNotInterfaces()
        .and()
        .areNotNestedClasses()
        .should()
        .haveSimpleNameEndingWith("UseCase")
        .orShould()
        .haveSimpleNameEndingWith("Service")
        .orShould()
        .haveSimpleNameEndingWith("Handler")
        .as("Application services should be named *UseCase, *Service, or *Handler");
  }

  /**
   * Aggregate roots should be named without specific suffix but should be in kernel.
   *
   * @return ArchRule
   */
  public static ArchRule aggregatesShouldBeInKernel() {
    return classes()
        .that()
        .implement("..kernel.ddd.AggregateRoot")
        .should()
        .resideInAPackage("..kernel..")
        .as("Aggregate roots should reside in kernel package");
  }

  /**
   * Domain events should be named *Event and should be records.
   *
   * @return ArchRule
   */
  public static ArchRule domainEventsShouldBeRecords() {
    return classes()
        .that()
        .implement("..kernel.ddd.DomainEvent")
        .should()
        .beRecords()
        .andShould()
        .haveSimpleNameEndingWith("Event")
        .as("Domain events should be records and named *Event");
  }

  /**
   * Value objects in kernel should be records (immutable).
   *
   * @return ArchRule
   */
  public static ArchRule valueObjectsShouldBeRecords() {
    return classes()
        .that()
        .resideInAPackage("..kernel..")
        .and()
        .haveSimpleNameEndingWith("Id")
        .or()
        .haveSimpleNameEndingWith("Value")
        .should()
        .beRecords()
        .as("Value objects (IDs and values) should be records for immutability");
  }

  /**
   * Validates all naming convention rules at once.
   *
   * @param classes classes to validate
   */
  public static void checkAllNamingRules(JavaClasses classes) {
    repositoriesShouldHaveProperNames().check(classes);
    portsShouldHaveProperNames().check(classes);
    useCasesShouldHaveProperNames().check(classes);
    aggregatesShouldBeInKernel().check(classes);
  }
}

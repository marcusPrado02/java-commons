package com.marcusprado02.commons.testkit.archunit.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * ArchUnit rules enforcing Domain-Driven Design (DDD) conventions.
 *
 * <p>Validates tactical DDD building blocks: Aggregates, Entities, Value Objects, Domain Events,
 * Domain Services, Repositories, and Factories.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.mycompany.myapp")
 * public class DddArchTest {
 *
 *   @ArchTest
 *   public static final ArchRule aggregates_in_domain =
 *       DddRules.aggregatesShouldResideInDomainPackage();
 *
 *   @ArchTest
 *   public static final ArchRule value_objects_immutable =
 *       DddRules.valueObjectsShouldBeImmutable();
 * }
 * }</pre>
 */
public final class DddRules {

  private static final String DOMAIN_PACKAGE = "..domain..";
  private static final String KERNEL_PACKAGE = "..kernel..";
  private static final String EVENT_SUFFIX = "Event";

  private DddRules() {}

  /**
   * DDD: Classes annotated with {@code @AggregateRoot} must reside in domain/kernel package.
   *
   * @return ArchRule
   */
  public static ArchRule aggregatesShouldResideInDomainPackage() {
    return classes()
        .that()
        .areAnnotatedWith("..kernel.ddd.AggregateRoot")
        .or()
        .haveSimpleNameEndingWith("Aggregate")
        .should()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE)
        .allowEmptyShould(true)
        .as("DDD: Aggregate classes must reside in the domain/kernel package");
  }

  /**
   * DDD: Aggregate classes must not be annotated with JPA {@code @Entity} directly.
   *
   * <p>JPA persistence is a concern of the adapter layer. Aggregates must remain pure domain.
   *
   * @return ArchRule
   */
  public static ArchRule aggregatesShouldNotBeJpaEntities() {
    return noClasses()
        .that()
        .haveSimpleNameEndingWith("Aggregate")
        .or()
        .areAnnotatedWith("..kernel.ddd.AggregateRoot")
        .should()
        .beAnnotatedWith("jakarta.persistence.Entity")
        .allowEmptyShould(true)
        .as("DDD: Aggregate classes must not be JPA @Entity (persistence is an adapter concern)");
  }

  // -------------------------------------------------------------------------
  // Value Objects
  // -------------------------------------------------------------------------

  /**
   * DDD: Value Objects should be immutable — implemented as records or final classes.
   *
   * @return ArchRule
   */
  public static ArchRule valueObjectsShouldBeImmutable() {
    return classes()
        .that()
        .haveSimpleNameEndingWith("ValueObject")
        .or()
        .areAnnotatedWith("..kernel.ddd.ValueObject")
        .should()
        .haveModifier(JavaModifier.FINAL)
        .allowEmptyShould(true)
        .as("DDD: Value Objects should be final (immutable)");
  }

  /**
   * DDD: Value Objects should not have setter methods.
   *
   * @return ArchRule
   */
  public static ArchRule valueObjectsShouldNotHaveSetters() {
    return noClasses()
        .that()
        .haveSimpleNameEndingWith("ValueObject")
        .or()
        .areAnnotatedWith("..kernel.ddd.ValueObject")
        .should(
            new ArchCondition<JavaClass>("have no setter methods") {
              @Override
              public void check(JavaClass item, ConditionEvents events) {
                item.getMethods().stream()
                    .filter(m -> m.getName().startsWith("set"))
                    .forEach(
                        m ->
                            events.add(
                                SimpleConditionEvent.violated(
                                    item, item.getName() + " has setter method: " + m.getName())));
              }
            })
        .allowEmptyShould(true)
        .as("DDD: Value Objects must not expose setter methods (must be immutable)");
  }

  // -------------------------------------------------------------------------
  // Domain Events
  // -------------------------------------------------------------------------

  /**
   * DDD: Domain Event classes must reside in domain/kernel package.
   *
   * @return ArchRule
   */
  public static ArchRule domainEventsShouldResideInDomainPackage() {
    return classes()
        .that()
        .haveSimpleNameEndingWith(EVENT_SUFFIX)
        .and()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE)
        .should()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE)
        .allowEmptyShould(true)
        .as("DDD: Domain Event classes must reside in domain/kernel package");
  }

  /**
   * DDD: Domain Event classes should be immutable (final).
   *
   * @return ArchRule
   */
  public static ArchRule domainEventsShouldBeImmutable() {
    return classes()
        .that()
        .haveSimpleNameEndingWith(EVENT_SUFFIX)
        .and()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE)
        .should()
        .haveModifier(JavaModifier.FINAL)
        .allowEmptyShould(true)
        .as("DDD: Domain Events should be final (immutable value objects of time)");
  }

  /**
   * DDD: Domain Events should not carry references to outer-layer types.
   *
   * @return ArchRule
   */
  public static ArchRule domainEventsShouldNotDependOnFrameworks() {
    return noClasses()
        .that()
        .haveSimpleNameEndingWith(EVENT_SUFFIX)
        .and()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..", "jakarta.persistence..", "com.fasterxml.jackson..")
        .allowEmptyShould(true)
        .as("DDD: Domain Events must not depend on frameworks (they are pure domain facts)");
  }

  // -------------------------------------------------------------------------
  // Repositories
  // -------------------------------------------------------------------------

  /**
   * DDD: Repository interfaces must reside in ports package, not in domain.
   *
   * <p>Repositories are defined as Port interfaces and implemented in Adapters.
   *
   * @return ArchRule
   */
  public static ArchRule repositoriesShouldBePortInterfaces() {
    return classes()
        .that()
        .haveSimpleNameEndingWith("Repository")
        .and()
        .areInterfaces()
        .should()
        .resideInAPackage("..ports..")
        .as("DDD: Repository interfaces (contracts) must reside in the ports package");
  }

  /**
   * DDD: Repository implementations must reside in adapters package.
   *
   * @return ArchRule
   */
  public static ArchRule repositoryImplementationsShouldBeInAdapters() {
    return classes()
        .that()
        .haveSimpleNameEndingWith("Repository")
        .and()
        .areNotInterfaces()
        .should()
        .resideInAPackage("..adapters..")
        .allowEmptyShould(true)
        .as("DDD: Repository implementations must reside in the adapters package");
  }

  // -------------------------------------------------------------------------
  // Domain Services
  // -------------------------------------------------------------------------

  /**
   * DDD: Domain Service classes must not depend on infrastructure or web frameworks.
   *
   * @return ArchRule
   */
  public static ArchRule domainServicesShouldBeFrameworkAgnostic() {
    return noClasses()
        .that()
        .haveSimpleNameEndingWith("DomainService")
        .or()
        .areAnnotatedWith("..kernel.ddd.DomainService")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..", "jakarta.persistence..", "com.fasterxml.jackson..")
        .allowEmptyShould(true)
        .as("DDD: Domain Services must be framework-agnostic");
  }

  // -------------------------------------------------------------------------
  // Factories
  // -------------------------------------------------------------------------

  /**
   * DDD: Factory classes must reside in domain/kernel package.
   *
   * @return ArchRule
   */
  public static ArchRule factoriesShouldResideInDomainPackage() {
    return classes()
        .that()
        .haveSimpleNameEndingWith("Factory")
        .and()
        .areNotAnnotatedWith("org.springframework.stereotype.Component")
        .and()
        .areNotAnnotatedWith("org.springframework.stereotype.Bean")
        .should()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE)
        .allowEmptyShould(true)
        .as("DDD: Domain Factory classes must reside in the domain/kernel package");
  }
}

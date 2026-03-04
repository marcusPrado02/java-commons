package com.marcusprado02.commons.testkit.archunit;

import com.marcusprado02.commons.testkit.archunit.architecture.CleanArchitectureRules;
import com.marcusprado02.commons.testkit.archunit.architecture.CqrsRules;
import com.marcusprado02.commons.testkit.archunit.architecture.DddRules;
import com.marcusprado02.commons.testkit.archunit.architecture.DependencyInjectionRules;
import com.marcusprado02.commons.testkit.archunit.architecture.EventDrivenRules;
import com.marcusprado02.commons.testkit.archunit.architecture.HexagonalRules;
import com.marcusprado02.commons.testkit.archunit.patterns.CircuitBreakerRules;
import com.marcusprado02.commons.testkit.archunit.patterns.IdempotencyRules;
import com.marcusprado02.commons.testkit.archunit.patterns.ObservabilityRules;
import com.marcusprado02.commons.testkit.archunit.patterns.OutboxPatternRules;
import com.marcusprado02.commons.testkit.archunit.patterns.TwelveFactorRules;
import com.marcusprado02.commons.testkit.archunit.principles.DryKissYagniRules;
import com.marcusprado02.commons.testkit.archunit.principles.SocCompositionDemeterRules;
import com.marcusprado02.commons.testkit.archunit.solid.SolidRules;
import com.tngtech.archunit.core.domain.JavaClasses;

/**
 * Facade that aggregates all platform architectural rules into a single entry point.
 *
 * <p>This class provides two usage models:
 *
 * <ol>
 *   <li><strong>Selective</strong> — import and use individual rule classes directly from their
 *       sub-packages.
 *   <li><strong>All-in-one</strong> — call {@link #checkAll(JavaClasses)} to apply every rule at
 *       once.
 * </ol>
 *
 * <p><strong>Principles covered:</strong>
 *
 * <ul>
 *   <li>SOLID (SRP, OCP, LSP, ISP, DIP)
 *   <li>DRY, KISS, YAGNI
 *   <li>Separation of Concerns (SoC)
 *   <li>Composition over Inheritance
 *   <li>Law of Demeter
 *   <li>Tell, Don't Ask
 *   <li>Clean Architecture
 *   <li>Hexagonal Architecture (Ports and Adapters)
 *   <li>Domain-Driven Design (DDD)
 *   <li>CQRS
 *   <li>Event-Driven Architecture (EDA)
 *   <li>Dependency Injection (DI) / Inversion of Control (IoC)
 *   <li>12-Factor App
 *   <li>Idempotency Pattern
 *   <li>Transactional Outbox Pattern
 *   <li>Circuit Breaker Pattern
 *   <li>Observability-First Design
 * </ul>
 *
 * <p>Usage (all rules at once):
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.mycompany.myapp")
 * public class FullArchitectureTest {
 *
 *   @Test
 *   void all_architecture_rules_pass() {
 *     JavaClasses classes = new ClassFileImporter()
 *         .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
 *         .importPackages("com.mycompany.myapp");
 *
 *     PlatformArchRules.checkAll(classes);
 *   }
 * }
 * }</pre>
 *
 * <p>Usage (selective, per principle):
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.mycompany.myapp")
 * public class SolidArchTest {
 *
 *   @ArchTest
 *   public static final ArchRule dip =
 *       SolidRules.dipDomainShouldNotDependOnFrameworks();
 *
 *   @ArchTest
 *   public static final ArchRule srp =
 *       SolidRules.srpServicesShouldNotMixStereotypes();
 * }
 * }</pre>
 */
public final class PlatformArchRules {

  private PlatformArchRules() {}

  /**
   * Runs all platform architectural rules against the provided {@link JavaClasses}.
   *
   * <p>Failures are collected and reported together. Each rule carries a clear message explaining
   * which principle was violated and why.
   *
   * @param classes the {@link JavaClasses} to validate (typically imported from project packages)
   */
  public static void checkAll(JavaClasses classes) {
    checkSolid(classes);
    checkDesignPrinciples(classes);
    checkCleanAndHexagonalArchitecture(classes);
    checkDdd(classes);
    checkCqrs(classes);
    checkEventDrivenArchitecture(classes);
    checkDependencyInjection(classes);
    checkPatterns(classes);
  }

  // -------------------------------------------------------------------------
  // SOLID
  // -------------------------------------------------------------------------

  /**
   * Checks all SOLID rules: SRP, OCP, LSP, ISP, DIP.
   *
   * @param classes the classes to check
   */
  public static void checkSolid(JavaClasses classes) {
    SolidRules.srpServicesShouldNotMixStereotypes().check(classes);
    SolidRules.srpDomainClassesShouldNotMixConcerns().check(classes);
    SolidRules.srpClassesShouldHaveLimitedPublicMethods().check(classes);
    SolidRules.ocpAdaptersShouldImplementInterfaces().check(classes);
    SolidRules.ocpUseCasesShouldImplementInterfaces().check(classes);
    SolidRules.lspDomainClassesShouldNotNarrowContracts().check(classes);
    SolidRules.ispPortsShouldHaveFewMethods().check(classes);
    SolidRules.ispDomainInterfacesShouldNotExtendMultiple().check(classes);
    SolidRules.dipDomainShouldNotDependOnFrameworks().check(classes);
    SolidRules.dipApplicationShouldNotDependOnAdapters().check(classes);
  }

  // -------------------------------------------------------------------------
  // DRY / KISS / YAGNI / SoC / Composition / Demeter / Tell Don't Ask
  // -------------------------------------------------------------------------

  /**
   * Checks DRY, KISS, YAGNI, SoC, Composition over Inheritance, Law of Demeter, Tell Don't Ask.
   *
   * @param classes the classes to check
   */
  public static void checkDesignPrinciples(JavaClasses classes) {
    DryKissYagniRules.dryUtilityClassesShouldBeFinal().check(classes);
    DryKissYagniRules.dryUtilityClassesShouldHavePrivateConstructor().check(classes);
    DryKissYagniRules.kissClassesShouldNotHaveTooManyMethods().check(classes);
    DryKissYagniRules.kissNoDeepInheritance().check(classes);
    DryKissYagniRules.kissConstructorsShouldHaveLimitedParameters().check(classes);
    DryKissYagniRules.yagniPortsShouldHaveAtLeastOneAdapter().check(classes);
    DryKissYagniRules.yagniAbstractClassesShouldBeUsed().check(classes);

    SocCompositionDemeterRules.socDomainShouldNotContainFrameworkConcerns().check(classes);
    SocCompositionDemeterRules.socPersistenceShouldOnlyBeInAdapters().check(classes);
    SocCompositionDemeterRules.socMessagingShouldOnlyBeInAdapters().check(classes);
    SocCompositionDemeterRules.socWebConcernsShouldOnlyBeInAdapters().check(classes);
    SocCompositionDemeterRules.compositionAdaptersShouldNotExtendConcreteClasses().check(classes);
    SocCompositionDemeterRules.compositionDomainShouldNotHaveDeepInheritance().check(classes);
    SocCompositionDemeterRules.demeterDomainShouldNotDependOnDistantTypes().check(classes);
    SocCompositionDemeterRules.demeterApplicationShouldOnlyTalkToDirectCollaborators()
        .check(classes);
    SocCompositionDemeterRules.tellDontAskEntitiesShouldExposeBehaivour().check(classes);
  }

  // -------------------------------------------------------------------------
  // Clean Architecture + Hexagonal
  // -------------------------------------------------------------------------

  /**
   * Checks Clean Architecture and Hexagonal Architecture rules.
   *
   * @param classes the classes to check
   */
  public static void checkCleanAndHexagonalArchitecture(JavaClasses classes) {
    CleanArchitectureRules.domainShouldNotDependOnOuterLayers().check(classes);
    CleanArchitectureRules.applicationShouldNotDependOnAdapters().check(classes);
    CleanArchitectureRules.applicationShouldNotDependOnWebFrameworks().check(classes);
    CleanArchitectureRules.portsShouldNotDependOnAdapters().check(classes);
    CleanArchitectureRules.noPackageCycles().check(classes);

    HexagonalRules.portsMustBeInterfaces().check(classes);
    HexagonalRules.adaptersMustImplementPorts().check(classes);
    HexagonalRules.adapterClassesShouldResideInAdaptersPackage().check(classes);
    HexagonalRules.domainMustNotDependOnPortsOrAdapters().check(classes);
    HexagonalRules.adaptersMustNotDependOnOtherAdapters().check(classes);
    HexagonalRules.portInterfacesShouldResideInPortsPackage().check(classes);
  }

  // -------------------------------------------------------------------------
  // DDD
  // -------------------------------------------------------------------------

  /**
   * Checks Domain-Driven Design rules.
   *
   * @param classes the classes to check
   */
  public static void checkDdd(JavaClasses classes) {
    DddRules.aggregatesShouldResideInDomainPackage().check(classes);
    DddRules.aggregatesShouldNotBeJpaEntities().check(classes);
    DddRules.valueObjectsShouldBeImmutable().check(classes);
    DddRules.valueObjectsShouldNotHaveSetters().check(classes);
    DddRules.domainEventsShouldResideInDomainPackage().check(classes);
    DddRules.domainEventsShouldBeImmutable().check(classes);
    DddRules.domainEventsShouldNotDependOnFrameworks().check(classes);
    DddRules.repositoriesShouldBePortInterfaces().check(classes);
    DddRules.repositoryImplementationsShouldBeInAdapters().check(classes);
    DddRules.domainServicesShouldBeFrameworkAgnostic().check(classes);
    DddRules.factoriesShouldResideInDomainPackage().check(classes);
  }

  // -------------------------------------------------------------------------
  // CQRS
  // -------------------------------------------------------------------------

  /**
   * Checks CQRS rules.
   *
   * @param classes the classes to check
   */
  public static void checkCqrs(JavaClasses classes) {
    CqrsRules.commandsShouldResideInAppLayer().check(classes);
    CqrsRules.commandsShouldBeImmutable().check(classes);
    CqrsRules.commandHandlersShouldResideInAppLayer().check(classes);
    CqrsRules.commandHandlersShouldNotDependOnAdapters().check(classes);
    CqrsRules.queriesShouldResideInAppLayer().check(classes);
    CqrsRules.queryHandlersShouldResideInAppLayer().check(classes);
    CqrsRules.queryHandlersShouldNotDependOnAdapters().check(classes);
  }

  // -------------------------------------------------------------------------
  // Event-Driven Architecture
  // -------------------------------------------------------------------------

  /**
   * Checks Event-Driven Architecture rules.
   *
   * @param classes the classes to check
   */
  public static void checkEventDrivenArchitecture(JavaClasses classes) {
    EventDrivenRules.domainEventsShouldBeImmutable().check(classes);
    EventDrivenRules.domainEventsShouldBeFrameworkAgnostic().check(classes);
    EventDrivenRules.domainEventsShouldResideInDomainPackage().check(classes);
    EventDrivenRules.integrationEventsShouldNotBeInDomainPackage().check(classes);
    EventDrivenRules.eventPublishingShouldGoThroughPorts().check(classes);
    EventDrivenRules.eventListenersShouldBeInAdapters().check(classes);
    EventDrivenRules.eventConsumersShouldNotCallDomainDirectly().check(classes);
  }

  // -------------------------------------------------------------------------
  // Dependency Injection / IoC
  // -------------------------------------------------------------------------

  /**
   * Checks Dependency Injection and Inversion of Control rules.
   *
   * @param classes the classes to check
   */
  public static void checkDependencyInjection(JavaClasses classes) {
    DependencyInjectionRules.domainShouldNotInstantiateDependencies().check(classes);
    DependencyInjectionRules.preferConstructorOverFieldInjection().check(classes);
    DependencyInjectionRules.domainClassesShouldNotBeSpringBeans().check(classes);
    DependencyInjectionRules.domainShouldNotUseServiceLocator().check(classes);
    DependencyInjectionRules.configurationClassesShouldNotBeInDomainOrApp().check(classes);
    DependencyInjectionRules.beanDefinitionsShouldOnlyBeInConfigOrStarter().check(classes);
  }

  // -------------------------------------------------------------------------
  // Patterns
  // -------------------------------------------------------------------------

  /**
   * Checks all pattern rules: Outbox, Circuit Breaker, Idempotency, Observability, 12-Factor.
   *
   * @param classes the classes to check
   */
  public static void checkPatterns(JavaClasses classes) {
    // Outbox Pattern
    OutboxPatternRules.outboxClassesShouldResideInAdapters().check(classes);
    OutboxPatternRules.outboxPortShouldResideInPorts().check(classes);
    OutboxPatternRules.domainShouldNotDependOnOutboxImplementation().check(classes);
    OutboxPatternRules.outboxShouldNotPublishDirectlyFromAppLayer().check(classes);

    // Circuit Breaker
    CircuitBreakerRules.circuitBreakerShouldOnlyBeInAdapters().check(classes);
    CircuitBreakerRules.domainShouldNotDependOnResilienceFramework().check(classes);
    CircuitBreakerRules.circuitBreakerClassesShouldResideInAdapters().check(classes);
    CircuitBreakerRules.hystrixShouldNotBeUsed().check(classes);

    // Idempotency
    IdempotencyRules.commandHandlersShouldBeAnnotatedAsIdempotent().check(classes);
    IdempotencyRules.idempotencyKeyClassesShouldBeInAppOrAdapters().check(classes);
    IdempotencyRules.domainShouldNotContainIdempotencyLogic().check(classes);
    IdempotencyRules.idempotencyPortShouldResideInPorts().check(classes);

    // Observability
    ObservabilityRules.tracingAnnotationsShouldOnlyBeInAdapters().check(classes);
    ObservabilityRules.domainShouldNotUseLoggingFrameworks().check(classes);
    ObservabilityRules.metricsInstrumentationShouldOnlyBeInAdapters().check(classes);
    ObservabilityRules.metricsRegistryClassesShouldResideInAdapters().check(classes);
    ObservabilityRules.healthIndicatorsShouldResideInAdapters().check(classes);
    ObservabilityRules.domainShouldNotDependOnActuator().check(classes);

    // 12-Factor App
    TwelveFactorRules.configShouldNotBeHardcoded().check(classes);
    TwelveFactorRules.configurationClassesShouldBeInConfigLayer().check(classes);
    TwelveFactorRules.backingServicesShouldBeAccessedViaPorts().check(classes);
    TwelveFactorRules.servicesShouldNotHaveMutableStaticState().check(classes);
    TwelveFactorRules.domainShouldNotUseLoggingFrameworks().check(classes);
  }
}

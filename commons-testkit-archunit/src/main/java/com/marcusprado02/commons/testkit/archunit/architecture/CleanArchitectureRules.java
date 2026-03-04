package com.marcusprado02.commons.testkit.archunit.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

/**
 * ArchUnit rules enforcing Clean Architecture (Uncle Bob).
 *
 * <p>Dependency rule: source code dependencies must point only inward, toward higher-level
 * policies.
 *
 * <pre>
 *   Frameworks &amp; Drivers  (adapters)
 *        ↓
 *   Interface Adapters    (ports / adapters)
 *        ↓
 *   Application Business Rules (app / application)
 *        ↓
 *   Enterprise Business Rules  (domain / kernel)
 * </pre>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.mycompany.myapp")
 * public class CleanArchTest {
 *
 *   @ArchTest
 *   public static final ArchRule domain_is_independent =
 *       CleanArchitectureRules.domainShouldNotDependOnOuterLayers();
 *
 *   @ArchTest
 *   public static final ArchRule no_cycles =
 *       CleanArchitectureRules.noPackageCycles();
 * }
 * }</pre>
 */
public final class CleanArchitectureRules {

  private static final String DOMAIN_PACKAGE = "..domain..";
  private static final String KERNEL_PACKAGE = "..kernel..";
  private static final String APP_PACKAGE = "..app..";
  private static final String APPLICATION_PACKAGE = "..application..";
  private static final String PORTS_PACKAGE = "..ports..";
  private static final String ADAPTERS_PACKAGE = "..adapters..";

  private CleanArchitectureRules() {}

  /**
   * Enterprise Business Rules (domain/kernel) must not depend on any outer layer.
   *
   * @return ArchRule
   */
  public static ArchRule domainShouldNotDependOnOuterLayers() {
    return noClasses()
        .that()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            PORTS_PACKAGE,
            ADAPTERS_PACKAGE,
            APP_PACKAGE,
            APPLICATION_PACKAGE,
            "..infrastructure..",
            "org.springframework..",
            "jakarta.persistence..")
        .as("Clean Architecture: Domain/kernel must not depend on any outer layer or framework");
  }

  /**
   * Application Business Rules must not depend on Interface Adapters or Frameworks &amp; Drivers.
   *
   * @return ArchRule
   */
  public static ArchRule applicationShouldNotDependOnAdapters() {
    return noClasses()
        .that()
        .resideInAnyPackage(APP_PACKAGE, APPLICATION_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAPackage(ADAPTERS_PACKAGE)
        .as("Clean Architecture: Application layer must not depend on Adapters (outer ring)");
  }

  /**
   * Application layer must not depend on web/HTTP framework classes.
   *
   * @return ArchRule
   */
  public static ArchRule applicationShouldNotDependOnWebFrameworks() {
    return noClasses()
        .that()
        .resideInAnyPackage(APP_PACKAGE, APPLICATION_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework.web..",
            "org.springframework.http..",
            "jakarta.ws.rs..",
            "io.micronaut.http..")
        .as("Clean Architecture: Application layer must not depend on web framework details");
  }

  /**
   * Ports (Interface Adapters ring) must not depend on Adapters (Frameworks &amp; Drivers ring).
   *
   * @return ArchRule
   */
  public static ArchRule portsShouldNotDependOnAdapters() {
    return noClasses()
        .that()
        .resideInAPackage(PORTS_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAPackage(ADAPTERS_PACKAGE)
        .as("Clean Architecture: Ports must not depend on Adapter implementations");
  }

  /**
   * All non-framework classes must reside in one of the four canonical rings.
   *
   * @return ArchRule
   */
  public static ArchRule allClassesShouldResideInCanonicalLayers() {
    return classes()
        .should()
        .resideInAnyPackage(
            DOMAIN_PACKAGE,
            KERNEL_PACKAGE,
            APP_PACKAGE,
            APPLICATION_PACKAGE,
            PORTS_PACKAGE,
            ADAPTERS_PACKAGE,
            "..infrastructure..",
            "..starter..",
            "..config..")
        .as(
            "Clean Architecture: All classes should reside in a canonical architectural layer"
                + " (domain/kernel, app, ports, adapters, infrastructure, starter, config)");
  }

  /**
   * No cyclic dependencies between packages (entire application is free of cycles).
   *
   * @return ArchRule
   */
  public static ArchRule noPackageCycles() {
    return SlicesRuleDefinition.slices()
        .matching("..(*)..")
        .should()
        .beFreeOfCycles()
        .as("Clean Architecture: Packages must be free of cyclic dependencies");
  }
}

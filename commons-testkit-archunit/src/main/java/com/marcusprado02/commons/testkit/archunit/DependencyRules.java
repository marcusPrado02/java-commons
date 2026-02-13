package com.marcusprado02.commons.testkit.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

/**
 * Reusable dependency rules for clean architecture.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.mycompany.myapp")
 * public class DependencyRulesTest {
 *
 *   @ArchTest
 *   public static final ArchRule no_cycles_in_packages =
 *       DependencyRules.noPackageCycles();
 *
 *   @ArchTest
 *   public static final ArchRule ports_should_not_use_spring =
 *       DependencyRules.portsShouldNotDependOnFrameworks();
 * }
 * }</pre>
 */
public final class DependencyRules {

  private DependencyRules() {}

  /**
   * No cyclic dependencies between packages.
   *
   * @return ArchRule
   */
  public static ArchRule noPackageCycles() {
    return SlicesRuleDefinition.slices()
        .matching("..(*)..")
        .should()
        .beFreeOfCycles()
        .as("Packages should not have cyclic dependencies");
  }

  /**
   * Kernel should not depend on any framework (Spring, Jakarta, etc.).
   *
   * @return ArchRule
   */
  public static ArchRule kernelShouldNotDependOnFrameworks() {
    return noClasses()
        .that()
        .resideInAPackage("..kernel..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "jakarta.persistence..",
            "jakarta.validation..",
            "org.hibernate..",
            "com.fasterxml.jackson..")
        .as("Kernel should not depend on frameworks (Spring, Jakarta, etc.)");
  }

  /**
   * Ports should not depend on frameworks (except minimal annotations).
   *
   * @return ArchRule
   */
  public static ArchRule portsShouldNotDependOnFrameworks() {
    return noClasses()
        .that()
        .resideInAPackage("..ports..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "jakarta.persistence..",
            "org.hibernate..",
            "com.fasterxml.jackson..")
        .as("Ports should not depend on frameworks");
  }

  /**
   * Only adapters should depend on Spring Framework.
   *
   * @return ArchRule
   */
  public static ArchRule onlyAdaptersShouldDependOnSpring() {
    return noClasses()
        .that()
        .resideOutsideOfPackages("..adapters..", "..springframework..", "..config..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.springframework..")
        .as("Only adapters and configuration should depend on Spring Framework");
  }

  /**
   * Only adapters should depend on persistence frameworks (JPA/Hibernate).
   *
   * @return ArchRule
   */
  public static ArchRule onlyAdaptersShouldDependOnJpa() {
    return noClasses()
        .that()
        .resideOutsideOfPackages("..adapters..", "..config..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("jakarta.persistence..", "org.hibernate..")
        .as("Only adapters should depend on JPA/Hibernate");
  }

  /**
   * Domain model should not use Java Persistence annotations.
   *
   * @return ArchRule
   */
  public static ArchRule domainShouldNotUseJpaAnnotations() {
    return noClasses()
        .that()
        .resideInAPackage("..kernel..")
        .should()
        .beAnnotatedWith("jakarta.persistence.Entity")
        .orShould()
        .beAnnotatedWith("jakarta.persistence.Table")
        .as("Domain model should not use JPA annotations");
  }

  /**
   * Validates all dependency rules at once.
   *
   * @param classes classes to validate
   */
  public static void checkAllDependencyRules(JavaClasses classes) {
    noPackageCycles().check(classes);
    kernelShouldNotDependOnFrameworks().check(classes);
    portsShouldNotDependOnFrameworks().check(classes);
    onlyAdaptersShouldDependOnSpring().check(classes);
    onlyAdaptersShouldDependOnJpa().check(classes);
  }
}

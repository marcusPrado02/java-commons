package com.marcusprado02.commons.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.lang.ArchRule;

/**
 * ArchUnit rules for Hexagonal Architecture (Ports and Adapters).
 *
 * <p>These rules enforce the dependency direction: Kernel <- Ports <- Application <- Adapters
 */
public final class HexagonalRules {

  private HexagonalRules() {}

  /**
   * Application layer should only depend on Kernel and Ports.
   */
  public static final ArchRule APPLICATION_SHOULD_ONLY_DEPEND_ON_KERNEL_AND_PORTS =
      noClasses()
          .that()
          .resideInAPackage("..app..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..adapters.."
          )
          .because("Application layer should not depend on Adapters (use Ports instead)");

  /**
   * Adapters should only depend on Ports and Kernel (not on Application layer).
   */
  public static final ArchRule ADAPTERS_SHOULD_ONLY_DEPEND_ON_PORTS_AND_KERNEL =
      noClasses()
          .that()
          .resideInAPackage("..adapters..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..app.."
          )
          .because("Adapters should not depend on Application layer");

  /**
   * Spring Starters should only depend on Adapters and Application layers.
   */
  public static final ArchRule STARTERS_SHOULD_ONLY_DEPEND_ON_ADAPTERS_AND_APP =
      noClasses()
          .that()
          .resideInAPackage("..spring.starter..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..kernel..",
              "..ports.."
          )
          .because("Starters should depend on Adapters and App (not directly on Kernel/Ports)");

  /**
   * Ports should only contain interfaces (no implementations).
   */
  public static final ArchRule PORTS_SHOULD_ONLY_CONTAIN_INTERFACES =
      noClasses()
          .that()
          .resideInAPackage("..ports..")
          .and()
          .areNotInterfaces()
          .and()
          .areNotEnums()
          .and()
          .areNotAnnotations()
          .and()
          .haveSimpleNameNotEndingWith("Exception")
          .and()
          .haveSimpleNameNotEndingWith("Config")
          .should()
          .beInterfaces()
          .because("Ports should only contain interfaces (no concrete implementations)");

  /**
   * Adapters should implement Ports interfaces.
   */
  public static final ArchRule ADAPTERS_SHOULD_IMPLEMENT_PORTS =
      noClasses()
          .that()
          .resideInAPackage("..adapters..")
          .and()
          .haveSimpleNameEndingWith("Adapter")
          .should()
          .dependOnClassesThat()
          .resideOutsideOfPackages(
              "..ports..",
              "..kernel..",
              "java..",
              "javax..",
              "jakarta..",
              "org.springframework..",
              "com.fasterxml..",
              "io.opentelemetry..",
              "io.github.resilience4j..",
              "org.hibernate..",
              "..adapters.."
          )
          .because("Adapters should primarily implement Ports and use allowed frameworks");
}

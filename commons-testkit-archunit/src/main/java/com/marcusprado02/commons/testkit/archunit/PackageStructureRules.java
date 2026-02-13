package com.marcusprado02.commons.testkit.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Reusable package structure rules.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.mycompany.myapp")
 * public class PackageStructureTest {
 *
 *   @ArchTest
 *   public static final ArchRule packages_should_be_organized =
 *       PackageStructureRules.kernelAppPortsAdaptersPackagesShouldExist();
 *
 *   @ArchTest
 *   public static final ArchRule no_empty_packages =
 *       PackageStructureRules.packagesShouldNotBeEmpty();
 * }
 * }</pre>
 */
public final class PackageStructureRules {

  private PackageStructureRules() {}

  /**
   * Application should have kernel, app, ports, and adapters packages.
   *
   * @return ArchRule
   */
  public static ArchRule kernelAppPortsAdaptersPackagesShouldExist() {
    return classes()
        .should()
        .resideInAnyPackage("..kernel..", "..app..", "..ports..", "..adapters..")
        .as(
            "Application should be organized in kernel (domain), app (use cases), ports, and"
                + " adapters packages");
  }

  /**
   * Interfaces should reside in ports package.
   *
   * @return ArchRule
   */
  public static ArchRule interfacesShouldBeInPorts() {
    return classes()
        .that()
        .areInterfaces()
        .and()
        .areTopLevelClasses()
        .and()
        .haveSimpleNameEndingWith("Port")
        .should()
        .resideInAPackage("..ports..")
        .as("Port interfaces should reside in ports package");
  }

  /**
   * Domain models should be in kernel package.
   *
   * @return ArchRule
   */
  public static ArchRule domainModelsShouldBeInKernel() {
    return classes()
        .that()
        .implement("..kernel.ddd.Entity")
        .or()
        .implement("..kernel.ddd.AggregateRoot")
        .should()
        .resideInAPackage("..kernel..")
        .as("Domain models (entities and aggregates) should reside in kernel package");
  }

  /**
   * Use cases should be in app package.
   *
   * @return ArchRule
   */
  public static ArchRule useCasesShouldBeInApp() {
    return classes()
        .that()
        .haveSimpleNameEndingWith("UseCase")
        .or()
        .haveSimpleNameEndingWith("Service")
        .should()
        .resideInAnyPackage("..app..", "..adapters..")
        .as("Use cases and services should reside in app or adapters package");
  }

  /**
   * Configurations should be in adapters or config package.
   *
   * @return ArchRule
   */
  public static ArchRule configurationsShouldBeInAdaptersOrConfig() {
    return classes()
        .that()
        .haveSimpleNameEndingWith("Configuration")
        .or()
        .haveSimpleNameEndingWith("Config")
        .should()
        .resideInAnyPackage("..adapters..", "..config..")
        .as("Configuration classes should reside in adapters or config package");
  }

  /**
   * DTOs should not be in kernel package.
   *
   * @return ArchRule
   */
  public static ArchRule dtosShouldNotBeInKernel() {
    return noClasses()
        .that()
        .haveSimpleNameEndingWith("DTO")
        .or()
        .haveSimpleNameEndingWith("Dto")
        .or()
        .haveSimpleNameEndingWith("Request")
        .or()
        .haveSimpleNameEndingWith("Response")
        .should()
        .resideInAPackage("..kernel..")
        .as("DTOs should not be in kernel (domain) package");
  }

  /**
   * Validates all package structure rules at once.
   *
   * @param classes classes to validate
   */
  public static void checkAllPackageRules(JavaClasses classes) {
    kernelAppPortsAdaptersPackagesShouldExist().check(classes);
    interfacesShouldBeInPorts().check(classes);
    domainModelsShouldBeInKernel().check(classes);
    useCasesShouldBeInApp().check(classes);
    configurationsShouldBeInAdaptersOrConfig().check(classes);
    dtosShouldNotBeInKernel().check(classes);
  }
}

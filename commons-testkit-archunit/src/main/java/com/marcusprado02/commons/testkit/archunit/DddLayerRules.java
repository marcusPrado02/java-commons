package com.marcusprado02.commons.testkit.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Reusable architectural rules for DDD/Hexagonal architecture layers.
 *
 * <p>Usage in your tests:
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.mycompany.myapp")
 * public class LayeredArchitectureTest {
 *
 *   @ArchTest
 *   public static final ArchRule kernel_should_not_depend_on_other_layers =
 *       DddLayerRules.kernelShouldNotDependOnOtherLayers();
 *
 *   @ArchTest
 *   public static final ArchRule adapters_should_not_depend_on_other_adapters =
 *       DddLayerRules.adaptersShouldNotDependOnOtherAdapters();
 * }
 * }</pre>
 */
public final class DddLayerRules {

  private DddLayerRules() {}

  /**
   * Kernel/domain layer should not depend on application, ports, or adapters.
   *
   * @return ArchRule
   */
  public static ArchRule kernelShouldNotDependOnOtherLayers() {
    return noClasses()
        .that()
        .resideInAPackage("..kernel..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..app..", "..ports..", "..adapters..")
        .as("Kernel/domain layer should not depend on app, ports, or adapters");
  }

  /**
   * Application layer should not depend on adapters.
   *
   * @return ArchRule
   */
  public static ArchRule appShouldNotDependOnAdapters() {
    return noClasses()
        .that()
        .resideInAPackage("..app..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..adapters..")
        .as("Application layer should not depend on adapters");
  }

  /**
   * Ports should not depend on adapters or application layer.
   *
   * @return ArchRule
   */
  public static ArchRule portsShouldNotDependOnAdaptersOrApp() {
    return noClasses()
        .that()
        .resideInAPackage("..ports..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..adapters..", "..app..")
        .as("Ports should not depend on adapters or application layer");
  }

  /**
   * Adapters should not depend on other adapters.
   *
   * @return ArchRule
   */
  public static ArchRule adaptersShouldNotDependOnOtherAdapters() {
    return noClasses()
        .that()
        .resideInAPackage("..adapters..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..adapters..")
        .as("Adapters should not depend on other adapters");
  }

  /**
   * Application layer can only be accessed by adapters or other application components.
   *
   * @return ArchRule
   */
  public static ArchRule appShouldOnlyBeAccessedByAdaptersOrApp() {
    return classes()
        .that()
        .resideInAPackage("..app..")
        .should()
        .onlyBeAccessed()
        .byAnyPackage("..app..", "..adapters..", "..springframework..")
        .as("Application layer should only be accessed by adapters or other app components");
  }

  /**
   * Validates all DDD layer rules at once.
   *
   * @param classes classes to validate
   */
  public static void checkAllLayerRules(JavaClasses classes) {
    kernelShouldNotDependOnOtherLayers().check(classes);
    appShouldNotDependOnAdapters().check(classes);
    portsShouldNotDependOnAdaptersOrApp().check(classes);
    adaptersShouldNotDependOnOtherAdapters().check(classes);
  }
}

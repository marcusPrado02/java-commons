package com.marcusprado02.commons.archunit.modules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Simplified module validation tests.
 *
 * <p>Validates the most important architectural rules: - Ports do not depend on adapters or
 * application - Adapters do not depend on application - Application does not depend on adapters -
 * Kernel does not depend on outer layers or frameworks
 */
@DisplayName("Module Layer Validation Tests")
class ModuleLayerValidationArchTest {

  private final JavaClasses allClasses =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.marcusprado02.commons");

  @Test
  @DisplayName("Ports should not depend on adapters layer")
  void portsShouldNotDependOnAdapters() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..ports..")
            .should()
            .onlyDependOnClassesThat()
            .resideOutsideOfPackage("..adapters..")
            .because("Ports are inner layer and cannot depend on outer adapters layer");

    rule.check(allClasses);
  }

  @Test
  @DisplayName("Ports should not depend on application layer")
  void portsShouldNotDependOnApplicationLayer() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..ports..")
            .should()
            .onlyDependOnClassesThat()
            .resideOutsideOfPackage("..app..")
            .because("Ports are inner layer and cannot depend on application layer");

    rule.check(allClasses);
  }

  @Test
  @DisplayName("Adapters should not depend on application layer")
  void adaptersShouldNotDependOnApplicationLayer() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..adapters..")
            .should()
            .onlyDependOnClassesThat()
            .resideOutsideOfPackage("..app..")
            .because("Adapters should only depend on ports and kernel, not application layer");

    rule.check(allClasses);
  }

  @Test
  @DisplayName("Application layer should not depend on adapters")
  void appLayerShouldNotDependOnAdapters() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..app..")
            .should()
            .onlyDependOnClassesThat()
            .resideOutsideOfPackage("..adapters..")
            .because("Application layer is inner and cannot depend on outer adapters");

    rule.check(allClasses);
  }

  @Test
  @DisplayName("Application layer should not depend on Spring Framework")
  void appLayerShouldNotDependOnSpring() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..app..")
            .should()
            .onlyDependOnClassesThat()
            .resideOutsideOfPackages("org.springframework..")
            .because("Application layer should be framework-independent");

    rule.check(allClasses);
  }

  @Test
  @DisplayName("Kernel should not depend on Spring Framework")
  void kernelShouldNotDependOnSpring() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..kernel..")
            .should()
            .onlyDependOnClassesThat()
            .resideOutsideOfPackages("org.springframework..")
            .because("Kernel must be framework-independent");

    rule.check(allClasses);
  }

  @Test
  @DisplayName("Kernel should not depend on Jackson")
  void kernelShouldNotDependOnJackson() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..kernel..")
            .should()
            .onlyDependOnClassesThat()
            .resideOutsideOfPackages("com.fasterxml.jackson..")
            .because("Kernel must not depend on serialization libraries");

    rule.check(allClasses);
  }

  @Test
  @DisplayName("Kernel should not depend on outer layers")
  void kernelShouldNotDependOnOuterLayers() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..kernel..")
            .should()
            .onlyDependOnClassesThat()
            .resideOutsideOfPackages("..app..", "..adapters..", "..ports..")
            .because("Kernel is the innermost layer and cannot depend on outer layers");

    rule.check(allClasses);
  }

  @Test
  @DisplayName("Main port interfaces should be interfaces")
  void mainPortInterfacesShouldBeInterfaces() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..ports..")
            .and()
            .haveSimpleNameEndingWith("Port")
            .should()
            .beInterfaces()
            .because("Classes ending with 'Port' should be interface contracts");

    rule.check(allClasses);
  }

  @Test
  @DisplayName("Repository contracts should be interfaces")
  void repositoryContractsShouldBeInterfaces() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..ports.persistence.contract..")
            .and()
            .haveSimpleNameContaining("Repository")
            .should()
            .beInterfaces()
            .because("Repository contracts should be interfaces");

    rule.check(allClasses);
  }
}

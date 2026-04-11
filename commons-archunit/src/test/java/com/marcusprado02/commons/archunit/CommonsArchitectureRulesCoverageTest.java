package com.marcusprado02.commons.archunit;

import com.marcusprado02.commons.archunit.rules.KernelIsolationRules;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Exercises all {@link CommonsArchitectureRules} aggregator methods and the branch paths in
 * anonymous rule conditions that are not hit by {@link PlatformModuleValidationTest}.
 */
class CommonsArchitectureRulesCoverageTest {

  private static final JavaClasses COMMONS_CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.marcusprado02.commons");

  /** Kernel packages known to be free of synthetic anonymous classes. */
  private static final JavaClasses SAFE_KERNEL_CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages(
              "com.marcusprado02.commons.kernel.core",
              "com.marcusprado02.commons.kernel.ddd",
              "com.marcusprado02.commons.kernel.time",
              "com.marcusprado02.commons.kernel.result");

  private static final JavaClasses FIXTURE_CLASSES =
      new ClassFileImporter().importPackages("com.marcusprado02.commons.adapters.fixture");

  // -------------------------------------------------------------------------
  // CommonsArchitectureRules aggregator — just load the methods (line coverage)
  // -------------------------------------------------------------------------

  @Test
  void aggregator_kernelIsolation_returns_non_empty_array() {
    ArchRule[] rules = CommonsArchitectureRules.kernelIsolation();
    Assertions.assertTrue(rules.length > 0);
  }

  @Test
  void aggregator_hexagonal_returns_non_empty_array() {
    ArchRule[] rules = CommonsArchitectureRules.hexagonal();
    Assertions.assertTrue(rules.length > 0);
  }

  @Test
  void aggregator_noCycles_returns_non_empty_array() {
    ArchRule[] rules = CommonsArchitectureRules.noCycles();
    Assertions.assertTrue(rules.length > 0);
  }

  @Test
  void aggregator_domainPurity_returns_non_empty_array() {
    ArchRule[] rules = CommonsArchitectureRules.domainPurity();
    Assertions.assertTrue(rules.length > 0);
  }

  @Test
  void aggregator_namingConventions_returns_non_empty_array() {
    ArchRule[] rules = CommonsArchitectureRules.namingConventions();
    Assertions.assertTrue(rules.length > 0);
  }

  @Test
  void aggregator_testOrganization_returns_non_empty_array() {
    ArchRule[] rules = CommonsArchitectureRules.testOrganization();
    Assertions.assertTrue(rules.length > 0);
  }

  // -------------------------------------------------------------------------
  // KernelIsolationRules$1.test() branches: final → true, abstract → true
  // -------------------------------------------------------------------------

  @Test
  void kernelFinalOrAbstract_passes_for_safe_kernel_packages() {
    // safe kernel packages have both final classes (records) and abstract classes,
    // covering both branches of the `contains(FINAL) || contains(ABSTRACT)` predicate.
    KernelIsolationRules.KERNEL_CLASSES_SHOULD_BE_FINAL_OR_ABSTRACT
        .allowEmptyShould(true)
        .check(SAFE_KERNEL_CLASSES);
  }

  // -------------------------------------------------------------------------
  // NamingConventionRules$1.check() inner-class branch (PACKAGES_SHOULD_BE_LOWERCASE)
  // -------------------------------------------------------------------------

  @Test
  void packagesLowercase_passes_for_commons_classes() {
    // Exercises check() body: pkg != null, !isBlank(), allMatch → ok=true (no violation)
    NamingConventionRules.PACKAGES_SHOULD_BE_LOWERCASE.check(COMMONS_CLASSES);
  }

  // -------------------------------------------------------------------------
  // HexagonalRules$1.check() violation branch (adapter without port)
  // -------------------------------------------------------------------------

  @Test
  void adaptersImplementPorts_detects_adapter_without_port() {
    // NoPortAdapter resides in ..adapters.. and ends with "Adapter" but implements no Port.
    // This exercises the `if (!implementsPort)` true-branch in the custom condition.
    Assertions.assertThrows(
        AssertionError.class,
        () -> HexagonalRules.ADAPTERS_SHOULD_IMPLEMENT_PORTS.check(FIXTURE_CLASSES));
  }
}

package com.marcusprado02.commons.archunit.solid;

import com.marcusprado02.commons.testkit.archunit.solid.SolidRules;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Validates that SOLID ArchUnit rules pass on the commons platform itself.
 *
 * <p>These tests ensure the rules in {@link SolidRules} are green before they can be promoted to
 * mandatory build gates in consuming projects.
 *
 * <p>Rules are checked against the commons kernel and ports — the layers where SOLID violations
 * would be most damaging. Adapter modules are excluded from most checks because they intentionally
 * depend on third-party frameworks.
 */
class SolidRulesArchTest {

  private static JavaClasses KERNEL_CLASSES;
  private static JavaClasses ALL_CLASSES;

  @BeforeAll
  static void importClasses() {
    var importer =
        new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS);
    KERNEL_CLASSES =
        importer.importPackages(
            "com.marcusprado02.commons.kernel", "com.marcusprado02.commons.ports");
    ALL_CLASSES = importer.importPackages("com.marcusprado02.commons");
  }

  // -------------------------------------------------------------------------
  // SRP
  // -------------------------------------------------------------------------

  @Test
  void srp_services_must_not_mix_stereotypes() {
    // No Spring @Service annotations in commons — this rule always passes.
    // Verifies the rule itself is syntactically valid and produces no false positives.
    SolidRules.srpServicesShouldNotMixStereotypes().allowEmptyShould(true).check(ALL_CLASSES);
  }

  @Test
  void srp_domain_classes_must_not_mix_http_or_persistence_concerns() {
    SolidRules.srpDomainClassesShouldNotMixConcerns().allowEmptyShould(true).check(KERNEL_CLASSES);
  }

  // -------------------------------------------------------------------------
  // OCP
  // -------------------------------------------------------------------------

  @Test
  void ocp_adapters_must_implement_interfaces() {
    // Checked against adapters package — adapters ending with "Adapter" must implement a Port.
    SolidRules.ocpAdaptersShouldImplementInterfaces().allowEmptyShould(true).check(ALL_CLASSES);
  }

  // -------------------------------------------------------------------------
  // LSP
  // -------------------------------------------------------------------------

  @Test
  void lsp_domain_classes_must_not_narrow_contracts() {
    SolidRules.lspDomainClassesShouldNotNarrowContracts()
        .allowEmptyShould(true)
        .check(KERNEL_CLASSES);
  }

  // -------------------------------------------------------------------------
  // DIP
  // -------------------------------------------------------------------------

  @Test
  void dip_kernel_must_not_depend_on_frameworks() {
    // Commons kernel must remain framework-free — this is a core architectural invariant.
    SolidRules.dipDomainShouldNotDependOnFrameworks().allowEmptyShould(true).check(KERNEL_CLASSES);
  }

  @Test
  void dip_application_must_not_depend_on_adapters() {
    // Application-layer classes must depend only on Port abstractions.
    SolidRules.dipApplicationShouldNotDependOnAdapters().allowEmptyShould(true).check(ALL_CLASSES);
  }
}

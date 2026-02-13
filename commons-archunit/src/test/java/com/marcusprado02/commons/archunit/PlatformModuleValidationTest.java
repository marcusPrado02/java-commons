package com.marcusprado02.commons.archunit;

import com.marcusprado02.commons.archunit.rules.KernelIsolationRules;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlatformModuleValidationTest {

  private static final ClassFileImporter IMPORTER =
      new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS);

  private static final List<String> KERNEL_MODULE_PACKAGES =
      List.of(
          "com.marcusprado02.commons.kernel.core",
          "com.marcusprado02.commons.kernel.ddd",
          "com.marcusprado02.commons.kernel.errors",
          "com.marcusprado02.commons.kernel.result",
          "com.marcusprado02.commons.kernel.time");

  private static final List<String> PORTS_MODULE_PACKAGES =
      List.of(
          "com.marcusprado02.commons.ports.cache",
          "com.marcusprado02.commons.ports.files",
          "com.marcusprado02.commons.ports.http",
          "com.marcusprado02.commons.ports.messaging",
          "com.marcusprado02.commons.ports.persistence",
          "com.marcusprado02.commons.ports.secrets");

  private static final List<String> APP_MODULE_PACKAGES =
      List.of(
          "com.marcusprado02.commons.app.idempotency",
          "com.marcusprado02.commons.app.observability",
          "com.marcusprado02.commons.app.outbox",
          "com.marcusprado02.commons.app.resilience");

  private static final List<String> ADAPTERS_MODULE_PACKAGES =
      List.of(
          "com.marcusprado02.commons.adapters.otel",
          "com.marcusprado02.commons.adapters.persistence.inmemory",
          "com.marcusprado02.commons.adapters.persistence.jpa",
          "com.marcusprado02.commons.adapters.resilience4j",
          "com.marcusprado02.commons.adapters.web",
          "com.marcusprado02.commons.adapters.web.spring",
          "com.marcusprado02.commons.adapters.web.spring.webflux");

  private static final List<String> STARTER_MODULE_PACKAGES =
      List.of(
          "com.marcusprado02.commons.starter.idempotency",
          "com.marcusprado02.commons.starter.observability",
          "com.marcusprado02.commons.starter.otel",
          "com.marcusprado02.commons.starter.outbox",
          "com.marcusprado02.commons.starter.resilience");

  @Test
  void kernel_modules_follow_kernel_isolation_rules() {
    for (String pkg : KERNEL_MODULE_PACKAGES) {
      var classes = IMPORTER.importPackages(pkg);
      KernelIsolationRules.KERNEL_SHOULD_ONLY_DEPEND_ON_JDK.check(classes);
      KernelIsolationRules.KERNEL_SHOULD_NOT_DEPEND_ON_OUTER_LAYERS.check(classes);
      KernelIsolationRules.KERNEL_SHOULD_NOT_USE_SPRING.check(classes);
      KernelIsolationRules.KERNEL_SHOULD_NOT_USE_JACKSON.check(classes);
    }
  }

  @Test
  void ports_modules_validate_contract_invariants() {
    for (String pkg : PORTS_MODULE_PACKAGES) {
      var classes = IMPORTER.importPackages(pkg);
      HexagonalRules.PORTS_SHOULD_ONLY_CONTAIN_INTERFACES.allowEmptyShould(true).check(classes);
      NamingConventionRules.TYPES_ENDING_WITH_PORT_SHOULD_BE_INTERFACES
          .allowEmptyShould(true)
          .check(classes);
      NamingConventionRules.TYPES_ENDING_WITH_PORT_SHOULD_RESIDE_IN_PORTS_PACKAGE
          .allowEmptyShould(true)
          .check(classes);
    }
  }

  @Test
  void adapters_modules_implement_ports() {
    for (String pkg : ADAPTERS_MODULE_PACKAGES) {
      var classes = IMPORTER.importPackages(pkg);
      HexagonalRules.ADAPTERS_SHOULD_ONLY_DEPEND_ON_PORTS_AND_KERNEL.check(classes);
      HexagonalRules.ADAPTERS_SHOULD_IMPLEMENT_PORTS.allowEmptyShould(true).check(classes);
      NamingConventionRules.TYPES_ENDING_WITH_ADAPTER_SHOULD_RESIDE_IN_ADAPTERS_PACKAGE
          .allowEmptyShould(true)
          .check(classes);
    }
  }

  @Test
  void app_modules_do_not_depend_on_adapters() {
    for (String pkg : APP_MODULE_PACKAGES) {
      var classes = IMPORTER.importPackages(pkg);
      HexagonalRules.APPLICATION_SHOULD_ONLY_DEPEND_ON_KERNEL_AND_PORTS.check(classes);
    }
  }

  @Test
  void starter_modules_do_not_depend_on_kernel_or_ports_directly() {
    for (String pkg : STARTER_MODULE_PACKAGES) {
      var classes = IMPORTER.importPackages(pkg);
      HexagonalRules.STARTERS_SHOULD_ONLY_DEPEND_ON_ADAPTERS_AND_APP.check(classes);
    }
  }

  @Test
  void platform_is_free_of_cycles() {
    var classes = IMPORTER.importPackages("com.marcusprado02.commons");
    NoCyclesRules.NO_CYCLES_BETWEEN_LAYERS.check(classes);
    NoCyclesRules.NO_CYCLES_WITHIN_KERNEL.check(classes);
    NoCyclesRules.NO_CYCLES_WITHIN_APPLICATION.check(classes);
    NoCyclesRules.NO_CYCLES_WITHIN_ADAPTERS.check(classes);
    NoCyclesRules.NO_CYCLES_WITHIN_PORTS.check(classes);
  }
}

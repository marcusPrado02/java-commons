package com.marcusprado02.commons.archunit;

import com.tngtech.archunit.lang.ArchRule;

/**
 * Comprehensive ArchUnit rules for the java-commons platform.
 *
 * <p>This class serves as a convenient aggregator for all architecture validation rules, allowing
 * consumers to reference and apply rules in their tests.
 */
public final class CommonsArchitectureRules {

  private CommonsArchitectureRules() {}

  /** Returns all kernel isolation rules. */
  public static ArchRule[] kernelIsolation() {
    return new ArchRule[] {
      com.marcusprado02.commons.archunit.rules.KernelIsolationRules
          .KERNEL_SHOULD_ONLY_DEPEND_ON_JDK,
      com.marcusprado02.commons.archunit.rules.KernelIsolationRules
          .KERNEL_SHOULD_NOT_DEPEND_ON_OUTER_LAYERS,
      com.marcusprado02.commons.archunit.rules.KernelIsolationRules.KERNEL_SHOULD_NOT_USE_SPRING,
      com.marcusprado02.commons.archunit.rules.KernelIsolationRules.KERNEL_SHOULD_NOT_USE_JACKSON,
      com.marcusprado02.commons.archunit.rules.KernelIsolationRules
          .KERNEL_CLASSES_SHOULD_BE_FINAL_OR_ABSTRACT
    };
  }

  /** Returns all hexagonal architecture rules. */
  public static ArchRule[] hexagonal() {
    return new ArchRule[] {
      HexagonalRules.APPLICATION_SHOULD_ONLY_DEPEND_ON_KERNEL_AND_PORTS,
      HexagonalRules.ADAPTERS_SHOULD_ONLY_DEPEND_ON_PORTS_AND_KERNEL,
      HexagonalRules.STARTERS_SHOULD_ONLY_DEPEND_ON_ADAPTERS_AND_APP,
      HexagonalRules.PORTS_SHOULD_ONLY_CONTAIN_INTERFACES,
      HexagonalRules.ADAPTERS_SHOULD_IMPLEMENT_PORTS
    };
  }

  /** Returns all no-cycles rules. */
  public static ArchRule[] noCycles() {
    return new ArchRule[] {
      NoCyclesRules.NO_CYCLES_BETWEEN_LAYERS,
      NoCyclesRules.NO_CYCLES_WITHIN_KERNEL,
      NoCyclesRules.NO_CYCLES_WITHIN_APPLICATION,
      NoCyclesRules.NO_CYCLES_WITHIN_ADAPTERS,
      NoCyclesRules.NO_CYCLES_WITHIN_PORTS
    };
  }

  /** Returns all domain purity (DDD) rules. */
  public static ArchRule[] domainPurity() {
    return new ArchRule[] {
      DomainPurityRules.ENTITIES_SHOULD_RESIDE_IN_DOMAIN_PACKAGE,
      DomainPurityRules.VALUE_OBJECTS_SHOULD_BE_IMMUTABLE,
      DomainPurityRules.AGGREGATES_SHOULD_BE_AGGREGATE_ROOTS,
      DomainPurityRules.DOMAIN_SERVICES_SHOULD_FOLLOW_NAMING,
      DomainPurityRules.REPOSITORIES_SHOULD_BE_INTERFACES,
      DomainPurityRules.DOMAIN_EVENTS_SHOULD_BE_IMMUTABLE
    };
  }

  /** Returns all naming convention rules. */
  public static ArchRule[] namingConventions() {
    return new ArchRule[] {
      NamingConventionRules.TYPES_ENDING_WITH_PORT_SHOULD_BE_INTERFACES,
      NamingConventionRules.TYPES_ENDING_WITH_PORT_SHOULD_RESIDE_IN_PORTS_PACKAGE,
      NamingConventionRules.TYPES_ENDING_WITH_ADAPTER_SHOULD_RESIDE_IN_ADAPTERS_PACKAGE,
      NamingConventionRules.USE_CASES_SHOULD_END_WITH_USE_CASE,
      NamingConventionRules.CONFIG_CLASSES_SHOULD_FOLLOW_NAMING,
      NamingConventionRules.EXCEPTIONS_SHOULD_END_WITH_EXCEPTION,
      NamingConventionRules.DTOS_SHOULD_FOLLOW_NAMING,
      NamingConventionRules.REST_CONTROLLERS_SHOULD_FOLLOW_NAMING,
      NamingConventionRules.PACKAGES_SHOULD_BE_LOWERCASE
    };
  }

  /** Returns all test organization rules. */
  public static ArchRule[] testOrganization() {
    return new ArchRule[] {
      TestOrganizationRules.TESTS_SHOULD_BE_IN_SAME_PACKAGE_AS_CODE,
      TestOrganizationRules.TEST_CLASSES_SHOULD_END_WITH_TEST,
      TestOrganizationRules.INTEGRATION_TESTS_SHOULD_USE_SPRING_BOOT_TEST,
      TestOrganizationRules.ARCH_TESTS_SHOULD_END_WITH_ARCH_TEST,
      TestOrganizationRules.TEST_CLASSES_SHOULD_NOT_BE_PUBLIC,
      TestOrganizationRules.TEST_METHODS_SHOULD_NOT_BE_PUBLIC,
      TestOrganizationRules.TEST_UTILITIES_SHOULD_BE_IN_SUPPORT_PACKAGE
    };
  }
}

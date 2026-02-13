package com.marcusprado02.commons.archunit;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.lang.ArchRule;

/**
 * ArchUnit rules to prevent cyclic dependencies between modules.
 *
 * <p>Cyclic dependencies make code harder to understand, test, and maintain. These rules ensure
 * clean architecture with unidirectional dependencies.
 */
public final class NoCyclesRules {

  private NoCyclesRules() {}

  /** No cycles between top-level packages (kernel, ports, app, adapters). */
  public static final ArchRule NO_CYCLES_BETWEEN_LAYERS =
      slices()
          .matching("com.marcusprado02.commons.(*)..")
          .should()
          .beFreeOfCycles()
          .because("Cyclic dependencies between layers violate clean architecture principles");

  /** No cycles within kernel modules. */
  public static final ArchRule NO_CYCLES_WITHIN_KERNEL =
      slices()
          .matching("..kernel.(*)..")
          .should()
          .beFreeOfCycles()
          .because("Kernel modules should have clear, acyclic dependencies");

  /** No cycles within application modules. */
  public static final ArchRule NO_CYCLES_WITHIN_APPLICATION =
      slices()
          .matching("..app.(*)..")
          .should()
          .beFreeOfCycles()
          .because("Application modules should have clear, acyclic dependencies");

  /** No cycles within adapter modules. */
  public static final ArchRule NO_CYCLES_WITHIN_ADAPTERS =
      slices()
          .matching("..adapters.(*)..")
          .should()
          .beFreeOfCycles()
          .because("Adapter modules should have clear, acyclic dependencies");

  /** No cycles within ports modules. */
  public static final ArchRule NO_CYCLES_WITHIN_PORTS =
      slices()
          .matching("..ports.(*)..")
          .should()
          .beFreeOfCycles()
          .because("Ports modules should have clear, acyclic dependencies");
}

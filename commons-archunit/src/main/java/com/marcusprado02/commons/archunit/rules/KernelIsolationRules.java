package com.marcusprado02.commons.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

/**
 * ArchUnit rules for Kernel isolation in Hexagonal Architecture.
 *
 * <p>The Kernel (core domain) must be completely framework-agnostic and have no external
 * dependencies except the JDK. This ensures the domain logic remains portable and testable.
 */
public final class KernelIsolationRules {

  private KernelIsolationRules() {}

  /**
   * Kernel modules (commons-kernel-*) should not depend on any external libraries except JDK.
   *
   * <p>Allowed: java.*, javax.*, jakarta.persistence (for JPA annotations only)
   */
  public static final ArchRule KERNEL_SHOULD_ONLY_DEPEND_ON_JDK =
      noClasses()
          .that()
          .resideInAPackage("..kernel..")
          .should()
          .dependOnClassesThat()
          .resideOutsideOfPackages(
              "java..",
              "javax..",
              "jakarta.persistence..", // JPA annotations for entities
              "..kernel.."
          )
          .because(
              "Kernel modules must be framework-agnostic and only depend on JDK (and JPA annotations)");

  /**
   * Kernel should not depend on Ports, Adapters, or Application layers.
   */
  public static final ArchRule KERNEL_SHOULD_NOT_DEPEND_ON_OUTER_LAYERS =
      noClasses()
          .that()
          .resideInAPackage("..kernel..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..ports..",
              "..adapters..",
              "..app..",
              "..application.."
          )
          .because("Kernel must not depend on outer layers (Dependency Inversion Principle)");

  /**
   * Kernel modules should not use Spring Framework annotations.
   */
  public static final ArchRule KERNEL_SHOULD_NOT_USE_SPRING =
      noClasses()
          .that()
          .resideInAPackage("..kernel..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.springframework.."
          )
          .because("Kernel must remain framework-agnostic");

  /**
   * Kernel modules should not use Jackson annotations.
   */
  public static final ArchRule KERNEL_SHOULD_NOT_USE_JACKSON =
      noClasses()
          .that()
          .resideInAPackage("..kernel..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "com.fasterxml.jackson.."
          )
          .because("Kernel must not depend on serialization libraries");

  /**
   * Kernel classes should be final or abstract (prevent unintended inheritance).
   */
  public static final ArchRule KERNEL_CLASSES_SHOULD_BE_FINAL_OR_ABSTRACT =
      classes()
          .that()
          .resideInAPackage("..kernel..")
          .and()
          .areNotInterfaces()
          .and()
          .areNotEnums()
          .and()
          .areNotAnnotations()
          .should()
          .beAssignableTo(
              new com.tngtech.archunit.base.DescribedPredicate<JavaClass>("final or abstract") {
                @Override
                public boolean test(JavaClass javaClass) {
                  return javaClass.getModifiers().contains(com.tngtech.archunit.core.domain.JavaModifier.FINAL)
                      || javaClass.getModifiers().contains(com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT);
                }
              }
          )
          .because("Kernel classes should be final or abstract to prevent unintended inheritance");
}

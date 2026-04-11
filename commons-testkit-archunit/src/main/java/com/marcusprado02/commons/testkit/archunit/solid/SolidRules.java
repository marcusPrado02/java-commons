package com.marcusprado02.commons.testkit.archunit.solid;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * ArchUnit rules enforcing SOLID principles.
 *
 * <p>Designed to be reused by any project that depends on java-commons.
 *
 * <ul>
 *   <li>SRP - Single Responsibility Principle
 *   <li>OCP - Open/Closed Principle
 *   <li>LSP - Liskov Substitution Principle (structural proxy)
 *   <li>ISP - Interface Segregation Principle
 *   <li>DIP - Dependency Inversion Principle
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.mycompany.myapp")
 * public class SolidArchTest {
 *
 *   @ArchTest
 *   public static final ArchRule srp_services =
 *       SolidRules.srpServicesShouldNotMixStereotypes();
 *
 *   @ArchTest
 *   public static final ArchRule dip_domain =
 *       SolidRules.dipDomainShouldNotDependOnFrameworks();
 * }
 * }</pre>
 */
public final class SolidRules {

  private static final int MAX_INTERFACE_METHODS = 5;
  private static final int MAX_CLASS_METHODS = 20;

  private static final String DOMAIN_PACKAGE = "..domain..";
  private static final String KERNEL_PACKAGE = "..kernel..";
  private static final String APP_PACKAGE = "..app..";
  private static final String APPLICATION_PACKAGE = "..application..";
  private static final String ADAPTERS_PACKAGE = "..adapters..";
  private static final String PORTS_PACKAGE = "..ports..";

  private SolidRules() {}

  // -------------------------------------------------------------------------
  // SRP — Single Responsibility Principle
  // -------------------------------------------------------------------------

  /**
   * SRP: A @Service class must not also be annotated with @Repository or @Controller.
   *
   * @return ArchRule
   */
  public static ArchRule srpServicesShouldNotMixStereotypes() {
    return noClasses()
        .that()
        .areAnnotatedWith("org.springframework.stereotype.Service")
        .should()
        .beAnnotatedWith("org.springframework.stereotype.Repository")
        .orShould()
        .beAnnotatedWith("org.springframework.stereotype.Controller")
        .orShould()
        .beAnnotatedWith("org.springframework.web.bind.annotation.RestController")
        .as("SRP: @Service classes must not also be @Repository or @Controller");
  }

  /**
   * SRP: Domain/kernel classes must not contain HTTP or persistence concerns.
   *
   * <p>Classes in {@code ..domain..} or {@code ..kernel..} must not be annotated with
   * {@code @RestController} or {@code @Entity}.
   *
   * @return ArchRule
   */
  public static ArchRule srpDomainClassesShouldNotMixConcerns() {
    return noClasses()
        .that()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE)
        .should()
        .beAnnotatedWith("org.springframework.web.bind.annotation.RestController")
        .orShould()
        .beAnnotatedWith("jakarta.persistence.Entity")
        .as("SRP: Domain/kernel classes must not mix HTTP or persistence concerns");
  }

  /**
   * SRP: Classes in any layer should have at most {@value #MAX_CLASS_METHODS} public methods.
   *
   * <p>This is a structural proxy for single responsibility; large public APIs often indicate
   * multiple responsibilities.
   *
   * @return ArchRule
   */
  public static ArchRule srpClassesShouldHaveLimitedPublicMethods() {
    return classes()
        .that()
        .areTopLevelClasses()
        .and()
        .areNotInterfaces()
        .and()
        .areNotEnums()
        .and()
        .areNotAnnotations()
        .should(
            new ArchCondition<JavaClass>("have at most " + MAX_CLASS_METHODS + " public methods") {
              @Override
              public void check(JavaClass item, ConditionEvents events) {
                long count =
                    item.getMethods().stream()
                        .filter(m -> m.getModifiers().contains(JavaModifier.PUBLIC))
                        .count();
                if (count > MAX_CLASS_METHODS) {
                  events.add(
                      SimpleConditionEvent.violated(
                          item,
                          item.getName()
                              + " has "
                              + count
                              + " public methods (max "
                              + MAX_CLASS_METHODS
                              + ")"));
                }
              }
            })
        .as("SRP: Classes should have at most " + MAX_CLASS_METHODS + " public methods");
  }

  // -------------------------------------------------------------------------
  // OCP — Open/Closed Principle
  // -------------------------------------------------------------------------

  /**
   * OCP: *Adapter classes must implement at least one interface (Port).
   *
   * <p>Adapters should be open for extension via Ports, not via concrete inheritance.
   *
   * @return ArchRule
   */
  public static ArchRule ocpAdaptersShouldImplementInterfaces() {
    return classes()
        .that()
        .resideInAPackage(ADAPTERS_PACKAGE)
        .and()
        .haveSimpleNameEndingWith("Adapter")
        .should(
            new ArchCondition<JavaClass>("implement at least one interface") {
              @Override
              public void check(JavaClass item, ConditionEvents events) {
                if (item.getInterfaces().isEmpty()) {
                  events.add(
                      SimpleConditionEvent.violated(
                          item, item.getName() + " does not implement any Port interface"));
                }
              }
            })
        .as("OCP: *Adapter classes must implement at least one Port interface");
  }

  /**
   * OCP: *Handler and *UseCase classes in app layer must implement an interface.
   *
   * @return ArchRule
   */
  public static ArchRule ocpUseCasesShouldImplementInterfaces() {
    return classes()
        .that()
        .resideInAnyPackage(APP_PACKAGE, APPLICATION_PACKAGE)
        .and()
        .haveSimpleNameEndingWith("UseCase")
        .should(
            new ArchCondition<JavaClass>("implement at least one interface") {
              @Override
              public void check(JavaClass item, ConditionEvents events) {
                if (item.getInterfaces().isEmpty()) {
                  events.add(
                      SimpleConditionEvent.violated(
                          item, item.getName() + " does not implement any interface"));
                }
              }
            })
        .as("OCP: *UseCase classes must implement an interface to stay open for extension");
  }

  // -------------------------------------------------------------------------
  // LSP — Liskov Substitution Principle (structural proxy)
  // -------------------------------------------------------------------------

  /**
   * LSP: Domain classes must not throw {@link UnsupportedOperationException} in overridden methods.
   *
   * <p>This is a <strong>structural proxy</strong> — full LSP verification requires runtime
   * analysis. This rule catches the most common violation pattern.
   *
   * @return ArchRule
   */
  public static ArchRule lspDomainClassesShouldNotNarrowContracts() {
    return noClasses()
        .that()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE)
        .and()
        .areNotInterfaces()
        .should()
        .callConstructorWhere(
            DescribedPredicate.describe(
                "constructs UnsupportedOperationException",
                call -> call.getTargetOwner().isAssignableTo(UnsupportedOperationException.class)))
        .as(
            "LSP: Domain subtypes must not narrow contracts by throwing"
                + " UnsupportedOperationException");
  }

  // -------------------------------------------------------------------------
  // ISP — Interface Segregation Principle
  // -------------------------------------------------------------------------

  /**
   * ISP: Port interfaces must have at most {@value #MAX_INTERFACE_METHODS} methods.
   *
   * <p>Large interfaces force implementors to implement methods they do not need.
   *
   * @return ArchRule
   */
  public static ArchRule ispPortsShouldHaveFewMethods() {
    return classes()
        .that()
        .resideInAPackage(PORTS_PACKAGE)
        .and()
        .areInterfaces()
        .should(
            new ArchCondition<JavaClass>("have at most " + MAX_INTERFACE_METHODS + " methods") {
              @Override
              public void check(JavaClass item, ConditionEvents events) {
                int count = item.getMethods().size();
                if (count > MAX_INTERFACE_METHODS) {
                  events.add(
                      SimpleConditionEvent.violated(
                          item,
                          item.getName()
                              + " has "
                              + count
                              + " methods (max "
                              + MAX_INTERFACE_METHODS
                              + ")"));
                }
              }
            })
        .as(
            "ISP: Port interfaces should be small and focused (≤ "
                + MAX_INTERFACE_METHODS
                + " methods)");
  }

  /**
   * ISP: Interfaces in domain/kernel must not extend more than one interface.
   *
   * @return ArchRule
   */
  public static ArchRule ispDomainInterfacesShouldNotExtendMultiple() {
    return classes()
        .that()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE)
        .and()
        .areInterfaces()
        .should(
            new ArchCondition<JavaClass>("extend at most one interface") {
              @Override
              public void check(JavaClass item, ConditionEvents events) {
                int count = item.getInterfaces().size();
                if (count > 1) {
                  events.add(
                      SimpleConditionEvent.violated(
                          item, item.getName() + " extends " + count + " interfaces (max 1)"));
                }
              }
            })
        .as("ISP: Domain interfaces should not extend multiple interfaces");
  }

  // -------------------------------------------------------------------------
  // DIP — Dependency Inversion Principle
  // -------------------------------------------------------------------------

  /**
   * DIP: High-level modules (domain, kernel, app) must not depend on low-level frameworks.
   *
   * @return ArchRule
   */
  public static ArchRule dipDomainShouldNotDependOnFrameworks() {
    return noClasses()
        .that()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE, APP_PACKAGE, APPLICATION_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "com.fasterxml.jackson..",
            "jakarta.persistence..",
            "org.hibernate..")
        .as("DIP: High-level modules must not depend on low-level framework details");
  }

  /**
   * DIP: Application layer must depend on Port abstractions, not on Adapter implementations.
   *
   * @return ArchRule
   */
  public static ArchRule dipApplicationShouldNotDependOnAdapters() {
    return noClasses()
        .that()
        .resideInAnyPackage(APP_PACKAGE, APPLICATION_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAPackage(ADAPTERS_PACKAGE)
        .as("DIP: Application layer must depend on Port abstractions, not Adapter implementations");
  }
}

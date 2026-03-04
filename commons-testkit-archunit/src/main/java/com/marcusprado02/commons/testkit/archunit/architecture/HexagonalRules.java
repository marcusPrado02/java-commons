package com.marcusprado02.commons.testkit.archunit.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * ArchUnit rules enforcing Hexagonal Architecture (Ports and Adapters).
 *
 * <pre>
 *        ┌───────────────────────────────────────┐
 *        │           Adapters (outer)            │
 *        │  ┌─────────────────────────────────┐  │
 *        │  │        Ports (boundary)         │  │
 *        │  │  ┌───────────────────────────┐  │  │
 *        │  │  │  Application / Use Cases  │  │  │
 *        │  │  │  ┌─────────────────────┐  │  │  │
 *        │  │  │  │  Domain / Kernel    │  │  │  │
 *        │  │  │  └─────────────────────┘  │  │  │
 *        │  │  └───────────────────────────┘  │  │
 *        │  └─────────────────────────────────┘  │
 *        └───────────────────────────────────────┘
 * </pre>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.mycompany.myapp")
 * public class HexagonalArchTest {
 *
 *   @ArchTest
 *   public static final ArchRule ports_are_interfaces =
 *       HexagonalRules.portsMustBeInterfaces();
 *
 *   @ArchTest
 *   public static final ArchRule adapters_implement_ports =
 *       HexagonalRules.adaptersMustImplementPorts();
 * }
 * }</pre>
 */
public final class HexagonalRules {

  private static final String PORTS_PACKAGE = "..ports..";
  private static final String ADAPTERS_PACKAGE = "..adapters..";

  private HexagonalRules() {}

  /**
   * Port classes must be interfaces (boundaries are defined as contracts, not implementations).
   *
   * @return ArchRule
   */
  public static ArchRule portsMustBeInterfaces() {
    return classes()
        .that()
        .resideInAPackage(PORTS_PACKAGE)
        .and()
        .areTopLevelClasses()
        .and()
        .areNotEnums()
        .and()
        .areNotAnnotations()
        .should()
        .beInterfaces()
        .as("Hexagonal: Port classes must be interfaces (define contracts, not implementations)");
  }

  /**
   * *Adapter classes must implement at least one Port interface.
   *
   * <p>Every adapter crossing the hexagon boundary must implement a corresponding Port.
   *
   * @return ArchRule
   */
  public static ArchRule adaptersMustImplementPorts() {
    return classes()
        .that()
        .resideInAPackage(ADAPTERS_PACKAGE)
        .and()
        .haveSimpleNameEndingWith("Adapter")
        .should(
            new ArchCondition<JavaClass>("implement at least one Port interface") {
              @Override
              public void check(JavaClass item, ConditionEvents events) {
                boolean implementsPort =
                    item.getAllRawInterfaces().stream()
                        .anyMatch(i -> i.getPackageName().contains("ports"));
                if (!implementsPort) {
                  events.add(
                      SimpleConditionEvent.violated(
                          item, item.getName() + " does not implement any Port interface"));
                }
              }
            })
        .allowEmptyShould(true)
        .as("Hexagonal: *Adapter classes must implement a Port interface");
  }

  /**
   * Adapters must reside in the adapters package.
   *
   * @return ArchRule
   */
  public static ArchRule adapterClassesShouldResideInAdaptersPackage() {
    return classes()
        .that()
        .haveSimpleNameEndingWith("Adapter")
        .and()
        .areTopLevelClasses()
        .should()
        .resideInAPackage(ADAPTERS_PACKAGE)
        .as("Hexagonal: Classes named *Adapter must reside in an adapters package");
  }

  /**
   * Domain (hexagon core) must not depend on Ports or Adapters.
   *
   * @return ArchRule
   */
  public static ArchRule domainMustNotDependOnPortsOrAdapters() {
    return noClasses()
        .that()
        .resideInAnyPackage("..domain..", "..kernel..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(PORTS_PACKAGE, ADAPTERS_PACKAGE)
        .as("Hexagonal: Domain (hexagon core) must not depend on Ports or Adapters");
  }

  /**
   * Adapters must not depend on other adapters (only on ports and domain).
   *
   * @return ArchRule
   */
  public static ArchRule adaptersMustNotDependOnOtherAdapters() {
    return noClasses()
        .that()
        .resideInAPackage(ADAPTERS_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAPackage(ADAPTERS_PACKAGE)
        .as("Hexagonal: Adapters must not depend on other Adapters");
  }

  /**
   * Port interfaces must reside in the ports package.
   *
   * @return ArchRule
   */
  public static ArchRule portInterfacesShouldResideInPortsPackage() {
    return classes()
        .that()
        .haveSimpleNameEndingWith("Port")
        .and()
        .areInterfaces()
        .should()
        .resideInAPackage(PORTS_PACKAGE)
        .as("Hexagonal: Interfaces named *Port must reside in a ports package");
  }
}

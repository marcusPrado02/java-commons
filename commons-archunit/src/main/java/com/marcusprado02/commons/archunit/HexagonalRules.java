package com.marcusprado02.commons.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * ArchUnit rules for Hexagonal Architecture (Ports and Adapters).
 *
 * <p>These rules enforce the dependency direction: Kernel <- Ports <- Application <- Adapters
 */
public final class HexagonalRules {

  private HexagonalRules() {}

  /** Application layer should only depend on Kernel and Ports. */
  public static final ArchRule APPLICATION_SHOULD_ONLY_DEPEND_ON_KERNEL_AND_PORTS =
      noClasses()
          .that()
          .resideInAPackage("..app..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..adapters..")
          .because("Application layer should not depend on Adapters (use Ports instead)");

  /** Adapters should only depend on Ports and Kernel (not on Application layer). */
  public static final ArchRule ADAPTERS_SHOULD_ONLY_DEPEND_ON_PORTS_AND_KERNEL =
      noClasses()
          .that()
          .resideInAPackage("..adapters..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..starter..")
          .because("Adapters should not depend on Starters (Starters sit at the top of the stack)");

  /** Spring Starters should only depend on Adapters and Application layers. */
  public static final ArchRule STARTERS_SHOULD_ONLY_DEPEND_ON_ADAPTERS_AND_APP =
      noClasses()
          .that()
          .resideInAPackage("..starter..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..ports..")
          .because("Starters should avoid depending directly on Ports (prefer App abstractions)");

  /** Types ending with 'Port' must be interfaces. */
  public static final ArchRule PORTS_SHOULD_ONLY_CONTAIN_INTERFACES =
      classes()
          .that()
          .resideInAPackage("..ports..")
          .and()
          .haveSimpleNameEndingWith("Port")
          .should()
          .beInterfaces()
          .because("Ports are contracts and must be expressed as interfaces");

  /** Adapters should implement Ports interfaces. */
  public static final ArchRule ADAPTERS_SHOULD_IMPLEMENT_PORTS =
      classes()
          .that()
          .resideInAPackage("..adapters..")
          .and()
          .haveSimpleNameEndingWith("Adapter")
          .should(implementAtLeastOnePortInterface())
          .because("Adapters should implement at least one Port interface");

  private static ArchCondition<JavaClass> implementAtLeastOnePortInterface() {
    return new ArchCondition<>("implement at least one Port interface") {
      @Override
      public void check(JavaClass item, ConditionEvents events) {
        boolean implementsPort =
            item.getAllRawInterfaces().stream()
                .anyMatch(itf -> itf.getSimpleName().endsWith("Port"));

        if (!implementsPort) {
          events.add(
              SimpleConditionEvent.violated(
                  item,
                  String.format(
                      "Adapter '%s' should implement at least one interface ending with 'Port'",
                      item.getName())));
        }
      }
    };
  }
}

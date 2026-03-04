package com.marcusprado02.commons.testkit.archunit.patterns;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * ArchUnit rules enforcing the Idempotency Pattern.
 *
 * <p>Idempotency ensures that an operation can be executed multiple times without changing the
 * result beyond the initial application. Critical for:
 *
 * <ul>
 *   <li>Message consumers (at-least-once delivery)
 *   <li>HTTP PUT/DELETE endpoints
 *   <li>Payment and financial operations
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.mycompany.myapp")
 * public class IdempotencyTest {
 *
 *   @ArchTest
 *   public static final ArchRule handlers_are_idempotent =
 *       IdempotencyRules.commandHandlersShouldBeAnnotatedAsIdempotent();
 * }
 * }</pre>
 */
public final class IdempotencyRules {

  private static final String IDEMPOTENCY = "Idempotency";

  private IdempotencyRules() {}

  /**
   * Idempotency: Command Handlers should carry an idempotency annotation.
   *
   * <p>Handlers that process external commands (e.g. from queues or HTTP) must declare their
   * idempotency contract via {@code @Idempotent} or an equivalent annotation.
   *
   * @return ArchRule
   */
  public static ArchRule commandHandlersShouldBeAnnotatedAsIdempotent() {
    return classes()
        .that()
        .haveSimpleNameEndingWith("CommandHandler")
        .should(
            new ArchCondition<JavaClass>("be annotated with @Idempotent or @IdempotencyKey") {
              @Override
              public void check(JavaClass item, ConditionEvents events) {
                boolean isIdempotent =
                    item.getAnnotations().stream()
                        .anyMatch(
                            a ->
                                a.getRawType().getSimpleName().equals("Idempotent")
                                    || a.getRawType().getSimpleName().equals("IdempotencyKey"));
                if (!isIdempotent) {
                  events.add(
                      SimpleConditionEvent.violated(
                          item,
                          item.getName()
                              + " is not annotated with @Idempotent or @IdempotencyKey"));
                }
              }
            })
        .allowEmptyShould(true)
        .as("Idempotency: CommandHandler classes should be annotated with @Idempotent");
  }

  /**
   * Idempotency: Idempotency key handling must reside in app or adapters layer.
   *
   * <p>Classes named *IdempotencyKey or *IdempotencyFilter are infrastructure/application concerns.
   *
   * @return ArchRule
   */
  public static ArchRule idempotencyKeyClassesShouldBeInAppOrAdapters() {
    return classes()
        .that()
        .haveSimpleNameContaining(IDEMPOTENCY)
        .and()
        .areNotInterfaces()
        .and()
        .areNotAnnotations()
        .should()
        .resideInAnyPackage("..app..", "..application..", "..adapters..")
        .allowEmptyShould(true)
        .as("Idempotency: Idempotency implementation classes must reside in app or adapters layer");
  }

  /**
   * Idempotency: Domain classes must not contain idempotency key logic.
   *
   * <p>Idempotency is a cross-cutting concern — the domain must remain agnostic.
   *
   * @return ArchRule
   */
  public static ArchRule domainShouldNotContainIdempotencyLogic() {
    return noClasses()
        .that()
        .resideInAnyPackage("..domain..", "..kernel..")
        .should()
        .haveSimpleNameContaining(IDEMPOTENCY)
        .orShould()
        .haveSimpleNameContaining("IdempotencyKey")
        .as(
            "Idempotency: Domain/kernel must not contain idempotency implementation"
                + " (it is a cross-cutting application/adapter concern)");
  }

  /**
   * Idempotency: Idempotency Port interface must reside in the ports package.
   *
   * @return ArchRule
   */
  public static ArchRule idempotencyPortShouldResideInPorts() {
    return classes()
        .that()
        .haveSimpleNameContaining(IDEMPOTENCY)
        .and()
        .areInterfaces()
        .should()
        .resideInAPackage("..ports..")
        .allowEmptyShould(true)
        .as("Idempotency: Idempotency Port interfaces must reside in the ports package");
  }
}

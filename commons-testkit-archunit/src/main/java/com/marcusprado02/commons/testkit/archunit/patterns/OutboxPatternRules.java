package com.marcusprado02.commons.testkit.archunit.patterns;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.lang.ArchRule;

/**
 * ArchUnit rules enforcing the Transactional Outbox Pattern.
 *
 * <p>The Outbox Pattern guarantees at-least-once delivery of domain events by writing events to an
 * outbox table within the same transaction as the business operation, instead of publishing
 * directly to a message broker.
 *
 * <ul>
 *   <li>Outbox entity must reside in the adapters/persistence layer.
 *   <li>Outbox publisher must use a Port abstraction.
 *   <li>Domain layer must not know about the outbox mechanism.
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.mycompany.myapp")
 * public class OutboxPatternTest {
 *
 *   @ArchTest
 *   public static final ArchRule outbox_in_adapters =
 *       OutboxPatternRules.outboxClassesShouldResideInAdapters();
 * }
 * }</pre>
 */
public final class OutboxPatternRules {

  private OutboxPatternRules() {}

  /**
   * Outbox Entity classes must reside in the adapters/persistence layer.
   *
   * <p>The outbox table is an infrastructure detail — its entity representation belongs in
   * adapters.
   *
   * @return ArchRule
   */
  public static ArchRule outboxClassesShouldResideInAdapters() {
    return classes()
        .that()
        .haveSimpleNameContaining("Outbox")
        .and()
        .areNotInterfaces()
        .should()
        .resideInAPackage("..adapters..")
        .allowEmptyShould(true)
        .as("Outbox Pattern: Outbox implementation classes must reside in the adapters layer");
  }

  /**
   * Outbox Port interface must reside in the ports package.
   *
   * @return ArchRule
   */
  public static ArchRule outboxPortShouldResideInPorts() {
    return classes()
        .that()
        .haveSimpleNameContaining("Outbox")
        .and()
        .areInterfaces()
        .should()
        .resideInAPackage("..ports..")
        .allowEmptyShould(true)
        .as("Outbox Pattern: Outbox Port interfaces must reside in the ports package");
  }

  /**
   * Domain/application layer must not depend on Outbox implementation classes.
   *
   * <p>The application layer should only see the {@code OutboxPort} abstraction.
   *
   * @return ArchRule
   */
  public static ArchRule domainShouldNotDependOnOutboxImplementation() {
    return noClasses()
        .that()
        .resideInAnyPackage("..domain..", "..kernel..", "..app..", "..application..")
        .should()
        .dependOnClassesThat()
        .haveSimpleNameContaining("OutboxEntity")
        .allowEmptyShould(true)
        .as(
            "Outbox Pattern: Domain/application must not depend on OutboxEntity"
                + " (use OutboxPort abstraction)");
  }

  /**
   * Outbox publisher must not publish directly to a message broker from domain/app layer.
   *
   * <p>Publishing must happen via the Outbox relay/dispatcher (adapter), not inline.
   *
   * @return ArchRule
   */
  public static ArchRule outboxShouldNotPublishDirectlyFromAppLayer() {
    return noClasses()
        .that()
        .resideInAnyPackage("..app..", "..application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.apache.kafka.clients.producer..",
            "com.rabbitmq.client..",
            "software.amazon.awssdk.services.sns..",
            "com.azure.messaging.servicebus..")
        .as(
            "Outbox Pattern: Application layer must not publish to brokers directly"
                + " — use the Outbox relay in the adapters layer");
  }
}

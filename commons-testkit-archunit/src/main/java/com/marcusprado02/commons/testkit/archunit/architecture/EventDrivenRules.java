package com.marcusprado02.commons.testkit.archunit.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchRule;

/**
 * ArchUnit rules enforcing Event-Driven Architecture (EDA) conventions.
 *
 * <ul>
 *   <li>Domain Events must be pure domain objects (no framework coupling).
 *   <li>Event publishing must go through a Port (not directly via framework APIs).
 *   <li>Event consumers/listeners must reside in the adapters layer.
 *   <li>Integration Events (crossing bounded contexts) must be separate from Domain Events.
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.mycompany.myapp")
 * public class EventDrivenArchTest {
 *
 *   @ArchTest
 *   public static final ArchRule events_are_immutable =
 *       EventDrivenRules.domainEventsShouldBeImmutable();
 *
 *   @ArchTest
 *   public static final ArchRule listeners_in_adapters =
 *       EventDrivenRules.eventListenersShouldBeInAdapters();
 * }
 * }</pre>
 */
public final class EventDrivenRules {

  private static final String DOMAIN_PACKAGE = "..domain..";
  private static final String KERNEL_PACKAGE = "..kernel..";
  private static final String APP_PACKAGE = "..app..";
  private static final String APPLICATION_PACKAGE = "..application..";
  private static final String ADAPTERS_PACKAGE = "..adapters..";

  private EventDrivenRules() {}

  // -------------------------------------------------------------------------
  // Domain Events
  // -------------------------------------------------------------------------

  /**
   * EDA: Domain Events must be immutable (final classes or records).
   *
   * @return ArchRule
   */
  public static ArchRule domainEventsShouldBeImmutable() {
    return classes()
        .that()
        .haveSimpleNameEndingWith("Event")
        .and()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE)
        .should()
        .haveModifier(JavaModifier.FINAL)
        .allowEmptyShould(true)
        .as("EDA: Domain Events must be final (immutable facts about what happened)");
  }

  /**
   * EDA: Domain Events must not depend on framework classes.
   *
   * @return ArchRule
   */
  public static ArchRule domainEventsShouldBeFrameworkAgnostic() {
    return noClasses()
        .that()
        .haveSimpleNameEndingWith("Event")
        .and()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "jakarta.persistence..",
            "org.apache.kafka..",
            "com.rabbitmq..",
            "software.amazon.awssdk..")
        .allowEmptyShould(true)
        .as("EDA: Domain Events must be framework-agnostic (pure domain facts)");
  }

  /**
   * EDA: Domain Events must reside in the domain/kernel package.
   *
   * @return ArchRule
   */
  public static ArchRule domainEventsShouldResideInDomainPackage() {
    return classes()
        .that()
        .haveSimpleNameEndingWith("DomainEvent")
        .should()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE)
        .allowEmptyShould(true)
        .as("EDA: DomainEvent classes must reside in domain/kernel package");
  }

  // -------------------------------------------------------------------------
  // Integration Events
  // -------------------------------------------------------------------------

  /**
   * EDA: Integration Events must be separated from Domain Events.
   *
   * <p>Integration Events cross bounded context boundaries and may carry serialization annotations.
   * They must not reside in domain/kernel packages.
   *
   * @return ArchRule
   */
  public static ArchRule integrationEventsShouldNotBeInDomainPackage() {
    return noClasses()
        .that()
        .haveSimpleNameEndingWith("IntegrationEvent")
        .should()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE)
        .allowEmptyShould(true)
        .as(
            "EDA: Integration Events must not reside in domain/kernel (they cross context boundaries)");
  }

  // -------------------------------------------------------------------------
  // Event Publishing
  // -------------------------------------------------------------------------

  /**
   * EDA: Domain/application layer must publish events through a Port, not directly via frameworks.
   *
   * <p>Direct use of Spring's {@code ApplicationEventPublisher} or Kafka producers in domain/app
   * layer violates Port isolation.
   *
   * @return ArchRule
   */
  public static ArchRule eventPublishingShouldGoThroughPorts() {
    return noClasses()
        .that()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE, APP_PACKAGE, APPLICATION_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.apache.kafka.clients.producer..",
            "com.rabbitmq.client..",
            "software.amazon.awssdk.services.sns..",
            "software.amazon.awssdk.services.sqs..",
            "com.azure.messaging.servicebus..")
        .as(
            "EDA: Domain/application layers must publish events through Port abstractions,"
                + " not directly via messaging framework clients");
  }

  // -------------------------------------------------------------------------
  // Event Consumers / Listeners
  // -------------------------------------------------------------------------

  /**
   * EDA: Event listener/consumer classes must reside in the adapters layer.
   *
   * <p>Kafka consumers, RabbitMQ listeners, and SQS message handlers are infrastructure concerns.
   *
   * @return ArchRule
   */
  public static ArchRule eventListenersShouldBeInAdapters() {
    return classes()
        .that()
        .areAnnotatedWith("org.springframework.kafka.annotation.KafkaListener")
        .or()
        .areAnnotatedWith("org.springframework.amqp.rabbit.annotation.RabbitListener")
        .or()
        .haveSimpleNameEndingWith("Consumer")
        .or()
        .haveSimpleNameEndingWith("Listener")
        .should()
        .resideInAPackage(ADAPTERS_PACKAGE)
        .allowEmptyShould(true)
        .as("EDA: Event consumers/listeners must reside in the adapters layer");
  }

  /**
   * EDA: Event consumer classes must not depend on domain classes directly (anti-corruption).
   *
   * <p>Consumers should call a Port/UseCase, not domain objects directly, to avoid coupling.
   *
   * @return ArchRule
   */
  public static ArchRule eventConsumersShouldNotCallDomainDirectly() {
    return noClasses()
        .that()
        .resideInAPackage(ADAPTERS_PACKAGE)
        .and()
        .haveSimpleNameEndingWith("Consumer")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE)
        .allowEmptyShould(true)
        .as(
            "EDA: Event consumers must call through Port/UseCase, not depend on domain classes"
                + " directly");
  }
}

package com.marcusprado02.commons.testkit.archunit.patterns;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * ArchUnit rules enforcing the 12-Factor App methodology.
 *
 * <p>The 12-Factor App is a methodology for building software-as-a-service applications. The
 * statically verifiable factors are:
 *
 * <ul>
 *   <li>Factor III — Configuration: Config must come from environment, not hardcoded.
 *   <li>Factor IV — Backing Services: External services accessed via Port abstractions.
 *   <li>Factor VI — Processes: Stateless processes — no in-memory state between requests.
 *   <li>Factor X — Dev/Prod Parity: No dev-only adaptation leaking into production code.
 *   <li>Factor XI — Logs: Treat logs as event streams (adapters, not domain).
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.mycompany.myapp")
 * public class TwelveFactorTest {
 *
 *   @ArchTest
 *   public static final ArchRule no_hardcoded_config =
 *       TwelveFactorRules.configShouldNotBeHardcoded();
 *
 *   @ArchTest
 *   public static final ArchRule backing_services_via_ports =
 *       TwelveFactorRules.backingServicesShouldBeAccessedViaPorts();
 * }
 * }</pre>
 */
public final class TwelveFactorRules {

  private static final String DOMAIN_PACKAGE = "..domain..";
  private static final String KERNEL_PACKAGE = "..kernel..";
  private static final String APP_PACKAGE = "..app..";
  private static final String APPLICATION_PACKAGE = "..application..";

  private TwelveFactorRules() {}

  // -------------------------------------------------------------------------
  // Factor III — Configuration
  // -------------------------------------------------------------------------

  /**
   * Factor III: Configuration must not be hardcoded in domain or application classes.
   *
   * <p>Domain/kernel and app layer must not access system properties or OS environment variables
   * directly. All configuration should be injected via constructor or {@code @Value}.
   *
   * @return ArchRule
   */
  public static ArchRule configShouldNotBeHardcoded() {
    return noClasses()
        .that()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE, APP_PACKAGE, APPLICATION_PACKAGE)
        .should()
        .callMethod(System.class, "getenv", String.class)
        .orShould()
        .callMethod(System.class, "getProperty", String.class)
        .orShould()
        .callMethod(System.class, "getProperty", String.class, String.class)
        .as(
            "12-Factor III: Domain/application classes must not read config via"
                + " System.getenv/getProperty — inject config via constructor or @Value");
  }

  /**
   * Factor III: Configuration classes must reside in adapters/starter/config package.
   *
   * @return ArchRule
   */
  public static ArchRule configurationClassesShouldBeInConfigLayer() {
    return classes()
        .that()
        .areAnnotatedWith("org.springframework.context.annotation.Configuration")
        .or()
        .areAnnotatedWith("org.springframework.boot.context.properties.ConfigurationProperties")
        .should()
        .resideInAnyPackage("..adapters..", "..starter..", "..config..", "..infra..")
        .allowEmptyShould(true)
        .as(
            "12-Factor III: @Configuration and @ConfigurationProperties classes must reside"
                + " in adapters/starter/config/infra packages");
  }

  // -------------------------------------------------------------------------
  // Factor IV — Backing Services
  // -------------------------------------------------------------------------

  /**
   * Factor IV: Backing services (DB, cache, queue) must be accessed through Port abstractions.
   *
   * <p>The application must treat all backing services as attached resources, accessed only via
   * Port interfaces, so they can be swapped without code changes.
   *
   * @return ArchRule
   */
  public static ArchRule backingServicesShouldBeAccessedViaPorts() {
    return noClasses()
        .that()
        .resideInAnyPackage(APP_PACKAGE, APPLICATION_PACKAGE, DOMAIN_PACKAGE, KERNEL_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "jakarta.persistence..",
            "org.springframework.data..",
            "org.springframework.kafka..",
            "org.springframework.amqp..",
            "org.springframework.data.redis..",
            "software.amazon.awssdk..",
            "com.azure..",
            "com.google.cloud..")
        .as(
            "12-Factor IV: Backing services must be accessed through Port abstractions,"
                + " not directly via infrastructure SDK classes");
  }

  // -------------------------------------------------------------------------
  // Factor VI — Stateless Processes
  // -------------------------------------------------------------------------

  /**
   * Factor VI: Application service classes must not have mutable static state.
   *
   * <p>Mutable static fields in services break statelessness across requests.
   *
   * @return ArchRule
   */
  public static ArchRule servicesShouldNotHaveMutableStaticState() {
    return noClasses()
        .that()
        .resideInAnyPackage(APP_PACKAGE, APPLICATION_PACKAGE, "..adapters..")
        .and()
        .areAnnotatedWith("org.springframework.stereotype.Service")
        .should(
            new ArchCondition<JavaClass>("not have mutable static fields") {
              @Override
              public void check(JavaClass item, ConditionEvents events) {
                item.getFields().stream()
                    .filter(
                        field ->
                            field.getModifiers().contains(JavaModifier.STATIC)
                                && !field.getModifiers().contains(JavaModifier.FINAL))
                    .forEach(
                        field ->
                            events.add(
                                SimpleConditionEvent.violated(
                                    item,
                                    item.getName()
                                        + " has mutable static field: "
                                        + field.getName())));
              }
            })
        .allowEmptyShould(true)
        .as(
            "12-Factor VI: @Service classes must not have mutable static fields"
                + " (processes must be stateless)");
  }

  // -------------------------------------------------------------------------
  // Factor XI — Logs as Event Streams
  // -------------------------------------------------------------------------

  /**
   * Factor XI: Domain classes must not contain logging framework calls.
   *
   * <p>Logs are an event stream concern, not a domain concern. Domain errors and events are
   * sufficient; logging adapter decorates them.
   *
   * @return ArchRule
   */
  public static ArchRule domainShouldNotUseLoggingFrameworks() {
    return noClasses()
        .that()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.slf4j..", "org.apache.logging..", "ch.qos.logback..", "java.util.logging..")
        .as(
            "12-Factor XI: Domain/kernel must not use logging frameworks"
                + " (treat logs as event streams at the adapter boundary)");
  }
}

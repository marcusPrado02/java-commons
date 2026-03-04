package com.marcusprado02.commons.testkit.archunit.patterns;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.lang.ArchRule;

/**
 * ArchUnit rules enforcing Observability-First Design.
 *
 * <p>Observability (tracing, metrics, logging) is a cross-cutting infrastructure concern. Signals
 * must be emitted at the boundary (adapters), never baked into the domain core.
 *
 * <ul>
 *   <li>Tracing and metrics instrumentation belong in adapters.
 *   <li>Domain/kernel must not contain logging framework calls.
 *   <li>Span/trace annotations must only appear in adapters or app layer.
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.mycompany.myapp")
 * public class ObservabilityTest {
 *
 *   @ArchTest
 *   public static final ArchRule tracing_only_in_adapters =
 *       ObservabilityRules.tracingAnnotationsShouldOnlyBeInAdapters();
 *
 *   @ArchTest
 *   public static final ArchRule domain_no_logging_framework =
 *       ObservabilityRules.domainShouldNotUseLoggingFrameworks();
 * }
 * }</pre>
 */
public final class ObservabilityRules {

  private static final String DOMAIN_PACKAGE = "..domain..";
  private static final String KERNEL_PACKAGE = "..kernel..";

  private ObservabilityRules() {}

  // -------------------------------------------------------------------------
  // Tracing
  // -------------------------------------------------------------------------

  /**
   * Observability: OpenTelemetry {@code @WithSpan} and tracing annotations must only appear in
   * adapters or app layer.
   *
   * @return ArchRule
   */
  public static ArchRule tracingAnnotationsShouldOnlyBeInAdapters() {
    return noClasses()
        .that()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE)
        .should()
        .beAnnotatedWith("io.opentelemetry.instrumentation.annotations.WithSpan")
        .orShould()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "io.opentelemetry..", "io.micrometer.tracing..", "brave..", "io.jaegertracing..")
        .as(
            "Observability: Tracing instrumentation must not exist in domain/kernel"
                + " (it belongs in adapters/app layer)");
  }

  /**
   * Observability: Domain/kernel must not use logging frameworks directly.
   *
   * <p>Logging is an infrastructure concern. Domain classes should not depend on SLF4J, Log4j, or
   * Logback. Use structured events or domain errors instead.
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
            "org.slf4j..",
            "org.apache.logging..",
            "ch.qos.logback..",
            "org.apache.log4j..",
            "java.util.logging..")
        .as(
            "Observability: Domain/kernel must not use logging frameworks"
                + " (emit domain events or errors instead)");
  }

  // -------------------------------------------------------------------------
  // Metrics
  // -------------------------------------------------------------------------

  /**
   * Observability: Micrometer metrics instrumentation must only reside in adapters or app layer.
   *
   * @return ArchRule
   */
  public static ArchRule metricsInstrumentationShouldOnlyBeInAdapters() {
    return noClasses()
        .that()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAPackage("io.micrometer..")
        .as(
            "Observability: Micrometer metrics must not be used in domain/kernel"
                + " (metrics are an adapter/app concern)");
  }

  /**
   * Observability: Prometheus/metrics registry must reside in adapters layer.
   *
   * @return ArchRule
   */
  public static ArchRule metricsRegistryClassesShouldResideInAdapters() {
    return classes()
        .that()
        .haveSimpleNameContaining("Metrics")
        .or()
        .haveSimpleNameContaining("MetricsCollector")
        .or()
        .haveSimpleNameContaining("MeterRegistry")
        .and()
        .areNotInterfaces()
        .should()
        .resideInAPackage("..adapters..")
        .allowEmptyShould(true)
        .as("Observability: Metrics registry/collector classes must reside in the adapters layer");
  }

  // -------------------------------------------------------------------------
  // Health Checks
  // -------------------------------------------------------------------------

  /**
   * Observability: Health check indicators must reside in adapters layer.
   *
   * @return ArchRule
   */
  public static ArchRule healthIndicatorsShouldResideInAdapters() {
    return classes()
        .that()
        .implement("org.springframework.boot.actuate.health.HealthIndicator")
        .should()
        .resideInAPackage("..adapters..")
        .allowEmptyShould(true)
        .as("Observability: Spring Boot HealthIndicator implementations must reside in adapters");
  }

  /**
   * Observability: Domain classes must not depend on Actuator/health check classes.
   *
   * @return ArchRule
   */
  public static ArchRule domainShouldNotDependOnActuator() {
    return noClasses()
        .that()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.springframework.boot.actuate..")
        .as("Observability: Domain/kernel must not depend on Spring Boot Actuator");
  }
}

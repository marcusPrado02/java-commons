package com.marcusprado02.commons.testkit.archunit.patterns;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.lang.ArchRule;

/**
 * ArchUnit rules enforcing the Circuit Breaker Pattern.
 *
 * <p>Circuit Breaker is a resilience pattern that detects failures and stops cascading errors by
 * short-circuiting calls to failing services. It is an infrastructure concern and must not leak
 * into the domain or application layer.
 *
 * <ul>
 *   <li>Circuit Breaker annotations/configurations must only appear in adapters.
 *   <li>Domain/app layer must not depend on resilience framework classes.
 *   <li>Circuit Breaker fallback methods must reside in adapters.
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.mycompany.myapp")
 * public class CircuitBreakerTest {
 *
 *   @ArchTest
 *   public static final ArchRule circuit_breaker_only_in_adapters =
 *       CircuitBreakerRules.circuitBreakerShouldOnlyBeInAdapters();
 * }
 * }</pre>
 */
public final class CircuitBreakerRules {

  private CircuitBreakerRules() {}

  /**
   * Circuit Breaker annotations must only appear in the adapters layer.
   *
   * <p>Resilience4j's {@code @CircuitBreaker}, {@code @Retry}, {@code @TimeLimiter},
   * {@code @Bulkhead}, and {@code @RateLimiter} annotations are infrastructure concerns.
   *
   * @return ArchRule
   */
  public static ArchRule circuitBreakerShouldOnlyBeInAdapters() {
    return noClasses()
        .that()
        .resideOutsideOfPackage("..adapters..")
        .should()
        .beAnnotatedWith("io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker")
        .orShould()
        .beAnnotatedWith("io.github.resilience4j.retry.annotation.Retry")
        .orShould()
        .beAnnotatedWith("io.github.resilience4j.timelimiter.annotation.TimeLimiter")
        .orShould()
        .beAnnotatedWith("io.github.resilience4j.bulkhead.annotation.Bulkhead")
        .orShould()
        .beAnnotatedWith("io.github.resilience4j.ratelimiter.annotation.RateLimiter")
        .allowEmptyShould(true)
        .as("Circuit Breaker: Resilience4j annotations must only be used in the adapters layer");
  }

  /**
   * Domain/application layer must not depend on Resilience4j classes.
   *
   * @return ArchRule
   */
  public static ArchRule domainShouldNotDependOnResilienceFramework() {
    return noClasses()
        .that()
        .resideInAnyPackage("..domain..", "..kernel..", "..app..", "..application..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("io.github.resilience4j..")
        .as(
            "Circuit Breaker: Domain/application layer must not depend on Resilience4j"
                + " (resilience is an adapter concern)");
  }

  /**
   * Circuit Breaker classes (wrappers/decorators) must reside in the adapters layer.
   *
   * @return ArchRule
   */
  public static ArchRule circuitBreakerClassesShouldResideInAdapters() {
    return classes()
        .that()
        .haveSimpleNameContaining("CircuitBreaker")
        .and()
        .areNotInterfaces()
        .should()
        .resideInAPackage("..adapters..")
        .allowEmptyShould(true)
        .as(
            "Circuit Breaker: CircuitBreaker implementation/wrapper classes must reside"
                + " in the adapters layer");
  }

  /**
   * Hystrix (legacy) must not be used — prefer Resilience4j.
   *
   * @return ArchRule
   */
  public static ArchRule hystrixShouldNotBeUsed() {
    return noClasses()
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.netflix.hystrix..")
        .as("Circuit Breaker: Hystrix is deprecated — use Resilience4j for circuit breaking");
  }
}

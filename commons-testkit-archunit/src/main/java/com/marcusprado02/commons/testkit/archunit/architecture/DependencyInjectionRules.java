package com.marcusprado02.commons.testkit.archunit.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * ArchUnit rules enforcing Dependency Injection (DI) and Inversion of Control (IoC).
 *
 * <ul>
 *   <li>DI: Dependencies must be injected, not instantiated with {@code new} inside domain/app.
 *   <li>IoC: High-level modules must not control the lifecycle of low-level modules.
 *   <li>Constructor injection is preferred over field injection.
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.mycompany.myapp")
 * public class DiIocArchTest {
 *
 *   @ArchTest
 *   public static final ArchRule no_field_injection =
 *       DependencyInjectionRules.preferConstructorOverFieldInjection();
 *
 *   @ArchTest
 *   public static final ArchRule domain_no_service_lookup =
 *       DependencyInjectionRules.domainShouldNotUseServiceLocator();
 * }
 * }</pre>
 */
public final class DependencyInjectionRules {

  private static final String DOMAIN_PACKAGE = "..domain..";
  private static final String KERNEL_PACKAGE = "..kernel..";
  private static final String APP_PACKAGE = "..app..";
  private static final String APPLICATION_PACKAGE = "..application..";

  private DependencyInjectionRules() {}

  // -------------------------------------------------------------------------
  // DI — Dependency Injection
  // -------------------------------------------------------------------------

  /**
   * DI: Domain/kernel classes must not instantiate their dependencies with {@code new}.
   *
   * <p>Collaborators must be injected, not created internally. This rule detects direct
   * instantiation of Port or Service types.
   *
   * @return ArchRule
   */
  public static ArchRule domainShouldNotInstantiateDependencies() {
    return noClasses()
        .that()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE, APP_PACKAGE, APPLICATION_PACKAGE)
        .should()
        .callConstructorWhere(
            DescribedPredicate.describe(
                "constructs a class from ports or adapters",
                call ->
                    call.getTargetOwner().getPackageName().contains("ports")
                        || call.getTargetOwner().getPackageName().contains("adapters")))
        .as(
            "DI: Domain/application classes must not instantiate Port or Adapter"
                + " dependencies directly (use constructor injection)");
  }

  /**
   * DI: Prefer constructor injection — Spring's {@code @Autowired} on fields is discouraged.
   *
   * <p>Field injection makes classes harder to test and hides dependencies.
   *
   * @return ArchRule
   */
  public static ArchRule preferConstructorOverFieldInjection() {
    return noClasses()
        .that()
        .areNotAnnotations()
        .should(
            new ArchCondition<JavaClass>("not have @Autowired/@Inject field injection") {
              @Override
              public void check(JavaClass item, ConditionEvents events) {
                item.getFields().stream()
                    .filter(
                        field ->
                            field.isAnnotatedWith(
                                    "org.springframework.beans.factory.annotation.Autowired")
                                || field.isAnnotatedWith("javax.inject.Inject")
                                || field.isAnnotatedWith("jakarta.inject.Inject"))
                    .forEach(
                        field ->
                            events.add(
                                SimpleConditionEvent.violated(
                                    item,
                                    item.getName()
                                        + " has field "
                                        + field.getName()
                                        + " annotated with @Autowired/@Inject")));
              }
            })
        .allowEmptyShould(true)
        .as("DI: Field injection via @Autowired/@Inject is discouraged; use constructor injection");
  }

  /**
   * DI: Spring components in domain/kernel are forbidden.
   *
   * <p>Domain classes must not be Spring-managed beans — they are plain objects instantiated by
   * Factories or the application layer.
   *
   * @return ArchRule
   */
  public static ArchRule domainClassesShouldNotBeSpringBeans() {
    return noClasses()
        .that()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE)
        .should()
        .beAnnotatedWith("org.springframework.stereotype.Component")
        .orShould()
        .beAnnotatedWith("org.springframework.stereotype.Service")
        .orShould()
        .beAnnotatedWith("org.springframework.stereotype.Repository")
        .as("DI: Domain/kernel classes must not be Spring-managed beans (@Component/@Service)");
  }

  // -------------------------------------------------------------------------
  // IoC — Inversion of Control
  // -------------------------------------------------------------------------

  /**
   * IoC: Domain/application layer must not use Spring's {@code ApplicationContext} as a Service
   * Locator.
   *
   * <p>Pulling dependencies from the context at runtime couples high-level modules to the
   * container, violating IoC.
   *
   * @return ArchRule
   */
  public static ArchRule domainShouldNotUseServiceLocator() {
    return noClasses()
        .that()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE, APP_PACKAGE, APPLICATION_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("org.springframework.context..", "org.springframework.beans.factory..")
        .as(
            "IoC: Domain/application classes must not use ApplicationContext/BeanFactory as a"
                + " Service Locator");
  }

  /**
   * IoC: Configuration classes (factories, beans) must reside in the adapters, starter, or config
   * package — not in domain or app.
   *
   * @return ArchRule
   */
  public static ArchRule configurationClassesShouldNotBeInDomainOrApp() {
    return noClasses()
        .that()
        .areAnnotatedWith("org.springframework.context.annotation.Configuration")
        .should()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE, APP_PACKAGE, APPLICATION_PACKAGE)
        .as(
            "IoC: @Configuration classes must not reside in domain/kernel/app"
                + " (they belong in adapters/starter/config)");
  }

  /**
   * IoC: {@code @Bean} factory methods must only appear in Configuration or Starter classes.
   *
   * @return ArchRule
   */
  public static ArchRule beanDefinitionsShouldOnlyBeInConfigOrStarter() {
    return classes()
        .that()
        .areAnnotatedWith("org.springframework.context.annotation.Configuration")
        .should()
        .resideInAnyPackage("..adapters..", "..starter..", "..config..", "..infra..")
        .allowEmptyShould(true)
        .as("IoC: @Configuration classes should reside in adapters/starter/config/infra packages");
  }
}

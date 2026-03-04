package com.marcusprado02.commons.testkit.archunit.principles;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * ArchUnit rules for:
 *
 * <ul>
 *   <li>Separation of Concerns (SoC)
 *   <li>Composition over Inheritance
 *   <li>Law of Demeter
 *   <li>Tell, Don't Ask
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.mycompany.myapp")
 * public class DesignPrinciplesTest {
 *
 *   @ArchTest
 *   public static final ArchRule soc_no_framework_in_domain =
 *       SocCompositionDemeterRules.socDomainShouldNotContainFrameworkConcerns();
 *
 *   @ArchTest
 *   public static final ArchRule composition_over_inheritance =
 *       SocCompositionDemeterRules.compositionAdaptersShouldNotExtendConcreteClasses();
 * }
 * }</pre>
 */
public final class SocCompositionDemeterRules {

  private static final String DOMAIN_PACKAGE = "..domain..";
  private static final String KERNEL_PACKAGE = "..kernel..";
  private static final String ADAPTERS_PACKAGE = "..adapters..";
  private static final String APP_PACKAGE = "..app..";
  private static final String APPLICATION_PACKAGE = "..application..";

  private static final int MAX_INHERITANCE_DEPTH = 2;

  private SocCompositionDemeterRules() {}

  // -------------------------------------------------------------------------
  // Separation of Concerns (SoC)
  // -------------------------------------------------------------------------

  /**
   * SoC: Domain/kernel classes must not contain HTTP, persistence, or messaging concerns.
   *
   * @return ArchRule
   */
  public static ArchRule socDomainShouldNotContainFrameworkConcerns() {
    return noClasses()
        .that()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework.web..",
            "org.springframework.data..",
            "org.springframework.kafka..",
            "org.springframework.amqp..",
            "jakarta.persistence..",
            "org.hibernate..",
            "com.fasterxml.jackson..")
        .as(
            "SoC: Domain/kernel must not contain HTTP, persistence, or messaging framework concerns");
  }

  /**
   * SoC: Persistence concerns (JPA, Hibernate) must only reside in adapters layer.
   *
   * @return ArchRule
   */
  public static ArchRule socPersistenceShouldOnlyBeInAdapters() {
    return noClasses()
        .that()
        .resideOutsideOfPackage(ADAPTERS_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("jakarta.persistence..", "org.hibernate..")
        .as("SoC: JPA/Hibernate annotations must only be used in the adapters layer");
  }

  /**
   * SoC: Messaging concerns (Kafka, RabbitMQ, SQS) must only reside in adapters layer.
   *
   * @return ArchRule
   */
  public static ArchRule socMessagingShouldOnlyBeInAdapters() {
    return noClasses()
        .that()
        .resideOutsideOfPackage(ADAPTERS_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework.kafka..",
            "org.springframework.amqp..",
            "software.amazon.awssdk.services.sqs..")
        .as("SoC: Messaging framework classes must only be used in the adapters layer");
  }

  /**
   * SoC: Web/HTTP concerns must only reside in adapters layer.
   *
   * @return ArchRule
   */
  public static ArchRule socWebConcernsShouldOnlyBeInAdapters() {
    return noClasses()
        .that()
        .resideOutsideOfPackage(ADAPTERS_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework.web.bind.annotation..",
            "org.springframework.http..",
            "jakarta.ws.rs..")
        .as("SoC: Web/HTTP annotations must only be used in the adapters layer");
  }

  // -------------------------------------------------------------------------
  // Composition over Inheritance
  // -------------------------------------------------------------------------

  /**
   * Composition over Inheritance: Adapter classes must not extend other concrete adapter classes.
   *
   * <p>Adapters should compose Port interfaces rather than inheriting from concrete
   * implementations.
   *
   * @return ArchRule
   */
  public static ArchRule compositionAdaptersShouldNotExtendConcreteClasses() {
    return noClasses()
        .that()
        .resideInAPackage(ADAPTERS_PACKAGE)
        .and()
        .haveSimpleNameEndingWith("Adapter")
        .should()
        .dependOnClassesThat(
            DescribedPredicate.<JavaClass>describe(
                "is a concrete adapter class",
                javaClass ->
                    javaClass.getPackageName().contains("adapters")
                        && !javaClass.isInterface()
                        && !javaClass.getModifiers().contains(JavaModifier.ABSTRACT)
                        && javaClass.getSimpleName().endsWith("Adapter")))
        .as(
            "Composition over Inheritance: Adapter classes should not extend other concrete Adapters");
  }

  /**
   * Composition over Inheritance: Domain classes must not have excessive inheritance depth.
   *
   * <p>Favor composition (field injection via constructor) over deep inheritance hierarchies.
   *
   * @return ArchRule
   */
  public static ArchRule compositionDomainShouldNotHaveDeepInheritance() {
    return classes()
        .that()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE)
        .and()
        .areNotInterfaces()
        .should(
            new ArchCondition<JavaClass>("have inheritance depth ≤ " + MAX_INHERITANCE_DEPTH) {
              @Override
              public void check(JavaClass item, ConditionEvents events) {
                int depth = 0;
                JavaClass current = item;
                java.util.Optional<? extends JavaType> optSuper = current.getSuperclass();
                while (optSuper.isPresent()) {
                  JavaType superType = optSuper.get();
                  if (superType.getName().equals("java.lang.Object")) break;
                  depth++;
                  current = superType.toErasure();
                  optSuper = current.getSuperclass();
                }
                if (depth > MAX_INHERITANCE_DEPTH) {
                  events.add(
                      SimpleConditionEvent.violated(
                          item,
                          item.getName()
                              + " has inheritance depth "
                              + depth
                              + " (max "
                              + MAX_INHERITANCE_DEPTH
                              + ")"));
                }
              }
            })
        .as(
            "Composition over Inheritance: Domain class inheritance depth must not exceed "
                + MAX_INHERITANCE_DEPTH);
  }

  // -------------------------------------------------------------------------
  // Law of Demeter
  // -------------------------------------------------------------------------

  /**
   * Law of Demeter: Domain/kernel classes must not call methods on objects returned from other
   * calls (no train wrecks: {@code a.getB().getC().doSomething()}).
   *
   * <p><strong>Note:</strong> This is a structural proxy. ArchUnit cannot fully verify call chains
   * in method bodies, but it can verify that classes do not depend on deeply nested types.
   *
   * @return ArchRule
   */
  public static ArchRule demeterDomainShouldNotDependOnDistantTypes() {
    return noClasses()
        .that()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(ADAPTERS_PACKAGE, "..infrastructure..")
        .as(
            "Law of Demeter: Domain classes must not depend on distant infrastructure types"
                + " (talk only to immediate collaborators)");
  }

  /**
   * Law of Demeter: Application use-cases must only depend on direct Port collaborators, not on
   * nested adapter internals.
   *
   * @return ArchRule
   */
  public static ArchRule demeterApplicationShouldOnlyTalkToDirectCollaborators() {
    return noClasses()
        .that()
        .resideInAnyPackage(APP_PACKAGE, APPLICATION_PACKAGE)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(ADAPTERS_PACKAGE, "..infrastructure..")
        .as(
            "Law of Demeter: Application layer must only depend on Port interfaces,"
                + " not on adapter internals");
  }

  // -------------------------------------------------------------------------
  // Tell, Don't Ask
  // -------------------------------------------------------------------------

  /**
   * Tell, Don't Ask: Domain entities must not expose raw field getters for all fields.
   *
   * <p>Classes named *Entity or *Aggregate should expose behavior, not raw state. This rule checks
   * that domain entity classes are not pure data bags (no class whose only public methods are
   * getters should be in the domain).
   *
   * <p><strong>Note:</strong> This is a structural proxy. A class where every public method is a
   * getter typically violates Tell, Don't Ask.
   *
   * @return ArchRule
   */
  public static ArchRule tellDontAskEntitiesShouldExposeBehaivour() {
    return classes()
        .that()
        .resideInAnyPackage(DOMAIN_PACKAGE, KERNEL_PACKAGE)
        .and()
        .areNotInterfaces()
        .and()
        .areNotEnums()
        .and()
        .areNotRecords()
        .should(
            new ArchCondition<JavaClass>("expose at least one public non-getter method") {
              @Override
              public void check(JavaClass item, ConditionEvents events) {
                long total =
                    item.getMethods().stream()
                        .filter(m -> m.getModifiers().contains(JavaModifier.PUBLIC))
                        .count();
                if (total == 0) return;
                long getters =
                    item.getMethods().stream()
                        .filter(m -> m.getModifiers().contains(JavaModifier.PUBLIC))
                        .filter(
                            m ->
                                m.getName().startsWith("get")
                                    || m.getName().startsWith("is")
                                    || m.getName().startsWith("has"))
                        .count();
                if (total >= 3 && getters == total) {
                  events.add(
                      SimpleConditionEvent.violated(
                          item,
                          item.getName()
                              + " exposes only getters ("
                              + total
                              + " public methods, all getters)"));
                }
              }
            })
        .allowEmptyShould(true)
        .as(
            "Tell, Don't Ask: Domain entity/aggregate classes should expose behavior,"
                + " not only getters");
  }
}

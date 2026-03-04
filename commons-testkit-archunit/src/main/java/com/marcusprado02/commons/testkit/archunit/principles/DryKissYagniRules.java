package com.marcusprado02.commons.testkit.archunit.principles;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * ArchUnit rules for DRY, KISS, and YAGNI principles.
 *
 * <p><strong>Important:</strong> These principles are <em>partially verifiable</em> via static
 * analysis. Rule-level checks verify structural proxies — e.g. class size, naming patterns, unused
 * abstractions. Full semantic verification (e.g. duplicated business logic) requires human review.
 *
 * <ul>
 *   <li>DRY — Don't Repeat Yourself (structural naming proxies)
 *   <li>KISS — Keep It Simple, Stupid (complexity proxies via method/class size)
 *   <li>YAGNI — You Aren't Gonna Need It (unused abstract layers proxy)
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.mycompany.myapp")
 * public class DesignPrinciplesTest {
 *
 *   @ArchTest
 *   public static final ArchRule kiss_no_deep_inheritance =
 *       DryKissYagniRules.kissNoDeepInheritance();
 *
 *   @ArchTest
 *   public static final ArchRule yagni_no_unused_ports =
 *       DryKissYagniRules.yagniPortsShouldHaveAtLeastOneAdapter();
 * }
 * }</pre>
 */
public final class DryKissYagniRules {

  private static final int MAX_INHERITANCE_DEPTH = 3;
  private static final int MAX_METHOD_COUNT = 20;
  private static final int MAX_CONSTRUCTOR_PARAMS = 7;

  private DryKissYagniRules() {}

  // -------------------------------------------------------------------------
  // DRY — Don't Repeat Yourself
  // -------------------------------------------------------------------------

  /**
   * DRY: Utility classes with only static methods should be declared as final.
   *
   * <p>Non-final utility classes invite subclassing that typically duplicates behavior.
   *
   * @return ArchRule
   */
  public static ArchRule dryUtilityClassesShouldBeFinal() {
    return classes()
        .that()
        .haveSimpleNameEndingWith("Utils")
        .or()
        .haveSimpleNameEndingWith("Util")
        .or()
        .haveSimpleNameEndingWith("Helper")
        .should()
        .haveModifier(JavaModifier.FINAL)
        .as("DRY: Utility/Helper classes should be final to prevent duplication via inheritance");
  }

  /**
   * DRY: Utility classes should have a private constructor to prevent instantiation.
   *
   * @return ArchRule
   */
  public static ArchRule dryUtilityClassesShouldHavePrivateConstructor() {
    return classes()
        .that()
        .haveSimpleNameEndingWith("Utils")
        .or()
        .haveSimpleNameEndingWith("Util")
        .or()
        .haveSimpleNameEndingWith("Helper")
        .should()
        .haveOnlyPrivateConstructors()
        .as("DRY: Utility classes should have only private constructors");
  }

  /**
   * DRY: Base/abstract classes in domain should be in a shared package.
   *
   * <p>Prevents copying base logic across multiple domain packages.
   *
   * @return ArchRule
   */
  public static ArchRule dryAbstractClassesShouldBeInSharedPackage() {
    return classes()
        .that()
        .haveModifier(JavaModifier.ABSTRACT)
        .and()
        .areNotInterfaces()
        .and()
        .resideInAnyPackage("..domain..", "..kernel..")
        .should()
        .resideInAPackage("..shared..")
        .orShould()
        .resideInAPackage("..common..")
        .orShould()
        .resideInAPackage("..base..")
        .orShould()
        .resideInAPackage("..core..")
        .as("DRY: Abstract domain base classes should be in a shared/common/base/core package");
  }

  // -------------------------------------------------------------------------
  // KISS — Keep It Simple, Stupid
  // -------------------------------------------------------------------------

  /**
   * KISS: Classes must not have more than {@value #MAX_METHOD_COUNT} methods.
   *
   * <p>Classes with too many methods are a structural signal of excessive complexity.
   *
   * @return ArchRule
   */
  public static ArchRule kissClassesShouldNotHaveTooManyMethods() {
    return classes()
        .that()
        .areTopLevelClasses()
        .and()
        .areNotInterfaces()
        .and()
        .areNotEnums()
        .should(
            new ArchCondition<JavaClass>("have at most " + MAX_METHOD_COUNT + " methods") {
              @Override
              public void check(JavaClass item, ConditionEvents events) {
                int count = item.getMethods().size();
                if (count > MAX_METHOD_COUNT) {
                  events.add(
                      SimpleConditionEvent.violated(
                          item,
                          item.getName()
                              + " has "
                              + count
                              + " methods (max "
                              + MAX_METHOD_COUNT
                              + ")"));
                }
              }
            })
        .as("KISS: Classes should not have more than " + MAX_METHOD_COUNT + " methods");
  }

  /**
   * KISS: Inheritance hierarchies must not exceed {@value #MAX_INHERITANCE_DEPTH} levels.
   *
   * <p>Deep hierarchies are hard to understand and maintain.
   *
   * @return ArchRule
   */
  public static ArchRule kissNoDeepInheritance() {
    return classes()
        .that()
        .areTopLevelClasses()
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
        .as("KISS: Inheritance depth must not exceed " + MAX_INHERITANCE_DEPTH + " levels");
  }

  /**
   * KISS: Constructors should not have more than {@value #MAX_CONSTRUCTOR_PARAMS} parameters.
   *
   * <p>Too many constructor parameters are a signal of too many responsibilities.
   *
   * @return ArchRule
   */
  public static ArchRule kissConstructorsShouldHaveLimitedParameters() {
    return noClasses()
        .that()
        .areTopLevelClasses()
        .and()
        .areNotInterfaces()
        .and()
        .areNotEnums()
        .should(
            new ArchCondition<JavaClass>(
                "not have a constructor with more than " + MAX_CONSTRUCTOR_PARAMS + " parameters") {
              @Override
              public void check(JavaClass item, ConditionEvents events) {
                item.getConstructors().stream()
                    .filter(c -> c.getParameters().size() > MAX_CONSTRUCTOR_PARAMS)
                    .forEach(
                        c ->
                            events.add(
                                SimpleConditionEvent.violated(
                                    item,
                                    item.getName()
                                        + " has a constructor with "
                                        + c.getParameters().size()
                                        + " parameters (max "
                                        + MAX_CONSTRUCTOR_PARAMS
                                        + ")")));
              }
            })
        .as(
            "KISS: Constructors should not have more than "
                + MAX_CONSTRUCTOR_PARAMS
                + " parameters");
  }

  // -------------------------------------------------------------------------
  // YAGNI — You Aren't Gonna Need It
  // -------------------------------------------------------------------------

  /**
   * YAGNI: Port interfaces should have at least one Adapter implementation.
   *
   * <p>A Port with no Adapter is a premature abstraction that is not needed yet.
   *
   * @return ArchRule
   */
  public static ArchRule yagniPortsShouldHaveAtLeastOneAdapter() {
    return classes()
        .that()
        .resideInAPackage("..ports..")
        .and()
        .areInterfaces()
        .should(
            new ArchCondition<JavaClass>(
                "have at least one class implementing it in ..adapters..") {
              @Override
              public void check(JavaClass item, ConditionEvents events) {
                boolean hasAdapter =
                    item.getAllSubclasses().stream()
                        .anyMatch(
                            sub -> sub.getPackageName().contains("adapters") && !sub.isInterface());
                if (!hasAdapter) {
                  events.add(
                      SimpleConditionEvent.violated(
                          item, item.getName() + " has no Adapter implementation in ..adapters.."));
                }
              }
            })
        .allowEmptyShould(true)
        .as("YAGNI: Every Port interface should have at least one Adapter implementation");
  }

  /**
   * YAGNI: Abstract classes in app/application layer should have at least one concrete subclass.
   *
   * @return ArchRule
   */
  public static ArchRule yagniAbstractClassesShouldBeUsed() {
    return classes()
        .that()
        .resideInAnyPackage("..app..", "..application..")
        .and()
        .haveModifier(JavaModifier.ABSTRACT)
        .and()
        .areNotInterfaces()
        .should(
            new ArchCondition<JavaClass>("have at least one concrete subclass") {
              @Override
              public void check(JavaClass item, ConditionEvents events) {
                boolean hasConcreteSubclass =
                    item.getAllSubclasses().stream()
                        .anyMatch(sub -> !sub.getModifiers().contains(JavaModifier.ABSTRACT));
                if (!hasConcreteSubclass) {
                  events.add(
                      SimpleConditionEvent.violated(
                          item, item.getName() + " has no concrete subclass"));
                }
              }
            })
        .allowEmptyShould(true)
        .as("YAGNI: Abstract classes in app layer must have at least one concrete subclass");
  }
}

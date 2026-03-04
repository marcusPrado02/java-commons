package com.marcusprado02.commons.testkit.archunit.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchRule;

/**
 * ArchUnit rules enforcing Command Query Responsibility Segregation (CQRS).
 *
 * <p>In CQRS:
 *
 * <ul>
 *   <li><strong>Commands</strong> — mutate state, must not return domain data.
 *   <li><strong>Queries</strong> — read state, must not mutate.
 *   <li><strong>Command/Query Handlers</strong> — reside in app/application layer.
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.mycompany.myapp")
 * public class CqrsArchTest {
 *
 *   @ArchTest
 *   public static final ArchRule commands_in_app_layer =
 *       CqrsRules.commandsShouldResideInAppLayer();
 *
 *   @ArchTest
 *   public static final ArchRule queries_in_app_layer =
 *       CqrsRules.queriesShouldResideInAppLayer();
 * }
 * }</pre>
 */
public final class CqrsRules {

  private static final String COMMAND = "Command";
  private static final String APP_PACKAGE = "..app..";
  private static final String APPLICATION_PACKAGE = "..application..";

  private CqrsRules() {}

  // -------------------------------------------------------------------------
  // Commands
  // -------------------------------------------------------------------------

  /**
   * CQRS: Command classes must reside in the app/application layer.
   *
   * @return ArchRule
   */
  public static ArchRule commandsShouldResideInAppLayer() {
    return classes()
        .that()
        .haveSimpleNameEndingWith(COMMAND)
        .and()
        .areNotInterfaces()
        .should()
        .resideInAnyPackage(APP_PACKAGE, APPLICATION_PACKAGE)
        .allowEmptyShould(true)
        .as("CQRS: Command classes must reside in the app/application layer");
  }

  /**
   * CQRS: Command classes should be immutable (final, or records).
   *
   * <p>Commands are messages — they should not be mutated after creation.
   *
   * @return ArchRule
   */
  public static ArchRule commandsShouldBeImmutable() {
    return classes()
        .that()
        .haveSimpleNameEndingWith(COMMAND)
        .and()
        .areNotInterfaces()
        .should()
        .haveModifier(JavaModifier.FINAL)
        .allowEmptyShould(true)
        .as("CQRS: Command classes should be final (immutable messages)");
  }

  /**
   * CQRS: Command Handler classes must reside in the app/application layer.
   *
   * @return ArchRule
   */
  public static ArchRule commandHandlersShouldResideInAppLayer() {
    return classes()
        .that()
        .haveSimpleNameEndingWith("CommandHandler")
        .should()
        .resideInAnyPackage(APP_PACKAGE, APPLICATION_PACKAGE)
        .allowEmptyShould(true)
        .as("CQRS: CommandHandler classes must reside in the app/application layer");
  }

  /**
   * CQRS: Command Handlers must not depend on adapter or infrastructure classes.
   *
   * @return ArchRule
   */
  public static ArchRule commandHandlersShouldNotDependOnAdapters() {
    return noClasses()
        .that()
        .haveSimpleNameEndingWith("CommandHandler")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..adapters..")
        .allowEmptyShould(true)
        .as("CQRS: Command Handlers must only depend on Port interfaces, not on Adapters");
  }

  // -------------------------------------------------------------------------
  // Queries
  // -------------------------------------------------------------------------

  /**
   * CQRS: Query classes must reside in the app/application layer.
   *
   * @return ArchRule
   */
  public static ArchRule queriesShouldResideInAppLayer() {
    return classes()
        .that()
        .haveSimpleNameEndingWith("Query")
        .and()
        .areNotInterfaces()
        .should()
        .resideInAnyPackage(APP_PACKAGE, APPLICATION_PACKAGE)
        .allowEmptyShould(true)
        .as("CQRS: Query classes must reside in the app/application layer");
  }

  /**
   * CQRS: Query Handler classes must reside in the app/application layer.
   *
   * @return ArchRule
   */
  public static ArchRule queryHandlersShouldResideInAppLayer() {
    return classes()
        .that()
        .haveSimpleNameEndingWith("QueryHandler")
        .should()
        .resideInAnyPackage(APP_PACKAGE, APPLICATION_PACKAGE)
        .allowEmptyShould(true)
        .as("CQRS: QueryHandler classes must reside in the app/application layer");
  }

  /**
   * CQRS: Query Handlers must not depend on adapter or infrastructure classes.
   *
   * @return ArchRule
   */
  public static ArchRule queryHandlersShouldNotDependOnAdapters() {
    return noClasses()
        .that()
        .haveSimpleNameEndingWith("QueryHandler")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..adapters..")
        .allowEmptyShould(true)
        .as("CQRS: Query Handlers must only depend on Port interfaces, not on Adapters");
  }

  /**
   * CQRS: Commands and Queries must be in separate packages.
   *
   * @return ArchRule
   */
  public static ArchRule commandsAndQueriesShouldBeInSeparatePackages() {
    return noClasses()
        .that()
        .haveSimpleNameEndingWith(COMMAND)
        .should()
        .resideInAPackage("..query..")
        .allowEmptyShould(true)
        .as("CQRS: Command and Query classes should reside in separate packages");
  }
}

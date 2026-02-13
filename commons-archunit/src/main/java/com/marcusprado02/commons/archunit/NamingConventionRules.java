package com.marcusprado02.commons.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.lang.ArchRule;

/**
 * ArchUnit rules for naming conventions across the commons platform.
 *
 * <p>Consistent naming improves code readability and helps identify components' roles.
 */
public final class NamingConventionRules {

  private NamingConventionRules() {}

  /**
   * Port interfaces should end with 'Port'.
   */
  public static final ArchRule PORTS_SHOULD_END_WITH_PORT =
      classes()
          .that()
          .resideInAPackage("..ports..")
          .and()
          .areInterfaces()
          .and()
          .areNotAnnotations()
          .should()
          .haveSimpleNameEndingWith("Port")
          .because("Port interfaces should be easily identifiable by 'Port' suffix");

  /**
   * Adapter implementations should end with 'Adapter'.
   */
  public static final ArchRule ADAPTERS_SHOULD_END_WITH_ADAPTER =
      classes()
          .that()
          .resideInAPackage("..adapters..")
          .and()
          .areNotInterfaces()
          .and()
          .areNotEnums()
          .and()
          .areNotAnnotations()
          .and()
          .haveSimpleNameNotEndingWith("Config")
          .and()
          .haveSimpleNameNotEndingWith("Exception")
          .and()
          .haveSimpleNameNotEndingWith("Properties")
          .should()
          .haveSimpleNameEndingWith("Adapter")
          .because("Adapter implementations should be easily identifiable by 'Adapter' suffix");

  /**
   * Use Cases should end with 'UseCase'.
   */
  public static final ArchRule USE_CASES_SHOULD_END_WITH_USE_CASE =
      classes()
          .that()
          .resideInAPackage("..app.usecase..")
          .or()
          .resideInAPackage("..application.usecase..")
          .and()
          .areNotInterfaces()
          .should()
          .haveSimpleNameEndingWith("UseCase")
          .because("Use Cases should be easily identifiable by 'UseCase' suffix");

  /**
   * Configuration classes should end with 'Config' or 'Configuration'.
   */
  public static final ArchRule CONFIG_CLASSES_SHOULD_FOLLOW_NAMING =
      classes()
          .that()
          .areAnnotatedWith("org.springframework.context.annotation.Configuration")
          .should()
          .haveSimpleNameEndingWith("Config")
          .orShould()
          .haveSimpleNameEndingWith("Configuration")
          .because("Configuration classes should follow naming conventions");

  /**
   * Exception classes should end with 'Exception'.
   */
  public static final ArchRule EXCEPTIONS_SHOULD_END_WITH_EXCEPTION =
      classes()
          .that()
          .areAssignableTo(Exception.class)
          .should()
          .haveSimpleNameEndingWith("Exception")
          .because("Exception classes should be easily identifiable by 'Exception' suffix");

  /**
   * DTOs should end with 'DTO' or 'Dto'.
   */
  public static final ArchRule DTOS_SHOULD_FOLLOW_NAMING =
      classes()
          .that()
          .resideInAPackage("..dto..")
          .and()
          .areNotInterfaces()
          .should()
          .haveSimpleNameEndingWith("DTO")
          .orShould()
          .haveSimpleNameEndingWith("Dto")
          .because("DTOs should be easily identifiable by 'DTO'/'Dto' suffix");

  /**
   * REST Controllers should end with 'Controller' or 'Resource'.
   */
  public static final ArchRule REST_CONTROLLERS_SHOULD_FOLLOW_NAMING =
      classes()
          .that()
          .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
          .should()
          .haveSimpleNameEndingWith("Controller")
          .orShould()
          .haveSimpleNameEndingWith("Resource")
          .because("REST Controllers should follow naming conventions");

  /**
   * Package names should be lowercase.
   */
  public static final ArchRule PACKAGES_SHOULD_BE_LOWERCASE =
      classes()
          .should()
          .resideInAPackage("..")
          .because("Package names should be lowercase by Java conventions");
}

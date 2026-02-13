package com.marcusprado02.commons.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * ArchUnit rules for naming conventions across the commons platform.
 *
 * <p>Consistent naming improves code readability and helps identify components' roles.
 */
public final class NamingConventionRules {

  private NamingConventionRules() {}

  /** Any type ending with 'Port' must be an interface. */
  public static final ArchRule TYPES_ENDING_WITH_PORT_SHOULD_BE_INTERFACES =
      classes()
          .that()
          .haveSimpleNameEndingWith("Port")
          .should()
          .beInterfaces()
          .because("Port contracts must be expressed as interfaces");

  /** Any type ending with 'Port' should reside under a ports package. */
  public static final ArchRule TYPES_ENDING_WITH_PORT_SHOULD_RESIDE_IN_PORTS_PACKAGE =
      classes()
          .that()
          .haveSimpleNameEndingWith("Port")
          .should()
          .resideInAPackage("..ports..")
          .because("Port contracts should live in ports modules/packages");

  /** Any type ending with 'Adapter' should reside under an adapters package. */
  public static final ArchRule TYPES_ENDING_WITH_ADAPTER_SHOULD_RESIDE_IN_ADAPTERS_PACKAGE =
      classes()
          .that()
          .haveSimpleNameEndingWith("Adapter")
          .should()
          .resideInAPackage("..adapters..")
          .because("Adapter implementations should live in adapters modules/packages");

  /** Use Cases should end with 'UseCase'. */
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

  /** Configuration classes should end with 'Config' or 'Configuration'. */
  public static final ArchRule CONFIG_CLASSES_SHOULD_FOLLOW_NAMING =
      classes()
          .that()
          .areAnnotatedWith("org.springframework.context.annotation.Configuration")
          .should()
          .haveSimpleNameEndingWith("Config")
          .orShould()
          .haveSimpleNameEndingWith("Configuration")
          .because("Configuration classes should follow naming conventions");

  /** Exception classes should end with 'Exception'. */
  public static final ArchRule EXCEPTIONS_SHOULD_END_WITH_EXCEPTION =
      classes()
          .that()
          .areAssignableTo(Exception.class)
          .should()
          .haveSimpleNameEndingWith("Exception")
          .because("Exception classes should be easily identifiable by 'Exception' suffix");

  /** DTOs should end with 'DTO' or 'Dto'. */
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

  /** REST Controllers should end with 'Controller' or 'Resource'. */
  public static final ArchRule REST_CONTROLLERS_SHOULD_FOLLOW_NAMING =
      classes()
          .that()
          .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
          .should()
          .haveSimpleNameEndingWith("Controller")
          .orShould()
          .haveSimpleNameEndingWith("Resource")
          .because("REST Controllers should follow naming conventions");

  /** Package names should be lowercase. */
  public static final ArchRule PACKAGES_SHOULD_BE_LOWERCASE =
      classes()
          .that()
          .resideInAPackage("com.marcusprado02.commons..")
          .should(haveLowercasePackageSegments())
          .because("Package names should be lowercase by Java conventions");

  private static ArchCondition<JavaClass> haveLowercasePackageSegments() {
    return new ArchCondition<>("have lowercase package segments") {
      @Override
      public void check(JavaClass item, ConditionEvents events) {
        String pkg = item.getPackageName();
        if (pkg == null || pkg.isBlank()) {
          return;
        }

        boolean ok =
            java.util.Arrays.stream(pkg.split("\\."))
                .allMatch(seg -> seg.equals(seg.toLowerCase(java.util.Locale.ROOT)));

        if (!ok) {
          events.add(
              SimpleConditionEvent.violated(
                  item,
                  String.format(
                      "Class '%s' is in a non-lowercase package '%s'", item.getName(), pkg)));
        }
      }
    };
  }
}

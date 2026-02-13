package com.marcusprado02.commons.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.lang.ArchRule;

/**
 * ArchUnit rules for test organization and structure.
 *
 * <p>These rules ensure tests are properly organized and follow best practices.
 */
public final class TestOrganizationRules {

  private TestOrganizationRules() {}

  /**
   * Test classes should reside in the same package as the tested code.
   *
   * <p>This improves test discoverability and allows testing package-private methods.
   */
  public static final ArchRule TESTS_SHOULD_BE_IN_SAME_PACKAGE_AS_CODE =
      classes()
          .that()
          .haveSimpleNameEndingWith("Test")
          .should()
          .resideInAPackage("com.marcusprado02.commons..")
          .because("Tests should be in the same package as the tested code for better organization");

  /**
   * Test classes should end with 'Test'.
   */
  public static final ArchRule TEST_CLASSES_SHOULD_END_WITH_TEST =
      classes()
          .that()
          .areAnnotatedWith("org.junit.jupiter.api.Test")
          .or()
          .haveSimpleNameContaining("Test")
          .should()
          .haveSimpleNameEndingWith("Test")
          .because("Test classes should be easily identifiable by 'Test' suffix");

  /**
   * Integration tests should be annotated with @SpringBootTest.
   */
  public static final ArchRule INTEGRATION_TESTS_SHOULD_USE_SPRING_BOOT_TEST =
      classes()
          .that()
          .haveSimpleNameEndingWith("IntegrationTest")
          .or()
          .haveSimpleNameEndingWith("IT")
          .should()
          .beAnnotatedWith("org.springframework.boot.test.context.SpringBootTest")
          .because("Integration tests should use @SpringBootTest for full context loading");

  /**
   * ArchUnit tests should end with 'ArchTest'.
   */
  public static final ArchRule ARCH_TESTS_SHOULD_END_WITH_ARCH_TEST =
      classes()
          .that()
          .areAnnotatedWith("com.tngtech.archunit.junit5.AnalyzeClasses")
          .should()
          .haveSimpleNameEndingWith("ArchTest")
          .because("ArchUnit tests should be easily identifiable by 'ArchTest' suffix");

  /**
   * Test classes should not be public (JUnit 5 doesn't require it).
   */
  public static final ArchRule TEST_CLASSES_SHOULD_NOT_BE_PUBLIC =
      classes()
          .that()
          .haveSimpleNameEndingWith("Test")
          .and()
          .areNotAnnotations()
          .should()
          .notBePublic()
          .because("JUnit 5 test classes don't need to be public (package-private is preferred)");

  /**
   * Test methods should not be public (JUnit 5 doesn't require it).
   */
  public static final ArchRule TEST_METHODS_SHOULD_NOT_BE_PUBLIC =
      classes()
          .that()
          .haveSimpleNameEndingWith("Test")
          .should()
          .notHaveModifier(com.tngtech.archunit.core.domain.JavaModifier.PUBLIC)
          .because("JUnit 5 test methods don't need to be public (package-private is preferred)");

  /**
   * Test utility classes should be in a 'support' or 'util' package.
   */
  public static final ArchRule TEST_UTILITIES_SHOULD_BE_IN_SUPPORT_PACKAGE =
      classes()
          .that()
          .resideInAPackage("..test..")
          .and()
          .haveSimpleNameNotEndingWith("Test")
          .and()
          .areNotEnums()
          .should()
          .resideInAPackage("..support..")
          .orShould()
          .resideInAPackage("..util..")
          .orShould()
          .resideInAPackage("..fixture..")
          .because("Test utilities should be organized in support/util/fixture packages");
}

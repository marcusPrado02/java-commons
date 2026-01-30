package com.marcusprado02.commons.archunit.ddd;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.marcusprado02.commons.archunit.support.ArchTestSupport;
import com.marcusprado02.commons.kernel.ddd.id.Identifier;
import com.tngtech.archunit.junit.ArchTest;

class IdentifierRulesArchTest {

  @ArchTest
  static void ids_should_implement_identifier_interface() {
    classes()
        .that()
        .haveSimpleNameEndingWith("Id")
        .and()
        .resideInAPackage("..kernel.ddd..")
        .should()
        .implement(Identifier.class)
        .check(ArchTestSupport.importCommons());
  }
}

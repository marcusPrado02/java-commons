package com.marcusprado02.commons.archunit.ddd;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.marcusprado02.commons.archunit.support.ArchTestSupport;
import com.marcusprado02.commons.kernel.ddd.id.Identifier;
import org.junit.jupiter.api.Test;

class IdentifierRulesArchTest {

  @Test
  void ids_should_implement_identifier_interface() {
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

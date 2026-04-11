package com.marcusprado02.commons.archunit.ddd;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.marcusprado02.commons.archunit.support.ArchTestSupport;
import com.marcusprado02.commons.kernel.ddd.entity.Entity;
import org.junit.jupiter.api.Test;

class EntityRulesArchTest {

  @Test
  void entities_should_extend_base_entity() {
    classes()
        .that()
        .resideInAPackage("..kernel.ddd..")
        .and()
        .haveSimpleNameEndingWith("Entity")
        .should()
        .beAssignableTo(Entity.class)
        .check(ArchTestSupport.importCommons());
  }
}

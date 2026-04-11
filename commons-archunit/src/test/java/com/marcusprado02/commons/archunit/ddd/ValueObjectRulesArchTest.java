package com.marcusprado02.commons.archunit.ddd;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.marcusprado02.commons.archunit.support.ArchTestSupport;
import com.marcusprado02.commons.kernel.ddd.vo.ValueObject;
import com.tngtech.archunit.core.domain.JavaModifier;
import org.junit.jupiter.api.Test;

class ValueObjectRulesArchTest {

  @Test
  void value_objects_should_be_final() {
    classes()
        .that()
        .implement(ValueObject.class)
        .and()
        .doNotHaveModifier(JavaModifier.ABSTRACT)
        .should()
        .haveModifier(JavaModifier.FINAL)
        .allowEmptyShould(true)
        .check(ArchTestSupport.importCommons());
  }
}

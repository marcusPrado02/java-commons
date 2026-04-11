package com.marcusprado02.commons.archunit.ddd;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.marcusprado02.commons.archunit.support.ArchTestSupport;
import com.marcusprado02.commons.kernel.ddd.entity.AggregateRoot;
import org.junit.jupiter.api.Test;

class AggregateRulesArchTest {

  @Test
  void aggregates_should_extend_aggregate_root() {
    classes()
        .that()
        .resideInAPackage("..kernel.ddd..")
        .and()
        .haveSimpleNameEndingWith("Aggregate")
        .should()
        .beAssignableTo(AggregateRoot.class)
        .allowEmptyShould(true)
        .check(ArchTestSupport.importCommons());
  }
}

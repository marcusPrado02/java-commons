package com.marcusprado02.commons.archunit.ddd;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.marcusprado02.commons.archunit.support.ArchTestSupport;
import com.marcusprado02.commons.kernel.ddd.entity.AggregateRoot;
import com.tngtech.archunit.junit.ArchTest;

class AggregateRulesArchTest {

  @ArchTest
  static void aggregates_should_extend_aggregate_root() {
    classes()
        .that()
        .resideInAPackage("..kernel.ddd..")
        .and()
        .haveSimpleNameEndingWith("Aggregate")
        .should()
        .beAssignableTo(AggregateRoot.class)
        .check(ArchTestSupport.importCommons());
  }
}

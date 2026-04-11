package com.marcusprado02.commons.archunit.ddd;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.marcusprado02.commons.archunit.support.ArchTestSupport;
import com.marcusprado02.commons.kernel.ddd.event.DomainEvent;
import com.tngtech.archunit.core.domain.JavaModifier;
import org.junit.jupiter.api.Test;

class DomainEventRulesArchTest {

  @Test
  void domain_events_should_be_final() {
    classes()
        .that()
        .implement(DomainEvent.class)
        .should()
        .haveModifier(JavaModifier.FINAL)
        .allowEmptyShould(true)
        .check(ArchTestSupport.importCommons());
  }
}

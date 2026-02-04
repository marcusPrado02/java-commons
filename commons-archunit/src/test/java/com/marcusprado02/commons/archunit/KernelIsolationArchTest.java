package com.marcusprado02.commons.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class KernelIsolationArchTest {

  private static final JavaClasses CLASSES = new ClassFileImporter().importClasspath();

  @Test
  void kernel_must_not_depend_on_framework_packages() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAnyPackage("com.marcusprado02.commons.kernel..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework..",
                "jakarta.persistence..",
                "javax.persistence..",
                "com.fasterxml.jackson..",
                "org.slf4j..",
                "org.hibernate..")
            .allowEmptyShould(true);

    rule.check(CLASSES);
  }
}

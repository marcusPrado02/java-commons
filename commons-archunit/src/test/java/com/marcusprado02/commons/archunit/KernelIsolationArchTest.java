package com.marcusprado02.commons.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class KernelIsolationArchTest {

  private static final String KERNEL = "com.marcusprado02.commons.kernel..";

  @Test
  void kernel_must_not_depend_on_framework_packages() {
    JavaClasses classes = new ClassFileImporter().importPackages("com.marcusprado02.commons..");

    noClasses()
        .that()
        .resideInAPackage(KERNEL)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "jakarta.persistence..",
            "javax.persistence..",
            "org.hibernate..",
            "org.slf4j..",
            "com.fasterxml.jackson..")
        .check(classes);
  }
}

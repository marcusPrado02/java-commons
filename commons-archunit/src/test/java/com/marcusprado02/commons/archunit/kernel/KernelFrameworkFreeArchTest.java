package com.marcusprado02.commons.archunit.kernel;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.marcusprado02.commons.archunit.support.ArchTestSupport;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

@AnalyzeClasses(packages = "com.marcusprado02.commons")
class KernelFrameworkFreeArchTest {

  @ArchTest
  static void kernel_must_not_depend_on_frameworks() {
    noClasses()
        .that()
        .resideInAnyPackage("..kernel..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "jakarta.persistence..",
            "jakarta.validation..",
            "javax.persistence..",
            "org.hibernate..",
            "io.quarkus..",
            "io.micronaut..")
        .check(ArchTestSupport.importCommons());
  }
}

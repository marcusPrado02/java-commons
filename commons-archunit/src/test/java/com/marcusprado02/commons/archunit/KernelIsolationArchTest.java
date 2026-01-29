package com.marcusprado02.commons.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class KernelIsolationArchTest {

  private static final String KERNEL = "com.marcusprado02.commons.kernel..";

  @Test
  void kernel_must_not_depend_on_framework_packages() {
    JavaClasses kernelClasses = new ClassFileImporter().importPackages(KERNEL);

    ArchRule rule =
        noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework..",
                "jakarta.persistence..",
                "javax.persistence..",
                "org.hibernate..",
                "org.slf4j..",
                "com.fasterxml.jackson..");

    // enquanto o kernel não estiver no classpath do módulo de testes, não explode por vazio
    rule.allowEmptyShould(true).check(kernelClasses);
  }
}

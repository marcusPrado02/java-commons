package com.marcusprado02.commons.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

public class KernelIsolationArchTest {

  private final JavaClasses kernelClasses =
      new ClassFileImporter()
          .withImportOption(new ImportOption.DoNotIncludeTests())
          .withImportOption(new ImportOption.DoNotIncludeArchives())
          .withImportOption(new ImportOption.DoNotIncludeJars())
          .importPackages(
              "com.marcusprado02.commons.kernel.core",
              "com.marcusprado02.commons.kernel.ddd",
              "com.marcusprado02.commons.kernel.errors",
              "com.marcusprado02.commons.kernel.result",
              "com.marcusprado02.commons.kernel.time"
            );

  @Test
  void kernel_must_not_depend_on_framework_packages() {
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
        .check(kernelClasses);
  }
}

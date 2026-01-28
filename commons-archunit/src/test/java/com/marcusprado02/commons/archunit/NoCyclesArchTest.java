package com.marcusprado02.commons.archunit;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class NoCyclesArchTest {

  @Test
  void no_cycles_in_base_package() {
    var classes = new ClassFileImporter().importPackages("com.marcusprado02.commons..");

    slices().matching("com.marcusprado02.commons.(*)..").should().beFreeOfCycles().check(classes);
  }
}

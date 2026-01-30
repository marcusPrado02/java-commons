package com.marcusprado02.commons.archunit.support;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

public final class ArchTestSupport {

  private ArchTestSupport() {}

  public static JavaClasses importCommons() {
    return new ClassFileImporter().importPackages("com.marcusprado02.commons");
  }
}

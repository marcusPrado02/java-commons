# Commons ArchUnit Rules

## Overview

The `commons-archunit` module provides architecture validation rules for the `java-commons` platform using [ArchUnit](https://www.archunit.org/).

It contains reusable `ArchRule` definitions (hexagonal dependency direction, kernel isolation, no-cycles, DDD purity, naming conventions, and test organization) and a small aggregator (`CommonsArchitectureRules`) to make them easy to consume.

## Rule Categories

### 1) Kernel isolation

Implemented in `com.marcusprado02.commons.archunit.rules.KernelIsolationRules`.

- `KERNEL_SHOULD_ONLY_DEPEND_ON_JDK`
- `KERNEL_SHOULD_NOT_DEPEND_ON_OUTER_LAYERS`
- `KERNEL_SHOULD_NOT_USE_SPRING`
- `KERNEL_SHOULD_NOT_USE_JACKSON`
- `KERNEL_CLASSES_SHOULD_BE_FINAL_OR_ABSTRACT`

### 2) Hexagonal architecture

Implemented in `com.marcusprado02.commons.archunit.HexagonalRules`.

- `APPLICATION_SHOULD_ONLY_DEPEND_ON_KERNEL_AND_PORTS`
- `ADAPTERS_SHOULD_ONLY_DEPEND_ON_PORTS_AND_KERNEL` (currently enforces that adapters do not depend on starters)
- `STARTERS_SHOULD_ONLY_DEPEND_ON_ADAPTERS_AND_APP` (currently enforces that starters avoid direct dependencies on `..ports..`)
- `PORTS_SHOULD_ONLY_CONTAIN_INTERFACES` (types ending with `Port` must be interfaces)
- `ADAPTERS_SHOULD_IMPLEMENT_PORTS` (types ending with `Adapter` must implement at least one `*Port` interface)

### 3) No cycles

Implemented in `com.marcusprado02.commons.archunit.NoCyclesRules`.

- `NO_CYCLES_BETWEEN_LAYERS`
- `NO_CYCLES_WITHIN_KERNEL`
- `NO_CYCLES_WITHIN_APPLICATION`
- `NO_CYCLES_WITHIN_ADAPTERS`
- `NO_CYCLES_WITHIN_PORTS`

### 4) Domain purity (DDD)

Implemented in `com.marcusprado02.commons.archunit.DomainPurityRules`.

### 5) Naming conventions

Implemented in `com.marcusprado02.commons.archunit.NamingConventionRules`.

- `TYPES_ENDING_WITH_PORT_SHOULD_BE_INTERFACES`
- `TYPES_ENDING_WITH_PORT_SHOULD_RESIDE_IN_PORTS_PACKAGE`
- `TYPES_ENDING_WITH_ADAPTER_SHOULD_RESIDE_IN_ADAPTERS_PACKAGE`
- `USE_CASES_SHOULD_END_WITH_USE_CASE`
- `CONFIG_CLASSES_SHOULD_FOLLOW_NAMING`
- `EXCEPTIONS_SHOULD_END_WITH_EXCEPTION`
- `DTOS_SHOULD_FOLLOW_NAMING`
- `REST_CONTROLLERS_SHOULD_FOLLOW_NAMING`
- `PACKAGES_SHOULD_BE_LOWERCASE`

### 6) Test organization

Implemented in `com.marcusprado02.commons.archunit.TestOrganizationRules`.

## Usage

### 1) Add dependency

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-archunit</artifactId>
  <scope>test</scope>
</dependency>
```

### 2) Use rules in JUnit 5 tests

This module exposes `ArchRule[]` aggregations via `CommonsArchitectureRules`.

```java
package com.yourcompany.yourproject;

import static com.marcusprado02.commons.archunit.CommonsArchitectureRules.*;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

  @Test
  void kernel_rules() {
    var classes = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("com.yourcompany.yourproject");

    for (var rule : kernelIsolation()) {
      rule.check(classes);
    }
  }
}
```

## Platform module validation (this repository)

For the `java-commons` repo itself, module-by-module validation lives in:

- `commons-archunit/src/test/java/com/marcusprado02/commons/archunit/PlatformModuleValidationTest.java`

It imports each module base package and checks the key invariants (kernel isolation, port contract rules, adapter implements port, and no-cycles).

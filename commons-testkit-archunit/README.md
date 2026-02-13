# Commons Testkit ArchUnit

Reusable architectural tests using ArchUnit for DDD/Hexagonal architecture validation.

## Overview

This module provides ready-to-use ArchUnit rules to enforce clean architecture principles in your Java projects. The rules validate:

- **Layer dependencies** (kernel → app → ports → adapters)
- **Naming conventions** (repositories, ports, adapters, use cases)
- **Framework independence** (kernel/ports should not depend on Spring, JPA, etc.)
- **Package structure** (proper organization of domain, application, and infrastructure)

## Installation

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-testkit-archunit</artifactId>
  <scope>test</scope>
</dependency>
```

## Usage

### DDD Layer Rules

Validate clean architecture layer dependencies:

```java
import com.marcusprado02.commons.testkit.archunit.DddLayerRules;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "com.mycompany.myapp",
    importOptions = ImportOption.DoNotIncludeTests.class)
public class LayeredArchitectureTest {

  @ArchTest
  public static final ArchRule kernel_should_not_depend_on_other_layers =
      DddLayerRules.kernelShouldNotDependOnOtherLayers();

  @ArchTest
  public static final ArchRule app_should_not_depend_on_adapters =
      DddLayerRules.appShouldNotDependOnAdapters();

  @ArchTest
  public static final ArchRule ports_should_not_depend_on_adapters_or_app =
      DddLayerRules.portsShouldNotDependOnAdaptersOrApp();

  @ArchTest
  public static final ArchRule adapters_should_not_depend_on_other_adapters =
      DddLayerRules.adaptersShouldNotDependOnOtherAdapters();
}
```

**Available Layer Rules:**
- `kernelShouldNotDependOnOtherLayers()` - Domain is independent
- `appShouldNotDependOnAdapters()` - Application layer uses ports
- `portsShouldNotDependOnAdaptersOrApp()` - Ports are pure interfaces
- `adaptersShouldNotDependOnOtherAdapters()` - Adapters are independent
- `appShouldOnlyBeAccessedByAdaptersOrApp()` - Enforce access control

### Naming Convention Rules

Enforce consistent naming across your codebase:

```java
import com.marcusprado02.commons.testkit.archunit.NamingConventionRules;

@AnalyzeClasses(packages = "com.mycompany.myapp")
public class NamingConventionTest {

  @ArchTest
  public static final ArchRule repositories_should_be_named_properly =
      NamingConventionRules.repositoriesShouldHaveProperNames();

  @ArchTest
  public static final ArchRule ports_should_be_named_properly =
      NamingConventionRules.portsShouldHaveProperNames();

  @ArchTest
  public static final ArchRule adapters_should_be_named_properly =
      NamingConventionRules.adaptersShouldHaveProperNames();

  @ArchTest
  public static final ArchRule use_cases_should_be_named_properly =
      NamingConventionRules.useCasesShouldHaveProperNames();

  @ArchTest
  public static final ArchRule domain_events_should_be_records =
      NamingConventionRules.domainEventsShouldBeRecords();

  @ArchTest
  public static final ArchRule value_objects_should_be_records =
      NamingConventionRules.valueObjectsShouldBeRecords();
}
```

**Available Naming Rules:**
- `repositoriesShouldHaveProperNames()` - `*Repository`
- `portsShouldHaveProperNames()` - `*Port`, `*Repository`, `*Specification`
- `adaptersShouldHaveProperNames()` - `*Adapter`, `*Configuration`, `*Controller`
- `useCasesShouldHaveProperNames()` - `*UseCase`, `*Service`, `*Handler`
- `aggregatesShouldBeInKernel()` - Aggregates in domain
- `domainEventsShouldBeRecords()` - `*Event` as records
- `valueObjectsShouldBeRecords()` - Value objects are immutable records

### Dependency Rules

Validate framework independence and clean dependencies:

```java
import com.marcusprado02.commons.testkit.archunit.DependencyRules;

@AnalyzeClasses(packages = "com.mycompany.myapp")
public class DependencyRulesTest {

  @ArchTest
  public static final ArchRule no_cycles_in_packages =
      DependencyRules.noPackageCycles();

  @ArchTest
  public static final ArchRule kernel_should_not_depend_on_frameworks =
      DependencyRules.kernelShouldNotDependOnFrameworks();

  @ArchTest
  public static final ArchRule ports_should_not_depend_on_frameworks =
      DependencyRules.portsShouldNotDependOnFrameworks();

  @ArchTest
  public static final ArchRule only_adapters_should_depend_on_spring =
      DependencyRules.onlyAdaptersShouldDependOnSpring();

  @ArchTest
  public static final ArchRule only_adapters_should_depend_on_jpa =
      DependencyRules.onlyAdaptersShouldDependOnJpa();

  @ArchTest
  public static final ArchRule domain_should_not_use_jpa_annotations =
      DependencyRules.domainShouldNotUseJpaAnnotations();
}
```

**Available Dependency Rules:**
- `noPackageCycles()` - No circular package dependencies
- `kernelShouldNotDependOnFrameworks()` - Domain is framework-free
- `portsShouldNotDependOnFrameworks()` - Ports are framework-free
- `onlyAdaptersShouldDependOnSpring()` - Spring only in adapters
- `onlyAdaptersShouldDependOnJpa()` - JPA only in adapters
- `domainShouldNotUseJpaAnnotations()` - No `@Entity` in domain

### Package Structure Rules

Enforce proper package organization:

```java
import com.marcusprado02.commons.testkit.archunit.PackageStructureRules;

@AnalyzeClasses(packages = "com.mycompany.myapp")
public class PackageStructureTest {

  @ArchTest
  public static final ArchRule packages_should_be_organized =
      PackageStructureRules.kernelAppPortsAdaptersPackagesShouldExist();

  @ArchTest
  public static final ArchRule interfaces_should_be_in_ports =
      PackageStructureRules.interfacesShouldBeInPorts();

  @ArchTest
  public static final ArchRule domain_models_should_be_in_kernel =
      PackageStructureRules.domainModelsShouldBeInKernel();

  @ArchTest
  public static final ArchRule use_cases_should_be_in_app =
      PackageStructureRules.useCasesShouldBeInApp();

  @ArchTest
  public static final ArchRule dtos_should_not_be_in_kernel =
      PackageStructureRules.dtosShouldNotBeInKernel();
}
```

**Available Package Rules:**
- `kernelAppPortsAdaptersPackagesShouldExist()` - Standard structure
- `interfacesShouldBeInPorts()` - Port interfaces in ports package
- `domainModelsShouldBeInKernel()` - Entities/aggregates in kernel
- `useCasesShouldBeInApp()` - Use cases in app package
- `configurationsShouldBeInAdaptersOrConfig()` - Config location
- `dtosShouldNotBeInKernel()` - DTOs outside domain

## Programmatic Validation

For custom test scenarios, validate all rules programmatically:

```java
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

public class ArchitectureValidation {

  public static void main(String[] args) {
    JavaClasses classes = new ClassFileImporter()
        .importPackages("com.mycompany.myapp");

    // Check all layer rules
    DddLayerRules.checkAllLayerRules(classes);

    // Check all naming conventions
    NamingConventionRules.checkAllNamingRules(classes);

    // Check all dependency rules
    DependencyRules.checkAllDependencyRules(classes);

    // Check all package structure rules
    PackageStructureRules.checkAllPackageRules(classes);
  }
}
```

## Example Project Structure

These rules expect a DDD/Hexagonal architecture structure like:

```
com.mycompany.myapp
├── kernel (domain)
│   ├── model
│   │   ├── Order.java (AggregateRoot)
│   │   ├── OrderId.java (record)
│   │   └── OrderCreatedEvent.java (record, DomainEvent)
│   └── exception
├── app (use cases)
│   ├── CreateOrderUseCase.java
│   └── OrderService.java
├── ports
│   ├── persistence
│   │   └── OrderRepository.java
│   ├── messaging
│   │   └── EventPublisherPort.java
│   └── http
│       └── PaymentClientPort.java
└── adapters
    ├── persistence
    │   └── JpaOrderRepositoryAdapter.java
    ├── messaging
    │   └── KafkaEventPublisherAdapter.java
    ├── http
    │   └── OkHttpPaymentClientAdapter.java
    └── web
        ├── OrderController.java
        └── OrderRequest.java (DTO)
```

## Benefits

1. **Automated Architecture Enforcement** - Rules run in CI/CD
2. **Documentation** - Rules serve as living architecture documentation
3. **Refactoring Safety** - Catch violations early
4. **Consistency** - Enforce patterns across teams
5. **Onboarding** - New developers learn architecture through tests

## Best Practices

1. **Run in CI/CD** - Fail builds on violations
2. **Start Small** - Add rules incrementally
3. **Team Agreement** - Discuss and agree on rules
4. **Document Exceptions** - Use `@ArchIgnore` sparingly with comments
5. **Combine Rules** - Use all 4 rule sets for complete coverage

## Dependencies

- ArchUnit JUnit 5
- JUnit 5

## License

See [LICENSE](../LICENSE)

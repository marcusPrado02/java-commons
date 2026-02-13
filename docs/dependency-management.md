# Dependency Management Strategy

## Overview

The `commons-bom` module provides centralized dependency version management for the entire java-commons platform and consuming applications. This ensures consistent versions across all modules and simplifies dependency management in projects.

## Why Use a BOM?

A **Bill of Materials (BOM)** is a Maven POM that declares dependency versions without adding those dependencies to your project. Benefits include:

- **Version Consistency**: All modules use the same version of each dependency
- **Simplified POMs**: Consuming projects don't need to specify versions
- **Conflict Prevention**: Reduces version conflicts and ensures compatibility
- **Security Updates**: One place to update vulnerable dependencies
- **Testing**: All versions are tested together in the platform builds

## How to Use

### In Your Project

Add the BOM to your `pom.xml`:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.marcusprado02.commons</groupId>
            <artifactId>commons-bom</artifactId>
            <version>0.1.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Then add dependencies without specifying versions:

```xml
<dependencies>
    <!-- Commons modules -->
    <dependency>
        <groupId>com.marcusprado02.commons</groupId>
        <artifactId>commons-kernel-ddd</artifactId>
    </dependency>

    <!-- External dependencies -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## Managed Dependencies

### BOMs (Imported)

The following BOMs are imported and provide transitive dependency management:

| BOM | Version | Provides |
|-----|---------|----------|
| `spring-boot-dependencies` | 3.5.10 | Spring Framework, Jackson, Tomcat, Logback, etc. |
| `spring-cloud-dependencies` | 2024.0.4 | Cloud Config, Circuit Breaker, Service Discovery |
| `testcontainers-bom` | 1.20.5 | Container modules (PostgreSQL, MongoDB, Kafka) |
| `opentelemetry-bom` | 1.58.0 | OpenTelemetry API and SDK |
| `micrometer-bom` | 1.15.3 | Metrics and observability |
| `resilience4j-bom` | 2.2.0 | Circuit Breaker, Retry, Rate Limiter |
| `jackson-bom` | 2.18.2 | JSON processing libraries |

### Direct Versions

| Category | Library | Version | Purpose |
|----------|---------|---------|---------|
| **Testing** | JUnit Jupiter | 5.10.2 | Test framework |
| | Mockito | 5.15.2 | Mocking framework |
| | AssertJ | 3.25.3 | Fluent assertions |
| | ArchUnit | 1.4.1 | Architecture testing |
| **Persistence** | Jakarta Persistence API | 3.1.0 | JPA specification |
| | Hibernate | 6.7.2.Final | JPA implementation |
| **Logging** | SLF4J | 2.0.13 | Logging facade |
| | Logback | 1.5.12 | Logging implementation |
| **Caching** | Caffeine | 3.1.8 | High-performance cache |
| **Utilities** | Lombok | 1.18.36 | Boilerplate reduction |
| | MapStruct | 1.6.3 | Mapping code generation |

### Internal Modules

All `commons-*` modules are versioned together using `${project.version}`:

- **Kernel**: core, ddd, errors, result, time
- **Ports**: persistence, messaging, http, cache, secrets, files
- **Application**: observability, resilience, outbox, idempotency
- **Adapters**: web, web-spring, web-spring-webflux, otel, resilience4j, persistence-jpa, persistence-inmemory
- **Spring Starters**: observability, otel, resilience, outbox, idempotency
- **Quality**: testkit-core, archunit, quality

## Versioning Strategy

### Version Selection Criteria

When selecting dependency versions, we follow these principles:

1. **Stability Over Novelty**: Prefer stable, production-tested versions
2. **LTS Support**: Use Long-Term Support versions when available (e.g., Spring Boot 3.x)
3. **Security**: Prioritize versions without known CVEs
4. **Compatibility**: Ensure versions work together (validated through builds)
5. **Community Adoption**: Prefer widely-used versions with active support

### Update Frequency

- **Major Versions**: Reviewed quarterly, planned migrations
- **Minor Versions**: Updated monthly if stable
- **Patch Versions**: Updated bi-weekly (security patches immediately)
- **Security Updates**: Applied within 24-48 hours of disclosure

### Testing Requirements

Before updating a dependency version:

1. **Build Validation**: All modules must compile successfully
2. **Unit Tests**: All tests must pass (80% line coverage, 75% branch coverage)
3. **Static Analysis**: No new critical issues from SpotBugs/Checkstyle/PMD
4. **Security Scan**: No new CVSS ≥ 7.0 vulnerabilities
5. **Smoke Tests**: Examples must run successfully

## Version Alignment

### Spring Ecosystem

All Spring dependencies are aligned through `spring-boot-dependencies` BOM:

- Spring Framework: 6.x
- Spring Boot: 3.5.x
- Spring Cloud: 2024.0.x
- Spring Data: 2024.0.x

### Testing Stack

All testing libraries are aligned for JUnit 5:

- JUnit Jupiter: 5.10.x
- Mockito: 5.x (JUnit 5 support)
- AssertJ: 3.25.x
- Testcontainers: 1.20.x

### Observability

OpenTelemetry and Micrometer versions are aligned:

- OpenTelemetry API: 1.58.0
- OpenTelemetry Instrumentation: 2.24.0
- Micrometer: 1.15.3

## Overriding Versions

### When to Override

Only override BOM versions when:

- Security vulnerability in BOM version
- Critical bug fix in newer version
- Specific feature requirement
- Temporary workaround (document in code comments)

### How to Override

In your project's `pom.xml`, define the version **before** importing the BOM:

```xml
<properties>
    <!-- Override commons-bom version -->
    <jackson.version>2.18.3</jackson.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.marcusprado02.commons</groupId>
            <artifactId>commons-bom</artifactId>
            <version>0.1.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Important**: Document why you're overriding and create a task to remove the override when no longer needed.

## Dependabot Integration

The project uses Dependabot to automate dependency updates:

- **Frequency**: Weekly scans for Maven and GitHub Actions
- **Auto-Merge**: Patch-level updates auto-merged after CI passes
- **Grouping**: Related dependencies updated together
- **Security**: High-priority security updates flagged for immediate review

Configuration in `.github/dependabot.yml`.

## Security Policy

### Vulnerability Management

1. **Detection**: Daily OWASP Dependency-Check scans in CI
2. **Threshold**: CVSS ≥ 7.0 fails the build
3. **Response Time**: 
   - CVSS 9.0-10.0 (Critical): 24 hours
   - CVSS 7.0-8.9 (High): 48 hours
   - CVSS 4.0-6.9 (Medium): 1 week
4. **Mitigation**: Update, suppress (with justification), or replace

### Suppression Files

Known false positives are documented in:
- `dependency-check-suppressions.xml` - OWASP suppressions
- Individual module README files

## Migration Guide

### Adopting commons-bom

1. **Add BOM import** to `dependencyManagement`
2. **Remove version tags** from dependencies
3. **Run `mvn dependency:tree`** to verify resolved versions
4. **Test thoroughly** - ensure no behavior changes
5. **Update CI** to validate against BOM versions

### Upgrading BOM Version

When upgrading `commons-bom` in your project:

```bash
# 1. Update version
mvn versions:set-property -Dproperty=commons.version -DnewVersion=0.2.0

# 2. Check for issues
mvn clean verify

# 3. Review dependency changes
mvn dependency:tree > tree-new.txt
# Compare with previous tree

# 4. Update documentation
# Document any breaking changes or new features
```

## Best Practices

### DO

✅ Import the BOM in `dependencyManagement`  
✅ Omit versions for dependencies managed by the BOM  
✅ Document version overrides with reasons  
✅ Test thoroughly before updating the BOM version  
✅ Report incompatibilities to the commons team  

### DON'T

❌ Add `commons-bom` as a direct dependency  
❌ Duplicate version properties from the BOM  
❌ Override versions without documentation  
❌ Mix different Spring Boot versions  
❌ Ignore security scan warnings  

## Troubleshooting

### Version Conflicts

If you see version conflicts:

```bash
# View dependency tree
mvn dependency:tree

# Find which dependency brings version X
mvn dependency:tree -Dincludes=groupId:artifactId

# Analyze conflicts
mvn dependency:analyze
```

### BOM Not Working

1. Ensure BOM is in `dependencyManagement`, not `dependencies`
2. Verify `<type>pom</type>` and `<scope>import</scope>`
3. Check BOM import order (overrides apply in order)
4. Ensure dependency `groupId:artifactId` matches BOM exactly

### Unexpected Versions

```bash
# Show effective POM
mvn help:effective-pom

# Show where version came from
mvn dependency:tree -Dverbose
```

## Contributing

To update the BOM:

1. Update version in `commons-bom/pom.xml` properties
2. Run full build: `mvn clean verify`
3. Run security scan: `mvn dependency-check:check`
4. Update this document if adding new dependencies
5. Update `BACKLOG.md` with changes
6. Create PR with changes and test results

## References

- [Maven BOM Documentation](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#bill-of-materials-bom-poms)
- [Spring Boot Dependency Management](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.build-systems.dependency-management)
- [OWASP Dependency-Check](https://owasp.org/www-project-dependency-check/)
- [Dependabot Documentation](https://docs.github.com/en/code-security/dependabot)

## Version History

| BOM Version | Release Date | Notable Changes |
|-------------|--------------|-----------------|
| 0.1.0-SNAPSHOT | TBD | Initial release with comprehensive dependency management |

---

**Last Updated**: 2025-01-XX  
**Maintained By**: java-commons team

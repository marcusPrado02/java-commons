# Migration Guide Template

> **Template for creating migration guides between major versions**  
> Copy this template when creating a new migration guide for a major version upgrade.

---

## Migration from vX.Y.Z to vA.B.C

**Version**: vX.Y.Z → vA.B.C  
**Release Date**: YYYY-MM-DD  
**Migration Effort**: 🟢 Low / 🟡 Medium / 🔴 High  
**Estimated Time**: X-Y hours (small projects) / Y-Z hours (large projects)

---

## Table of Contents

- [Overview](#overview)
- [Breaking Changes](#breaking-changes)
- [Deprecated Features](#deprecated-features)
- [New Features](#new-features)
- [Dependency Changes](#dependency-changes)
- [Migration Steps](#migration-steps)
- [Automated Migration](#automated-migration)
- [Manual Migration](#manual-migration)
- [Testing Changes](#testing-changes)
- [Configuration Changes](#configuration-changes)
- [Rollback Plan](#rollback-plan)
- [FAQ](#faq)
- [Support](#support)

---

## Overview

### What's New

Brief summary of major changes and improvements in this release.

**Highlights**:
- ✨ Feature 1: Description
- ⚡ Performance improvement in X
- 🔒 Security enhancement for Y
- 🐛 Bug fixes for Z

### Why Upgrade?

- **Reason 1**: Explanation
- **Reason 2**: Explanation
- **Reason 3**: Explanation

### Migration Complexity

| Component | Impact | Effort |
|-----------|--------|--------|
| Module A  | 🔴 High | 4-6 hours |
| Module B  | 🟡 Medium | 2-3 hours |
| Module C  | 🟢 Low | 30 minutes |

---

## Breaking Changes

### 1. Breaking Change Title

**Impact**: 🔴 High / 🟡 Medium / 🟢 Low  
**Affected Modules**: `module-name`  
**Migration Time**: X hours

#### What Changed

Detailed explanation of what changed and why.

#### Before (vX.Y.Z)

```java
// Old code example
public class OldWay {
    public void oldMethod() {
        // Old implementation
    }
}
```

#### After (vA.B.C)

```java
// New code example
public class NewWay {
    public void newMethod() {
        // New implementation
    }
}
```

#### Migration Path

1. **Step 1**: Detailed instruction
   ```java
   // Code example
   ```

2. **Step 2**: Detailed instruction
   ```java
   // Code example
   ```

3. **Step 3**: Detailed instruction

#### Automated Fix

```bash
# Command or script to automatically fix this change
./scripts/migrate-vX-to-vA.sh --fix breaking-change-1
```

#### Manual Fix

If automated migration is not possible:

1. Search for usages: `grep -r "OldClass" src/`
2. Replace with: `NewClass`
3. Update imports: `import com.example.NewClass`

---

### 2. Another Breaking Change

*(Repeat the structure above for each breaking change)*

---

## Deprecated Features

### Deprecated Class/Method

**Deprecated in**: vX.Y.Z  
**Removal in**: vA.B.C  
**Alternative**: Use `NewClass` instead

#### Example

```java
// ❌ Deprecated - will be removed in vA.B.C
@Deprecated(since = "X.Y.Z", forRemoval = true)
public void oldMethod() { }

// ✅ Recommended
public void newMethod() { }
```

---

## New Features

### Feature Name

Brief description of the new feature.

#### Usage Example

```java
// Example of how to use the new feature
public class NewFeatureExample {
    public void example() {
        // Implementation
    }
}
```

#### Benefits

- Benefit 1
- Benefit 2
- Benefit 3

---

## Dependency Changes

### Updated Dependencies

| Dependency | Old Version | New Version | Notes |
|------------|-------------|-------------|-------|
| Spring Boot | 3.0.0 | 3.2.0 | See Spring Boot migration guide |
| Jackson | 2.14.0 | 2.15.0 | No breaking changes |
| Hibernate | 6.1.0 | 6.4.0 | See notes below |

### New Dependencies

- **dependency-name** (version): Description of why it was added

### Removed Dependencies

- **old-dependency** (version): Reason for removal, alternative if applicable

---

## Migration Steps

### Prerequisites

- [ ] Java 21 installed
- [ ] Maven 3.9+ or Gradle 8+
- [ ] All tests passing on vX.Y.Z
- [ ] Code committed to version control
- [ ] Create backup branch: `git checkout -b backup-before-vA`

### Step-by-Step Migration

#### 1. Update Dependencies

```xml
<!-- Update in pom.xml -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-bom</artifactId>
    <version>A.B.C</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

#### 2. Run Automated Migration Script

```bash
# Download migration script
curl -o migrate.sh https://raw.githubusercontent.com/marcusPrado02/java-commons/main/scripts/migrate-vX-to-vA.sh
chmod +x migrate.sh

# Run migration
./migrate.sh --dry-run  # Preview changes
./migrate.sh --apply    # Apply changes
```

#### 3. Fix Compilation Errors

```bash
# Build to identify issues
mvn clean compile

# Review and fix errors
# See detailed migration instructions for each breaking change above
```

#### 4. Update Tests

```bash
# Run tests
mvn test

# Fix failing tests
# Update assertions, mocks, and test data as needed
```

#### 5. Update Configuration

Update application configuration files:

```yaml
# application.yml
commons:
  new-feature:
    enabled: true
    # New configuration options
```

#### 6. Verify Migration

```bash
# Run full build with tests
mvn clean verify

# Run integration tests
mvn verify -P integration-tests

# Manual verification checklist:
# [ ] All tests pass
# [ ] Application starts successfully
# [ ] No deprecation warnings (or documented)
# [ ] Performance benchmarks pass
```

---

## Automated Migration

### Migration Script

A migration script is provided to automate common changes:

```bash
# Download and run
curl -sSL https://raw.githubusercontent.com/marcusPrado02/java-commons/main/scripts/migrate-vX-to-vA.sh | bash

# Or clone and run locally
git clone https://github.com/marcusPrado02/java-commons.git
cd java-commons/scripts
./migrate-vX-to-vA.sh /path/to/your/project
```

### What the Script Does

- ✅ Updates import statements
- ✅ Renames deprecated methods
- ✅ Updates configuration files
- ✅ Fixes common code patterns
- ✅ Updates documentation

### What Requires Manual Intervention

- ⚠️ Custom implementations of deprecated interfaces
- ⚠️ Complex business logic changes
- ⚠️ Test assertions that depend on old behavior

---

## Manual Migration

### For Each Breaking Change

For detailed migration instructions for each breaking change, see the [Breaking Changes](#breaking-changes) section above.

### Common Patterns

#### Pattern 1: Method Signature Changes

```java
// Find all usages
grep -r "oldMethod" src/

// Replace with new signature
// Before: result.oldMethod(param)
// After:  result.newMethod(param, additionalParam)
```

#### Pattern 2: Return Type Changes

```java
// Before: Optional<T>
Optional<User> user = repo.findById(id);

// After: Result<T>
Result<User> user = repo.findById(id);
user.onSuccess(u -> System.out.println(u.getName()));
```

---

## Testing Changes

### Test Dependency Updates

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-testkit-core</artifactId>
    <version>A.B.C</version>
    <scope>test</scope>
</dependency>
```

### Updated Test Utilities

- **Old**: `TestHelper.createUser()`
- **New**: `UserTestBuilder.aUser().build()`

### Example Test Migration

```java
// Before
@Test
void testOldWay() {
    User user = TestHelper.createUser();
    assertTrue(user.isActive());
}

// After
@Test
void testNewWay() {
    User user = UserTestBuilder.aUser()
        .withActive(true)
        .build();
    assertThat(user.isActive()).isTrue();
}
```

---

## Configuration Changes

### Application Properties

#### Renamed Properties

| Old Property | New Property | Notes |
|--------------|--------------|-------|
| `old.property` | `new.property` | Description |

#### New Properties

```properties
# New configuration options
commons.new-feature.enabled=true
commons.new-feature.timeout=5s
```

#### Removed Properties

- `obsolete.property` - No longer used, remove from configuration

### Spring Boot Configuration

```yaml
# application.yml updates
commons:
  feature-flags:
    enabled: true
    source: remote  # New option
```

---

## Rollback Plan

### If Migration Fails

1. **Restore from backup**:
   ```bash
   git checkout backup-before-vA
   git branch -D main
   git checkout -b main
   ```

2. **Revert dependency versions**:
   ```xml
   <version>X.Y.Z</version>  <!-- Roll back to previous version -->
   ```

3. **Rebuild and test**:
   ```bash
   mvn clean install
   mvn test
   ```

### Known Issues and Workarounds

#### Issue 1: Description

**Workaround**:
```java
// Temporary workaround code
```

**Tracking**: [Issue #XXX](https://github.com/marcusPrado02/java-commons/issues/XXX)

---

## FAQ

### Q: How long does migration typically take?

**A**: For small projects (< 10k LOC): 2-4 hours  
For medium projects (10k-50k LOC): 4-8 hours  
For large projects (> 50k LOC): 1-2 days

### Q: Can I migrate incrementally?

**A**: No, this is a major version upgrade requiring all changes at once. However, you can:
1. Update dependencies first
2. Fix compilation errors
3. Update tests
4. Deploy to staging for validation

### Q: What if I'm not ready to upgrade?

**A**: Version X.Y.Z will receive security patches for 6 months. See [Version Support Policy](../versioning-and-releases.md#version-support).

### Q: Are there any performance impacts?

**A**: Generally, performance is improved or unchanged. Specific benchmarks:
- Feature X: 20% faster
- Feature Y: No change
- Feature Z: 5% memory reduction

### Q: How do I report migration issues?

**A**: Open an issue: [GitHub Issues](https://github.com/marcusPrado02/java-commons/issues)  
Include:
- Source version
- Target version
- Error messages
- Minimal reproduction

---

## Support

### Resources

- **Documentation**: [https://github.com/marcusPrado02/java-commons/docs](https://github.com/marcusPrado02/java-commons/docs)
- **Changelog**: [CHANGELOG.md](../../CHANGELOG.md)
- **Release Notes**: [vA.B.C Release](https://github.com/marcusPrado02/java-commons/releases/tag/vA.B.C)
- **API Docs**: [JavaDoc](https://javadoc.io/doc/com.marcusprado02.commons)

### Community

- **GitHub Issues**: [Report bugs or request features](https://github.com/marcusPrado02/java-commons/issues)
- **Discussions**: [Ask questions](https://github.com/marcusPrado02/java-commons/discussions)

### Need Help?

If you encounter issues during migration:

1. Check this migration guide
2. Review the [FAQ](#faq)
3. Search [existing issues](https://github.com/marcusPrado02/java-commons/issues)
4. Open a new issue with:
   - Source version
   - Target version
   - Steps to reproduce
   - Error messages
   - Project context

---

**Last Updated**: YYYY-MM-DD  
**Template Version**: 1.0.0

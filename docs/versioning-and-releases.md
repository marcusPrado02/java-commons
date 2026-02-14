# Versioning and Release Strategy

This document describes the versioning, release, and change management strategy for the Java Commons library.

## Table of Contents

- [Semantic Versioning](#semantic-versioning)
- [Conventional Commits](#conventional-commits)
- [Release Process](#release-process)
- [Backward Compatibility](#backward-compatibility)
- [Changelog](#changelog)
- [Deprecation Policy](#deprecation-policy)
- [Version Support](#version-support)

---

## Semantic Versioning

This project adheres strictly to [Semantic Versioning 2.0.0](https://semver.org/).

### Version Format

```
MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]
```

- **MAJOR**: Incompatible API changes (breaking changes)
- **MINOR**: New functionality in a backward-compatible manner
- **PATCH**: Backward-compatible bug fixes
- **PRERELEASE**: Optional pre-release identifier (e.g., `-alpha.1`, `-beta.2`, `-rc.1`)
- **BUILD**: Optional build metadata (e.g., `+20130313144700`)

### Version Increment Rules

| Change Type | Version Bump | Example |
|------------|--------------|---------|
| Breaking change (API incompatibility) | MAJOR | `1.2.3` → `2.0.0` |
| New feature (backward compatible) | MINOR | `1.2.3` → `1.3.0` |
| Bug fix (backward compatible) | PATCH | `1.2.3` → `1.2.4` |

### Examples of Breaking Changes

**MAJOR version changes** are required when introducing:

1. **Removing public APIs**
   ```java
   // Before (v1.0.0)
   public class UserService {
       public void deleteUser(String id) { ... }
   }
   
   // After (v2.0.0) - BREAKING
   public class UserService {
       // Method removed - requires MAJOR bump
   }
   ```

2. **Changing method signatures**
   ```java
   // Before (v1.0.0)
   public Result<User> findUser(String id) { ... }
   
   // After (v2.0.0) - BREAKING
   public Result<User> findUser(UUID id) { ... }
   ```

3. **Changing return types**
   ```java
   // Before (v1.0.0)
   public User getUser(String id) { ... }
   
   // After (v2.0.0) - BREAKING
   public Optional<User> getUser(String id) { ... }
   ```

4. **Reducing visibility**
   ```java
   // Before (v1.0.0)
   public class Configuration { ... }
   
   // After (v2.0.0) - BREAKING
   protected class Configuration { ... }
   ```

5. **Changing behavior**
   - Throwing new checked exceptions
   - Changing validation rules that reject previously valid inputs
   - Altering default values that affect behavior

### Examples of Non-Breaking Changes

**MINOR version changes** for:

1. **Adding new public APIs**
   ```java
   // Before (v1.0.0)
   public class UserService {
       public User findUser(String id) { ... }
   }
   
   // After (v1.1.0) - Non-breaking
   public class UserService {
       public User findUser(String id) { ... }
       public List<User> findUsers(List<String> ids) { ... } // New method
   }
   ```

2. **Adding optional parameters (via overloading)**
   ```java
   // Before (v1.0.0)
   public void notify(String message) { ... }
   
   // After (v1.1.0) - Non-breaking
   public void notify(String message) { ... }
   public void notify(String message, NotificationOptions options) { ... }
   ```

3. **Deprecating APIs** (with at least one minor version notice)
   ```java
   // v1.1.0 - Non-breaking
   @Deprecated(since = "1.1.0", forRemoval = true)
   public void oldMethod() { ... }
   
   public void newMethod() { ... }
   ```

**PATCH version changes** for:

1. **Bug fixes**
2. **Performance improvements** (without API changes)
3. **Internal refactoring**
4. **Documentation updates**
5. **Dependency updates** (patch versions only)

---

## Conventional Commits

All commit messages MUST follow the [Conventional Commits](https://www.conventionalcommits.org/) specification.

### Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

| Type | Description | Version Impact |
|------|-------------|----------------|
| `feat` | New feature | MINOR |
| `fix` | Bug fix | PATCH |
| `docs` | Documentation only | No bump |
| `style` | Code style (formatting) | No bump |
| `refactor` | Code refactoring | No bump |
| `perf` | Performance improvement | PATCH |
| `test` | Tests only | No bump |
| `build` | Build system/dependencies | No bump |
| `ci` | CI/CD configuration | No bump |
| `chore` | Other changes | No bump |
| `revert` | Revert a commit | Depends |

### Breaking Changes

Breaking changes are indicated by:

1. **`!` after type/scope**:
   ```
   feat(kernel)!: remove deprecated Result.getOrThrow method
   ```

2. **`BREAKING CHANGE:` in footer**:
   ```
   feat(persistence): change Repository interface

   BREAKING CHANGE: Repository.save() now returns Result<T> instead of T
   ```

### Examples

```bash
# New feature
feat(configuration): add support for remote configuration providers

# Bug fix
fix(result): handle null values correctly in Result.map

# Breaking change
feat(kernel)!: change DomainEvent interface signature

BREAKING CHANGE: DomainEvent.occurredOn() now returns Instant instead of ZonedDateTime

# Documentation
docs(readme): update installation instructions

# Performance improvement
perf(cache): optimize Redis serialization

# Multiple scopes
feat(web,messaging): add correlation ID propagation
```

### Commit Message Template

A Git commit message template is provided in `.gitmessage`. To use it:

```bash
git config commit.template .gitmessage
```

### Validation

Commit messages are automatically validated:

- **Locally**: Via git hooks (if configured)
- **CI/CD**: On every PR and push to main branches

See [`.commitlintrc.json`](../.commitlintrc.json) for validation rules.

---

## Release Process

### Automated Release (Recommended)

Releases are automated using GitHub Actions. To create a new release:

1. **Trigger the release workflow**:
   - Go to **Actions** → **Release**
   - Click **Run workflow**
   - Select release type: `major`, `minor`, or `patch`
   - Click **Run workflow**

2. **The workflow will**:
   - Calculate the new version
   - Update `pom.xml` versions
   - Generate/update `CHANGELOG.md`
   - Run tests and build
   - Create a git tag
   - Create a GitHub Release
   - Deploy to GitHub Packages
   - Bump to next SNAPSHOT version

### Manual Release

If needed, releases can be done manually:

1. **Prepare the release**:
   ```bash
   # Ensure clean working directory
   git status
   
   # Update to release version
   ./mvnw versions:set -DnewVersion=1.2.0 -DgenerateBackupPoms=false
   
   # Generate CHANGELOG
   ./mvnw git-changelog:git-changelog
   
   # Commit
   git add .
   git commit -m "chore: release version 1.2.0"
   git tag -a v1.2.0 -m "Release v1.2.0"
   ```

2. **Build and test**:
   ```bash
   ./mvnw clean verify
   ```

3. **Deploy**:
   ```bash
   ./mvnw deploy -P release
   ```

4. **Bump to next snapshot**:
   ```bash
   ./mvnw versions:set -DnewVersion=1.2.1-SNAPSHOT -DgenerateBackupPoms=false
   git add .
   git commit -m "chore: prepare for next development iteration 1.2.1-SNAPSHOT"
   ```

5. **Push to GitHub**:
   ```bash
   git push origin main
   git push origin v1.2.0
   ```

### Release Checklist

Before creating a release:

- ✅ All tests pass
- ✅ CHANGELOG is up to date
- ✅ Documentation is updated
- ✅ No backward incompatible changes (unless MAJOR bump)
- ✅ All deprecation notices are in place
- ✅ Security vulnerabilities are addressed

---

## Backward Compatibility

### Compatibility Checks

Backward compatibility is automatically verified using **JApiCmp** (Java API Compliance Checker).

JApiCmp runs on:
- Every PR to main branches
- Before each release

### What is Checked

JApiCmp detects:

1. **Binary incompatibilities**:
   - Removed classes, methods, fields
   - Changed method signatures
   - Changed field types
   - Reduced visibility

2. **Source incompatibilities**:
   - Return type changes
   - Exception changes
   - Generic type changes

3. **Behavioral incompatibilities**:
   - Changed semantics (if documented in JavaDoc)

### Handling Incompatibilities

If JApiCmp detects incompatible changes:

1. **Option 1: Fix the incompatibility**
   - Restore the removed API
   - Use method overloading instead of changing signatures
   - Deprecate old APIs and add new ones

2. **Option 2: Accept the breaking change**
   - Ensure this is a MAJOR version bump
   - Document in CHANGELOG with `BREAKING CHANGE:` section
   - Update migration guide

### Configuration

JApiCmp configuration is in [`pom.xml`](../pom.xml):

```xml
<plugin>
  <groupId>com.github.siom79.japicmp</groupId>
  <artifactId>japicmp-maven-plugin</artifactId>
  <configuration>
    <breakBuildOnBinaryIncompatibleModifications>true</breakBuildOnBinaryIncompatibleModifications>
  </configuration>
</plugin>
```

### Running Manually

Check compatibility locally:

```bash
# Compare current code against latest release
./mvnw japicmp:cmp

# View HTML report
open **/target/japicmp/japicmp.html
```

---

## Changelog

### Automatic Generation

The `CHANGELOG.md` is automatically generated from commit messages using the **git-changelog-maven-plugin**.

Changelog generation happens:
- During release builds
- Manually via: `./mvnw git-changelog:git-changelog`

### Format

The changelog follows [Keep a Changelog](https://keepachangelog.com/) format:

```markdown
# Changelog

## [1.2.0] - 2026-02-14

### ✨ Features
- Add support for remote configuration providers ([abc123](link))

### 🐛 Bug Fixes
- Fix null handling in Result.map ([def456](link))

### ⚠️ BREAKING CHANGES
- Change DomainEvent interface signature ([ghi789](link))
```

### Sections

Commits are grouped by type:
- ✨ **Features** (`feat`)
- 🐛 **Bug Fixes** (`fix`)
- ⚡ **Performance Improvements** (`perf`)
- ♻️ **Code Refactoring** (`refactor`)
- 📚 **Documentation** (`docs`)
- ✅ **Tests** (`test`)
- 🏗️ **Build System** (`build`)
- 👷 **CI/CD** (`ci`)

### Manual Edits

While the changelog is auto-generated, you can manually edit it to:
- Add release notes and highlights
- Provide migration instructions
- Include links to documentation

**Note**: Manual edits should be made to the relevant release section, not the template.

---

## Deprecation Policy

### Deprecation Process

1. **Mark as deprecated** (in MINOR version)
   ```java
   /**
    * Use {@link #newMethod()} instead.
    * @deprecated since 1.5.0, for removal in 2.0.0
    */
   @Deprecated(since = "1.5.0", forRemoval = true)
   public void oldMethod() {
       newMethod(); // Delegate to new implementation
   }
   
   public void newMethod() {
       // New implementation
   }
   ```

2. **Document in CHANGELOG**
   ```markdown
   ### Deprecated
   - `UserService.oldMethod()` - use `newMethod()` instead (removal in v2.0.0)
   ```

3. **Maintain for at least ONE MINOR release**
   - v1.5.0: Method deprecated
   - v1.6.0: Still available (deprecated)
   - v2.0.0: Can be removed

4. **Remove in next MAJOR version**
   ```java
   // v2.0.0 - Method removed
   public class UserService {
       public void newMethod() { ... }
   }
   ```

### Deprecation Timeline

| Version | Action |
|---------|--------|
| 1.5.0 | API deprecated with `@Deprecated` annotation and JavaDoc |
| 1.6.0 | API still available (deprecated warnings in logs/docs) |
| 1.7.0 | API still available (deprecated warnings) |
| 2.0.0 | API removed |

### Deprecation Notification

Deprecated APIs are communicated via:
1. **JavaDoc** with `@deprecated` tag
2. **`@Deprecated` annotation** with `since` and `forRemoval`
3. **Compiler warnings** when used
4. **CHANGELOG** entry
5. **Migration guide** in docs

---

## Version Support

### Support Policy

| Version Type | Support Duration | Updates |
|-------------|------------------|---------|
| **Latest MAJOR.MINOR** | Until next MINOR release | Bug fixes, security patches |
| **Previous MINOR** | 6 months | Critical security patches only |
| **Older versions** | Best effort | No guaranteed updates |

### Example

Current version: `2.3.0`

| Version | Status | Support |
|---------|--------|---------|
| 2.3.x | **Current** | Full support |
| 2.2.x | **Maintenance** | Security patches (6 months) |
| 2.1.x | **Unsupported** | No updates |
| 1.x.x | **Unsupported** | No updates |

### Security Patches

Security vulnerabilities are addressed:
- **Current version**: Immediate patch release
- **Previous MINOR**: Patch within 30 days (if feasible)
- **Older versions**: Upgrade to latest recommended

### Dependency Updates

Dependencies are updated:
- **Weekly**: Automated Dependabot PRs for patch versions
- **Monthly**: Review and test minor version updates
- **Quarterly**: Evaluate major version updates

---

## Migration Guides

When breaking changes are introduced, migration guides are provided:

1. **In CHANGELOG**: Brief migration instructions
2. **In `docs/migrations/`**: Detailed migration guides per MAJOR version

Example: [`docs/migrations/v1-to-v2.md`](migrations/v1-to-v2.md)

---

## Tools and Configuration

### Maven Plugins

- **maven-release-plugin**: Automates release process
- **versions-maven-plugin**: Version management
- **git-changelog-maven-plugin**: CHANGELOG generation
- **japicmp-maven-plugin**: Compatibility verification

### Git Configuration

- **`.gitmessage`**: Commit message template
- **`.commitlintrc.json`**: Commit message validation rules
- **`.cz.toml`**: Commitizen configuration

### CI/CD Workflows

- **`release.yml`**: Automated release workflow
- **`validate-commits.yml`**: Commit message validation
- **`compatibility-check.yml`**: Backward compatibility verification

---

## FAQ

### Q: Can I use emojis in commit messages?

**A**: Emojis in commit message bodies are fine, but the subject line should follow Conventional Commits format without emojis.

```bash
# ✅ Good
feat(kernel): add Result monad

# ❌ Bad
✨ add Result monad
```

### Q: What if I forget to follow Conventional Commits?

**A**: The CI/CD pipeline will reject commits that don't follow the format. Amend your commits or use interactive rebase to fix commit messages.

### Q: How do I know what version bump is needed?

**A**: Use the Compatibility Check workflow results and commit message analysis:
- `BREAKING CHANGE:` or `!` = MAJOR
- `feat:` = MINOR
- `fix:` or `perf:` = PATCH

### Q: Can I create pre-releases?

**A**: Yes, use versions like `1.0.0-alpha.1`, `1.0.0-beta.1`, `1.0.0-rc.1`:

```bash
./mvnw versions:set -DnewVersion=2.0.0-rc.1
```

### Q: How long are deprecated APIs supported?

**A**: Minimum ONE MINOR release cycle (e.g., deprecated in 1.5.0, can be removed in 2.0.0).

---

## References

- [Semantic Versioning 2.0.0](https://semver.org/)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Keep a Changelog](https://keepachangelog.com/)
- [JApiCmp Documentation](https://siom79.github.io/japicmp/)
- [Maven Release Plugin](https://maven.apache.org/maven-release/maven-release-plugin/)

---

**Last Updated**: 2026-02-14

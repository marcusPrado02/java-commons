# Semantic Versioning Policy — Public Kernel APIs

This document defines what constitutes a **breaking change** in the `commons-kernel-*`
and `commons-ports-*` modules, and when each version number component must be incremented.

## Version Format

```
MAJOR.MINOR.PATCH  (e.g., 1.3.2)
```

Follows [Semantic Versioning 2.0.0](https://semver.org/).

---

## MAJOR version — Breaking Changes

Increment MAJOR when any of the following occur in `commons-kernel-*` or `commons-ports-*`:

### API surface changes
| Change | Breaking? |
|---|---|
| Remove a public class, interface, or enum | **YES** |
| Remove a public or protected method | **YES** |
| Remove a public or protected field | **YES** |
| Change a method signature (parameter types, return type) | **YES** |
| Change a method from instance to static (or vice versa) | **YES** |
| Add a non-default method to a public interface | **YES** — breaks implementors |
| Remove a sealed class `permits` entry | **YES** |
| Change a record's component order or type | **YES** |
| Change the package of a public type | **YES** |
| Narrow the visibility of a public member (e.g., `public` → `protected`) | **YES** |
| Change a checked exception to unchecked (or vice versa) | **YES** |
| Change enum constant names or ordinals | **YES** |

### Behavioral contract changes
| Change | Breaking? |
|---|---|
| Change the semantics of `Result.ok()` / `Result.fail()` | **YES** |
| Change `equals()`/`hashCode()` contract for `ErrorCode`, `Problem`, etc. | **YES** |
| Change thread-safety guarantees of a public class | **YES** |
| Change exception type thrown by a public method | **YES** |

### Module-level changes
| Change | Breaking? |
|---|---|
| Remove a module from `commons-bom` | **YES** |
| Change a module's `groupId` or `artifactId` | **YES** |
| Remove or rename a `commons-ports-*` interface | **YES** |

---

## MINOR version — New Features (Backward Compatible)

Increment MINOR when adding new functionality that does not break existing consumers:

| Change | MINOR? |
|---|---|
| Add a new public class or interface | Yes |
| Add a new method to a class (not an interface) | Yes |
| Add a **default** method to an existing interface | Yes |
| Add a new module to `commons-bom` | Yes |
| Add a new `Result` combinator (`mapAsync`, `flatMapAsync`, etc.) | Yes |
| Add a new factory method to an existing class | Yes |
| Add a new `sealed` permits entry | Yes |
| Add a new enum constant (if consumers use exhaustive switch, coordinate) | Yes* |
| Deprecate (but not remove) a public API | Yes |

*Adding enum constants to an interface used in exhaustive `switch` expressions
may cause compile errors in consumer code. Announce 1 minor version in advance.

---

## PATCH version — Bug Fixes (Backward Compatible)

Increment PATCH for fixes that do not alter the public API:

| Change | PATCH? |
|---|---|
| Fix incorrect behavior without changing the method signature | Yes |
| Fix a NullPointerException in an edge case | Yes |
| Fix a thread-safety bug without changing the API | Yes |
| Update Javadoc without changing behavior | Yes |
| Internal refactoring with identical external behavior | Yes |
| Dependency version bump (patch-level) that fixes a CVE | Yes |
| Performance improvement with identical semantics | Yes |

---

## Special Rules for Sealed Interfaces

`Result<T>` is a sealed interface with `Ok<T>` and `Fail<T>` as its permitted subtypes.
Any change to the permitted subtypes is a **MAJOR** change, because consumers may use
exhaustive `switch` expressions that would fail to compile.

```java
// Consumer code that would break if a third subtype is added:
return switch (result) {
    case Result.Ok<T> ok -> ok.value();
    case Result.Fail<T> fail -> throw new RuntimeException(fail.problem().message());
};
```

---

## Deprecation Policy

Before removing a public API in a MAJOR release:

1. Annotate with `@Deprecated` in a MINOR release
2. Add Javadoc `@deprecated` explaining the replacement
3. Keep the deprecated API functional for **at least one full minor version cycle**
4. Announce the removal in `CHANGELOG.md` under the next MAJOR version section
5. Remove in the MAJOR release

---

## Enforcement

Binary compatibility between releases is verified by **JApiCmp** (`japicmp-maven-plugin`).
It runs in the `verify` phase for release builds and fails on binary-incompatible changes.

To check compatibility locally before release:
```bash
mvn verify -Djapicmp.skip=false -pl commons-kernel-result,commons-kernel-errors,commons-ports-payment
```

The JApiCmp configuration in the root `pom.xml` is set to `breakBuildOnBinaryIncompatibleModifications=true`
for the `release` profile.

---

## What Is NOT Covered

The following are **not** subject to this policy:

- Internal implementation classes in non-exported packages
- Test utilities (`commons-testkit-*`)
- `@Internal` annotated classes (opt-out from stability guarantee)
- `SNAPSHOT` versions — these never carry stability guarantees
- Documentation-only changes

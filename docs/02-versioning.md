# Versioning

We follow SemVer at repository level and per-module stability expectations.

## Repository version
- The root version (commons) is the version used for all modules.

## Stability expectations
- kernel-* modules: extremely stable; breaking changes are rare and deliberate.
- app-* modules: stable; breaking changes occasionally when capabilities evolve.
- adapters-* and spring-starter-* (future): may change faster.

## Breaking change rules (examples)
- Removing a public class/method/type in api/model packages
- Changing method signatures or generic bounds
- Changing serialization shape of contract types

## Release types
- PATCH: bugfixes, non-breaking improvements
- MINOR: new types/features, backward compatible
- MAJOR: breaking changes

# Commons Library Documentation

Welcome to the Java Commons library documentation. This directory contains comprehensive guides for using, extending, and migrating the Commons library.

## 📚 Core Documentation

### Architecture & Design
- [**Module Overview**](modules.md) - Complete list of all modules with descriptions
- [**Dependency Rules**](01-dependency-rules.md) - Architectural dependency constraints and guidelines
- [**Overview**](00-overview.md) - High-level architecture and design principles

### Versioning & Releases
- [**Versioning Strategy**](versioning-and-releases.md) ⭐ - Semantic versioning, conventional commits, release process
  - Semantic Versioning 2.0.0 specification
  - Conventional Commits format and validation
  - Release automation with GitHub Actions
  - Backward compatibility policies
  - Deprecation timeline and best practices
  - CHANGELOG generation
  - JApiCmp integration for API compatibility

### Upgrade & Migration
- [**Upgrade Process**](upgrade-process.md) ⭐ - Complete guide for upgrading between versions
  - Upgrade strategies (all-at-once, incremental, parallel)
  - Step-by-step process (6 phases)
  - Risk assessment and planning
  - Testing strategies
  - Rollback procedures
  - Best practices and troubleshooting

---

## 🔄 Migration Guides

Detailed step-by-step guides for migrating between major versions:

### Available Guides
- [**v0 → v1**](migrations/v0-to-v1.md) - Migration from v0.x to v1.0.0
- [**v1 → v2**](migrations/v1-to-v2.md) - Migration from v1.x to v2.0.0
- [**Template**](migrations/TEMPLATE.md) - Template for creating new migration guides

### What's in a Migration Guide?
Each migration guide includes:
- ✅ Breaking changes with before/after code examples
- ✅ Deprecated features and replacements
- ✅ New features and usage examples
- ✅ Dependency changes
- ✅ Step-by-step migration instructions
- ✅ Automated migration scripts
- ✅ Testing strategies
- ✅ Rollback plan
- ✅ FAQ and troubleshooting

---

## 🛠️ Automation Scripts

Located in [`../scripts/`](../scripts/README.md):

### Available Scripts

| Script | Purpose | Usage |
|--------|---------|-------|
| [migrate.sh](../scripts/migrate.sh) | Automate migration process | `./migrate.sh 1.0.0 2.0.0 /project` |
| [validate-pre-migration.sh](../scripts/validate-pre-migration.sh) | Validate project readiness | `./validate-pre-migration.sh /project 2.0.0` |
| [detect-deprecations.sh](../scripts/detect-deprecations.sh) | Find deprecated API usage | `./detect-deprecations.sh /project` |

**See**: [Scripts README](../scripts/README.md) for detailed usage and examples.

---

## 📖 Quick Start

### New to Commons?

1. Start with [Module Overview](modules.md) to understand the structure
2. Read [Dependency Rules](01-dependency-rules.md) for architectural constraints
3. See [Versioning Strategy](versioning-and-releases.md) for release information

### Planning an Upgrade?

1. Read [Upgrade Process](upgrade-process.md) for the complete workflow
2. Find your version in [Migration Guides](migrations/) (e.g., v1 → v2)
3. Run [Pre-Migration Validation](../scripts/README.md#2-validate-pre-migrationsh)
4. Use [Migration Scripts](../scripts/README.md) to automate the upgrade

### Creating a Release?

1. Review [Versioning Strategy](versioning-and-releases.md#release-process)
2. Follow [Conventional Commits](versioning-and-releases.md#conventional-commits)
3. Use [Release Workflow](../.github/workflows/release.yml)
4. Generate [CHANGELOG](versioning-and-releases.md#changelog) automatically

---

## 📋 Documentation Index

### By Topic

#### Architecture
- [00-overview.md](00-overview.md) - Architecture overview
- [01-dependency-rules.md](01-dependency-rules.md) - Dependency constraints
- [modules.md](modules.md) - Module catalog

#### Versioning
- [versioning-and-releases.md](versioning-and-releases.md) - Versioning strategy
- [02-versioning.md](02-versioning.md) - Version numbering

#### Migration
- [upgrade-process.md](upgrade-process.md) - Upgrade workflow
- [migrations/TEMPLATE.md](migrations/TEMPLATE.md) - Migration guide template
- [migrations/v0-to-v1.md](migrations/v0-to-v1.md) - v0 to v1 migration
- [migrations/v1-to-v2.md](migrations/v1-to-v2.md) - v1 to v2 migration

### By Audience

#### For Library Users
- [Upgrade Process](upgrade-process.md) - How to upgrade
- [Migration Guides](migrations/) - Version-specific instructions
- [Versioning Strategy](versioning-and-releases.md) - When to upgrade

#### For Contributors
- [Dependency Rules](01-dependency-rules.md) - Architectural constraints
- [Versioning Strategy](versioning-and-releases.md#conventional-commits) - Commit message format
- [Migration Template](migrations/TEMPLATE.md) - Creating migration guides

#### For Maintainers
- [Versioning Strategy](versioning-and-releases.md#release-process) - Release process
- [Backward Compatibility](versioning-and-releases.md#backward-compatibility) - API compatibility
- [Deprecation Policy](versioning-and-releases.md#deprecation-policy) - Deprecation workflow

---

## 🔗 External Resources

### Related Projects
- [GitHub Repository](https://github.com/marcusPrado02/java-commons)
- [CHANGELOG](../CHANGELOG.md)
- [Contributing Guide](../CONTRIBUTING.md)
- [License](../LICENSE)

### Standards & References
- [Semantic Versioning 2.0.0](https://semver.org/)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Keep a Changelog](https://keepachangelog.com/)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)

---

## 🤝 Contributing to Documentation

### Improving Existing Docs

Found an error or want to improve documentation?

1. Fork the repository
2. Edit the relevant `.md` file
3. Submit a pull request with:
   - Clear description of changes
   - Reason for the change
   - Any related issues

### Creating New Migration Guides

When creating a new major version release:

1. Copy [migrations/TEMPLATE.md](migrations/TEMPLATE.md)
2. Rename to `vX-to-vY.md` (e.g., `v2-to-v3.md`)
3. Fill in all sections with actual changes
4. Create corresponding [migration rules](../scripts/migrations/)
5. Update this README to include the new guide

### Documentation Standards

- **Format**: Markdown with proper headings
- **Code Examples**: Always include before/after
- **Links**: Use relative paths for internal docs
- **Tone**: Clear, concise, and helpful
- **Audience**: Assume intermediate Java knowledge

---

## 📞 Support

### Getting Help

- **Questions**: [GitHub Discussions](https://github.com/marcusPrado02/java-commons/discussions)
- **Bug Reports**: [GitHub Issues](https://github.com/marcusPrado02/java-commons/issues)
- **Documentation Issues**: [GitHub Issues](https://github.com/marcusPrado02/java-commons/issues) (label: documentation)

### Frequently Asked Questions

See the FAQ sections in:
- [Upgrade Process FAQ](upgrade-process.md#faq)
- [Versioning FAQ](versioning-and-releases.md#faq)
- Migration guide FAQs (e.g., [v1→v2 FAQ](migrations/v1-to-v2.md#faq))

---

## 📝 Document Status

| Document | Last Updated | Status |
|----------|--------------|--------|
| versioning-and-releases.md | 2026-02-14 | ✅ Current |
| upgrade-process.md | 2026-02-14 | ✅ Current |
| migrations/TEMPLATE.md | 2026-02-14 | ✅ Current |
| migrations/v0-to-v1.md | 2026-02-14 | ✅ Current |
| migrations/v1-to-v2.md | 2026-02-14 | ✅ Current |
| modules.md | 2025-xx-xx | 🟡 Review needed |
| 00-overview.md | 2025-xx-xx | 🟡 Review needed |
| 01-dependency-rules.md | 2025-xx-xx | 🟡 Review needed |

---

**Documentation Version**: 1.0.0  
**Last Updated**: 2026-02-14  
**Maintained by**: Commons Library Team

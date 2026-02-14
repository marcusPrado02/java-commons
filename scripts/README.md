# Migration Scripts

Automated tools to help migrate projects between major versions of the Commons library.

## Available Scripts

### 1. migrate.sh

Main migration script that automates the upgrade process.

**Usage**:
```bash
./migrate.sh [OPTIONS] <source-version> <target-version> <project-path>
```

**Options**:
- `--dry-run` - Preview changes without applying
- `--backup` - Create backup before migrating  
- `--skip-tests` - Skip test execution
- `--verbose` - Show detailed output

**Examples**:
```bash
# Preview migration
./migrate.sh --dry-run 0.1.0 1.0.0 /path/to/project

# Migrate with backup
./migrate.sh --backup 1.0.0 2.0.0 /path/to/project

# Full migration
./migrate.sh 1.0.0 2.0.0 /path/to/project
```

### 2. validate-pre-migration.sh

Validates that a project is ready for migration.

**Usage**:
```bash
./validate-pre-migration.sh <project-path> <target-version>
```

**Checks**:
- ✓ Project structure
- ✓ Git repository status
- ✓ Build tool configuration
- ✓ Java version compatibility
- ✓ Current build status
- ✓ Deprecated API usage
- ✓ Disk space

**Example**:
```bash
./validate-pre-migration.sh /path/to/project 2.0.0
```

### 3. detect-deprecations.sh

Scans codebase for usage of deprecated APIs.

**Usage**:
```bash
./detect-deprecations.sh <project-path> [--json]
```

**Output Formats**:
- **Text** (default): Human-readable report
- **JSON**: Machine-readable output for CI/CD

**Examples**:
```bash
# Text output
./detect-deprecations.sh /path/to/project

# JSON output for CI/CD
./detect-deprecations.sh /path/to/project --json | jq
```

## Migration Rules

Version-specific migration rules are stored in `migrations/*.rules`.

**Format**:
```
old_pattern|new_replacement
```

**Example** (`migrations/v0-to-v1.rules`):
```
\.getOrThrow\(\)|.getOrElse(null)
OldClassName|NewClassName
import com\.example\.|import com.marcusprado02.commons.
```

## Workflow

### Recommended Migration Process

1. **Validate readiness**:
   ```bash
   ./validate-pre-migration.sh /path/to/project 2.0.0
   ```

2. **Preview changes**:
   ```bash
   ./migrate.sh --dry-run 1.0.0 2.0.0 /path/to/project
   ```

3. **Create backup**:
   ```bash
   cd /path/to/project
   git checkout -b backup-before-v2
   git push origin backup-before-v2
   ```

4. **Run migration**:
   ```bash
   ./migrate.sh --backup 1.0.0 2.0.0 /path/to/project
   ```

5. **Review changes**:
   ```bash
   cd /path/to/project
   git diff
   ```

6. **Test thoroughly**:
   ```bash
   mvn clean verify
   # Run integration tests
   # Manual testing
   ```

7. **Commit**:
   ```bash
   git add .
   git commit -m "chore: migrate from v1.0.0 to v2.0.0"
   git push
   ```

### Rollback

If migration fails:

```bash
cd /path/to/project
git checkout backup-before-v2
git checkout -B main
git push origin main --force
```

Or restore from file backup:

```bash
rm -rf /path/to/project
cp -r /path/to/project_backup_* /path/to/project
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Validate Migration

on:
  pull_request:
    branches: [main]

jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Check for deprecated APIs
        run: |
          curl -sSL https://raw.githubusercontent.com/marcusPrado02/java-commons/main/scripts/detect-deprecations.sh | bash -s . --json
```

### Pre-commit Hook

Add to `.git/hooks/pre-commit`:

```bash
#!/bin/bash
# Check for deprecated API usage before commit

./scripts/detect-deprecations.sh . >/dev/null 2>&1

if [ $? -ne 0 ]; then
    echo "⚠️  Deprecated API usage detected"
    echo "Run: ./scripts/detect-deprecations.sh ."
    echo ""
    echo "Fix or commit with --no-verify to bypass"
    exit 1
fi
```

## Creating Custom Migration Rules

### 1. Create rules file

```bash
cat > migrations/v1-to-v2.rules << 'EOF'
OldClass|NewClass
oldMethod|newMethod
import old\.package\.|import new.package.
EOF
```

### 2. Test rules

```bash
./migrate.sh --dry-run 1.0.0 2.0.0 /path/to/test/project
```

### 3. Document in migration guide

Update `docs/migrations/v1-to-v2.md` with manual steps not covered by automation.

## Extending Scripts

### Adding New Checks

Edit `validate-pre-migration.sh`:

```bash
# Check 8: Custom validation
if [ -f "$PROJECT_PATH/custom-config.yml" ]; then
    check_passed "Custom configuration found"
else
    check_warning "Custom configuration missing"
fi
```

### Adding Migration Steps

Edit `migrate.sh`:

```bash
# Custom migration function
migrate_custom() {
    print_info "Running custom migration..."
    
    # Your custom logic here
    
    print_success "Custom migration completed"
}

# Add to main() function
main() {
    # ... existing steps ...
    migrate_custom  # Add new step
    # ... remaining steps ...
}
```

## Troubleshooting

### Script fails with "permission denied"

```bash
chmod +x scripts/*.sh
```

### Migration rules not applied

- Check rules file exists: `ls migrations/*.rules`
- Verify file format (no trailing spaces)
- Use `--verbose` flag for details

### Tests fail after migration

1. Review test changes in migration guide
2. Update test assertions
3. Check for new test utilities
4. Review CHANGELOG for test-related changes

## Support

- **Documentation**: [Migration Guides](../docs/migrations/)
- **Issues**: [GitHub Issues](https://github.com/marcusPrado02/java-commons/issues)
- **Discussions**: [GitHub Discussions](https://github.com/marcusPrado02/java-commons/discussions)

## License

Same as the parent project - see [LICENSE](../LICENSE)

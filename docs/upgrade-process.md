# Upgrade Process Guide

Complete guide for upgrading projects using the Commons library between versions.

## Table of Contents

- [Overview](#overview)
- [Before You Start](#before-you-start)
- [Upgrade Strategies](#upgrade-strategies)
- [Step-by-Step Process](#step-by-step-process)
- [Version-Specific Guides](#version-specific-guides)
- [Troubleshooting](#troubleshooting)
- [Best Practices](#best-practices)

---

## Overview

### Upgrade Types

| Type | Description | Complexity | Downtime |
|------|-------------|------------|----------|
| **Patch** (0.1.0 → 0.1.1) | Bug fixes only | 🟢 Low | None |
| **Minor** (0.1.0 → 0.2.0) | New features, backward compatible | 🟡 Medium | None |
| **Major** (0.1.0 → 1.0.0) | Breaking changes | 🔴 High | Possible |

### When to Upgrade

✅ **Upgrade when**:
- Security vulnerabilities are fixed
- Critical bugs are patched
- New features solve your problems
- Old version approaching end-of-support

⚠️ **Consider delaying when**:
- In feature freeze period
- Near major release deadline
- Breaking changes require extensive refactoring
- Team lacks capacity for thorough testing

---

## Before You Start

### Prerequisites Checklist

- [ ] **Review release notes**: Read CHANGELOG and migration guide
- [ ] **Current version stable**: All tests passing, no known issues
- [ ] **Code committed**: Clean git status, no uncommitted changes
- [ ] **Backup created**: Branch or file system backup
- [ ] **Team notified**: Communicate upgrade plan and timeline
- [ ] **Allocate time**: Reserve time for testing and fixes
- [ ] **Java version**: Ensure compatible JDK installed

### Risk Assessment

Evaluate the risk level:

```
Risk = (Breaking Changes Count × 10) + (Deprecated APIs Used × 5) + (Custom Extensions × 3)

< 20  = Low Risk (simple upgrade)
20-50 = Medium Risk (allocate 1-2 days)
> 50  = High Risk (allocate > 2 days, consider staged rollout)
```

### Preparation

1. **Create feature branch**:
   ```bash
   git checkout -b upgrade/commons-v2.0.0
   ```

2. **Document current state**:
   ```bash
   mvn dependency:tree > dependency-tree-before.txt
   mvn test > test-results-before.txt
   ```

3. **Identify deprecated APIs**:
   ```bash
   ./scripts/detect-deprecations.sh . > deprecations-report.txt
   ```

---

## Upgrade Strategies

### Strategy 1: All-at-Once (Recommended for Minor/Patch)

**When to use**: Patch or minor version upgrades with few/no breaking changes.

**Process**:
1. Update all dependency versions
2. Fix compilation errors
3. Update tests
4. Deploy

**Pros**: Fast, simple  
**Cons**: Higher risk for major upgrades

---

### Strategy 2: Incremental (Recommended for Major)

**When to use**: Major version upgrades with breaking changes.

**Process**:
1. Update dependencies incrementally (module by module)
2. Fix compilation and tests after each module
3. Deploy to staging after each module
4. Full testing before production

**Pros**: Lower risk, easier to debug  
**Cons**: Slower, more complex

---

### Strategy 3: Parallel (For Critical Systems)

**When to use**: Production systems that cannot have downtime.

**Process**:
1. Create parallel environment with new version
2. Dual-run (old + new) with traffic comparison
3. Gradually shift traffic to new version
4. Decommission old version

**Pros**: Zero downtime, easy rollback  
**Cons**: Resource intensive, complex setup

---

## Step-by-Step Process

### Phase 1: Preparation (30 min - 2 hours)

#### 1.1 Read Documentation

```bash
# Download and review migration guide
curl -o migration-guide.md \
  https://raw.githubusercontent.com/marcusPrado02/java-commons/main/docs/migrations/vX-to-vY.md

# Review CHANGELOG
less CHANGELOG.md
```

#### 1.2 Run Pre-Migration Validation

```bash
./scripts/validate-pre-migration.sh /path/to/project 2.0.0
```

Fix any issues reported before proceeding.

#### 1.3 Create Backup

```bash
# Git backup
git checkout -b backup-before-v2
git push origin backup-before-v2

# File system backup (optional)
tar -czf ../project-backup-$(date +%Y%m%d).tar.gz .
```

---

### Phase 2: Dependency Update (15 min - 1 hour)

#### 2.1 Update pom.xml

**Manual**:
```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-bom</artifactId>
    <version>2.0.0</version>  <!-- Updated -->
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

**Automated**:
```bash
./scripts/migrate.sh --dry-run 1.0.0 2.0.0 .
./scripts/migrate.sh 1.0.0 2.0.0 .
```

#### 2.2 Update Dependency Tree

```bash
mvn dependency:tree > dependency-tree-after.txt
diff dependency-tree-before.txt dependency-tree-after.txt
```

Review changes to ensure expected dependencies are updated.

---

### Phase 3: Code Migration (2-8 hours)

#### 3.1 Fix Compilation Errors

```bash
mvn clean compile 2>&1 | tee compile-errors.log
```

Work through errors systematically:

1. **Import errors**: Update package names
   ```bash
   # Find and replace
   find src -name "*.java" -exec sed -i 's/old.package/new.package/g' {} \;
   ```

2. **Method signature errors**: Update method calls
   ```java
   // Before
   result.getOrThrow()
   
   // After
   result.getOrElse(null)
   ```

3. **Type errors**: Update types
   ```java
   // Before
   Optional<User> user = repo.findById(id);
   
   // After
   Result<User> user = repo.findById(id);
   ```

#### 3.2 Apply Automated Fixes

```bash
# Run automated migration
./scripts/migrate.sh 1.0.0 2.0.0 .

# Review changes
git diff
```

#### 3.3 Manual Code Updates

Refer to migration guide for manual changes:
- [ ] Update custom implementations
- [ ] Replace deprecated methods
- [ ] Update configuration files
- [ ] Modify business logic if needed

---

### Phase 4: Testing (2-4 hours)

#### 4.1 Unit Tests

```bash
mvn test
```

Fix failing tests:
- Update assertions
- Update mocks/stubs
- Update test data
- Update test utilities

#### 4.2 Integration Tests

```bash
mvn verify -P integration-tests
```

#### 4.3 Performance Tests

```bash
# Run performance benchmarks
mvn test -P performance-tests

# Compare with baseline
diff perf-baseline.txt perf-current.txt
```

#### 4.4 Manual Testing

Test critical user flows:
- [ ] Core business flows
- [ ] Error handling
- [ ] Authentication/Authorization
- [ ] Data persistence
- [ ] External integrations

---

### Phase 5: Deployment (1-2 hours)

#### 5.1 Deploy to Staging

```bash
mvn clean package
# Deploy to staging environment
```

#### 5.2 Smoke Tests

Run smoke tests in staging:
- [ ] Application starts successfully
- [ ] Health checks pass
- [ ] Critical endpoints respond
- [ ] Database connectivity works
- [ ] External services accessible

#### 5.3 Staging Validation

Full regression testing in staging:
- [ ] All automated tests pass
- [ ] Manual test scenarios pass
- [ ] Performance acceptable
- [ ] No error spikes in logs

#### 5.4 Production Deployment

```bash
# Tag release
git tag -a v2.0.0-upgrade -m "Upgraded to Commons v2.0.0"

# Deploy to production
mvn clean package -P production
# Deploy via your deployment process
```

#### 5.5 Post-Deployment Monitoring

Monitor for 24-48 hours:
- [ ] Error rates normal
- [ ] Response times acceptable
- [ ] No memory leaks
- [ ] No unusual logs

---

### Phase 6: Cleanup (30 min)

#### 6.1 Remove Deprecated Code

```bash
# Remove backup branches (after confirmation)
git branch -D backup-before-v2
git push origin --delete backup-before-v2
```

#### 6.2 Update Documentation

- [ ] Update README with new version
- [ ] Update deployment docs
- [ ] Update API documentation
- [ ] Archive migration notes

#### 6.3 Team Communication

- [ ] Notify team of successful upgrade
- [ ] Share lessons learned
- [ ] Update runbooks if needed

---

## Version-Specific Guides

Detailed migration guides for each major version:

- [v0 to v1](migrations/v0-to-v1.md)
- [v1 to v2](migrations/v1-to-v2.md)
- [v2 to v3](migrations/v2-to-v3.md) *(when available)*

---

## Troubleshooting

### Common Issues

#### Issue: "NoClassDefFoundError"

**Cause**: Dependency not updated or missing  
**Solution**:
```bash
mvn dependency:tree | grep commons
mvn clean install -U  # Force update
```

#### Issue: "Method not found"

**Cause**: Breaking change in API  
**Solution**: Consult migration guide for method replacement

#### Issue: Tests fail after upgrade

**Cause**: Test utilities changed  
**Solution**: Update test dependencies and assertions

#### Issue: Performance degradation

**Cause**: New version behavior or configuration  
**Solution**:
1. Review performance-related changes in CHANGELOG
2. Check configuration changes
3. Profile application
4. Report issue if unexpected

### Rollback

If upgrade fails, rollback immediately:

```bash
# Restore from git
git checkout backup-before-v2
git checkout -B main
git push origin main --force

# Redeploy old version
mvn clean package
# Deploy
```

Document the failure:
1. What went wrong
2. Error messages
3. Steps taken
4. Why rollback was necessary

Plan remediation:
1. Fix root cause
2. Test fix in isolation
3. Retry upgrade

---

## Best Practices

### Do's ✅

- ✅ **Read release notes thoroughly**
- ✅ **Test in non-production first**
- ✅ **Create backups before upgrading**
- ✅ **Upgrade dependencies regularly** (don't skip versions)
- ✅ **Automate testing**
- ✅ **Monitor after deployment**
- ✅ **Document issues and solutions**
- ✅ **Communicate with team**

### Don'ts ❌

- ❌ **Skip testing**
- ❌ **Upgrade during peak hours**
- ❌ **Upgrade multiple major versions at once**
- ❌ **Ignore deprecation warnings**
- ❌ **Rush the process**
- ❌ **Deploy Friday afternoons**
- ❌ **Skipdocumentation**

### Tips for Success

1. **Start early**: Begin upgrade process well before deadline

2. **Test thoroughly**: Invest time in comprehensive testing

3. **Communicate**: Keep stakeholders informed

4. **Automate**: Use provided scripts and tools

5. **Learn**: Document lessons for future upgrades

6. **Stay current**: Don't fall multiple versions behind

---

## Additional Resources

- [Semantic Versioning](https://semver.org/)
- [Versioning and Releases](versioning-and-releases.md)
- [Migration Scripts](../scripts/README.md)
- [CHANGELOG](../CHANGELOG.md)

---

**Last Updated**: 2026-02-14

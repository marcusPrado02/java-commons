# GitHub Labels Configuration

This document describes the labels used by Dependabot and other automation workflows.

## Dependabot Labels

### Dependency Labels

| Label | Color | Description | Auto-Applied |
|-------|-------|-------------|--------------|
| `dependencies` | `0366d6` | Pull requests that update a dependency file | ✅ All Dependabot PRs |
| `java` | `b07219` | Java/Maven dependency updates | ✅ Maven ecosystem |
| `maven` | `b07219` | Maven-specific updates | ✅ Maven ecosystem |
| `github-actions` | `000000` | GitHub Actions workflow updates | ✅ Actions ecosystem |
| `ci-cd` | `0e8a16` | CI/CD pipeline changes | ✅ Actions ecosystem |

### Auto-Merge Labels

| Label | Color | Description | Auto-Applied |
|-------|-------|-------------|--------------|
| `auto-merge` | `1d76db` | PR will be automatically merged after CI passes | ✅ Patch updates |

### Security Labels

| Label | Color | Description | Auto-Applied |
|-------|-------|-------------|--------------|
| `security` | `d93f0b` | Security vulnerability fix | ✅ Security updates |
| `priority:high` | `d93f0b` | High priority PR - requires immediate attention | ✅ Security updates |

## Creating Labels via GitHub CLI

```bash
# Dependency labels
gh label create "dependencies" --color "0366d6" --description "Pull requests that update a dependency file"
gh label create "java" --color "b07219" --description "Java/Maven dependency updates"
gh label create "maven" --color "b07219" --description "Maven-specific updates"
gh label create "github-actions" --color "000000" --description "GitHub Actions workflow updates"
gh label create "ci-cd" --color "0e8a16" --description "CI/CD pipeline changes"

# Auto-merge labels
gh label create "auto-merge" --color "1d76db" --description "PR will be automatically merged after CI passes"

# Security labels
gh label create "security" --color "d93f0b" --description "Security vulnerability fix"
gh label create "priority:high" --color "d93f0b" --description "High priority PR - requires immediate attention"
```

## Creating Labels via GitHub Web UI

1. Navigate to: `https://github.com/marcusPrado02/java-commons/labels`
2. Click **New label**
3. Enter:
   - **Label name**: From table above
   - **Description**: From table above
   - **Color**: Hex code from table above
4. Click **Create label**
5. Repeat for all labels

## Label Usage

### Automatic (Dependabot)
- Dependabot applies labels automatically based on `.github/dependabot.yml` configuration
- `dependabot-auto-merge.yml` workflow adds `auto-merge` and security labels

### Manual (Developers)
Labels can be manually added to PRs when needed:
```bash
gh pr edit <PR_NUMBER> --add-label "priority:high"
```

## Filtering PRs by Label

```bash
# View all Dependabot PRs
gh pr list --label "dependencies"

# View auto-merge candidates
gh pr list --label "auto-merge"

# View security updates
gh pr list --label "security"

# View Java updates
gh pr list --label "java,maven"
```

## Best Practices

1. **Don't remove auto-applied labels** - They're used by automation
2. **Add priority labels manually** when urgent review is needed
3. **Use labels for filtering** in GitHub UI and CLI
4. **Keep label descriptions updated** for clarity
5. **Follow color scheme** for visual grouping

## Colors Reference

- `0366d6` - Blue (Dependencies)
- `b07219` - Brown (Java/Maven)
- `000000` - Black (GitHub Actions)
- `0e8a16` - Green (CI/CD)
- `1d76db` - Dark Blue (Auto-merge)
- `d93f0b` - Red (Security/Priority)

---

**Note**: Labels are automatically applied by Dependabot and GitHub Actions workflows. Manual creation is only needed once per repository setup.

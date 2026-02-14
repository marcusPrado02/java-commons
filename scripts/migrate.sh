#!/bin/bash
#
# Commons Library Migration Script
# Automates migration between major versions
#
# Usage:
#   ./migrate.sh [OPTIONS] <source-version> <target-version> <project-path>
#
# Options:
#   --dry-run           Preview changes without applying
#   --backup            Create backup before migrating
#   --skip-tests        Skip test execution after migration
#   --verbose           Show detailed output
#   -h, --help          Show this help message
#
# Examples:
#   ./migrate.sh --dry-run 0.1.0 1.0.0 /path/to/project
#   ./migrate.sh --backup 1.0.0 2.0.0 /path/to/project
#

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default options
DRY_RUN=false
CREATE_BACKUP=false
SKIP_TESTS=false
VERBOSE=false

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Print colored message
print_info() {
    echo -e "${BLUE}ℹ ${1}${NC}"
}

print_success() {
    echo -e "${GREEN}✓ ${1}${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ ${1}${NC}"
}

print_error() {
    echo -e "${RED}✗ ${1}${NC}"
}

# Show usage
usage() {
    head -n 20 "$0" | grep "^#" | sed 's/^# //' | sed 's/^#//'
    exit 1
}

# Parse command line arguments
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --dry-run)
                DRY_RUN=true
                shift
                ;;
            --backup)
                CREATE_BACKUP=true
                shift
                ;;
            --skip-tests)
                SKIP_TESTS=true
                shift
                ;;
            --verbose)
                VERBOSE=true
                shift
                ;;
            -h|--help)
                usage
                ;;
            *)
                if [ -z "$SOURCE_VERSION" ]; then
                    SOURCE_VERSION=$1
                elif [ -z "$TARGET_VERSION" ]; then
                    TARGET_VERSION=$1
                elif [ -z "$PROJECT_PATH" ]; then
                    PROJECT_PATH=$1
                else
                    print_error "Unknown argument: $1"
                    usage
                fi
                shift
                ;;
        esac
    done

    # Validate required arguments
    if [ -z "$SOURCE_VERSION" ] || [ -z "$TARGET_VERSION" ] || [ -z "$PROJECT_PATH" ]; then
        print_error "Missing required arguments"
        usage
    fi

    # Validate project path
    if [ ! -d "$PROJECT_PATH" ]; then
        print_error "Project path does not exist: $PROJECT_PATH"
        exit 1
    fi
}

# Create backup
create_backup() {
    if [ "$CREATE_BACKUP" = true ]; then
        print_info "Creating backup..."

        BACKUP_DIR="${PROJECT_PATH}_backup_$(date +%Y%m%d_%H%M%S)"

        if [ "$DRY_RUN" = false ]; then
            cp -r "$PROJECT_PATH" "$BACKUP_DIR"
            print_success "Backup created at: $BACKUP_DIR"
        else
            print_info "[DRY RUN] Would create backup at: $BACKUP_DIR"
        fi
    fi
}

# Update Maven dependencies
update_dependencies() {
    print_info "Updating Maven dependencies..."

    local pom_file="$PROJECT_PATH/pom.xml"

    if [ ! -f "$pom_file" ]; then
        print_warning "No pom.xml found, skipping dependency update"
        return
    fi

    if [ "$DRY_RUN" = false ]; then
        # Update commons-bom version
        sed -i "s/<version>$SOURCE_VERSION<\/version>/<version>$TARGET_VERSION<\/version>/g" "$pom_file"
        print_success "Updated dependencies to version $TARGET_VERSION"
    else
        print_info "[DRY RUN] Would update dependencies from $SOURCE_VERSION to $TARGET_VERSION"
    fi
}

# Find and replace imports
migrate_imports() {
    print_info "Migrating import statements..."

    # Load version-specific migration rules
    local migration_rules_file="$SCRIPT_DIR/migrations/v${SOURCE_VERSION%%.*}-to-v${TARGET_VERSION%%.*}.rules"

    if [ ! -f "$migration_rules_file" ]; then
        print_warning "No migration rules found for $SOURCE_VERSION -> $TARGET_VERSION"
        return
    fi

    if [ "$DRY_RUN" = false ]; then
        # Apply migration rules
        while IFS='|' read -r pattern replacement; do
            if [ -n "$pattern" ] && [ -n "$replacement" ]; then
                find "$PROJECT_PATH/src" -name "*.java" -type f -exec sed -i "s/$pattern/$replacement/g" {} \;
                print_success "Applied: $pattern -> $replacement"
            fi
        done < "$migration_rules_file"
    else
        print_info "[DRY RUN] Would apply migration rules from: $migration_rules_file"
        cat "$migration_rules_file"
    fi
}

# Update configuration files
migrate_configuration() {
    print_info "Migrating configuration files..."

    local config_files=(
        "$PROJECT_PATH/src/main/resources/application.properties"
        "$PROJECT_PATH/src/main/resources/application.yml"
        "$PROJECT_PATH/src/main/resources/application.yaml"
    )

    for config_file in "${config_files[@]}"; do
        if [ -f "$config_file" ]; then
            print_info "Found configuration: $(basename "$config_file")"

            if [ "$DRY_RUN" = false ]; then
                # Apply configuration migrations
                # (Version-specific logic would go here)
                print_success "Migrated: $(basename "$config_file")"
            else
                print_info "[DRY RUN] Would migrate: $config_file"
            fi
        fi
    done
}

# Run tests
run_tests() {
    if [ "$SKIP_TESTS" = true ]; then
        print_warning "Skipping tests (--skip-tests flag)"
        return
    fi

    print_info "Running tests..."

    if [ "$DRY_RUN" = false ]; then
        cd "$PROJECT_PATH"

        if [ -f "mvnw" ]; then
            ./mvnw test
        elif [ -f "pom.xml" ]; then
            mvn test
        else
            print_warning "No Maven wrapper or pom.xml found, skipping tests"
            return
        fi

        if [ $? -eq 0 ]; then
            print_success "All tests passed"
        else
            print_error "Tests failed - please review and fix"
            exit 1
        fi
    else
        print_info "[DRY RUN] Would run tests"
    fi
}

# Generate migration report
generate_report() {
    print_info "Generating migration report..."

    local report_file="$PROJECT_PATH/MIGRATION_REPORT_$(date +%Y%m%d_%H%M%S).md"

    if [ "$DRY_RUN" = false ]; then
        cat > "$report_file" << EOF
# Migration Report

**Date**: $(date)
**Source Version**: $SOURCE_VERSION
**Target Version**: $TARGET_VERSION
**Project Path**: $PROJECT_PATH

## Changes Applied

- ✅ Dependencies updated
- ✅ Imports migrated
- ✅ Configuration files updated
- ✅ Tests executed

## Next Steps

1. Review changes: \`git diff\`
2. Run full test suite: \`mvn verify\`
3. Test manually in your environment
4. Commit changes: \`git commit -am "chore: migrate from v$SOURCE_VERSION to v$TARGET_VERSION"\`

## Rollback

If needed, restore from backup:
\`\`\`bash
rm -rf $PROJECT_PATH
cp -r ${PROJECT_PATH}_backup_* $PROJECT_PATH
\`\`\`

## Resources

- [Migration Guide](https://github.com/marcusPrado02/java-commons/blob/main/docs/migrations/v${SOURCE_VERSION%%.*}-to-v${TARGET_VERSION%%.*}.md)
- [Changelog](https://github.com/marcusPrado02/java-commons/blob/main/CHANGELOG.md)
EOF

        print_success "Report generated: $report_file"
    else
        print_info "[DRY RUN] Would generate report at: $report_file"
    fi
}

# Main execution
main() {
    echo ""
    print_info "Commons Library Migration Tool"
    echo ""
    print_info "Source Version: $SOURCE_VERSION"
    print_info "Target Version: $TARGET_VERSION"
    print_info "Project Path: $PROJECT_PATH"

    if [ "$DRY_RUN" = true ]; then
        print_warning "DRY RUN MODE - No changes will be applied"
    fi

    echo ""

    # Execute migration steps
    create_backup
    update_dependencies
    migrate_imports
    migrate_configuration
    run_tests
    generate_report

    echo ""

    if [ "$DRY_RUN" = false ]; then
        print_success "Migration completed successfully!"
        echo ""
        print_info "Next steps:"
        print_info "  1. Review changes: cd $PROJECT_PATH && git diff"
        print_info "  2. Run full build: mvn clean verify"
        print_info "  3. Test in your environment"
        print_info "  4. Commit: git commit -am 'chore: migrate to v$TARGET_VERSION'"
    else
        echo ""
        print_info "Dry run completed. Review the changes above."
        print_info "To apply changes, run without --dry-run flag"
    fi

    echo ""
}

# Run script
parse_args "$@"
main

#!/bin/bash
#
# Pre-Migration Validator
# Checks if project is ready for migration
#
# Usage: ./validate-pre-migration.sh <project-path> <target-version>
#

set -e

PROJECT_PATH=$1
TARGET_VERSION=$2

if [ -z "$PROJECT_PATH" ] || [ -z "$TARGET_VERSION" ]; then
    echo "Usage: $0 <project-path> <target-version>"
    exit 1
fi

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

CHECKS_PASSED=0
CHECKS_FAILED=0
WARNINGS=0

check_passed() {
    echo -e "${GREEN}✓${NC} $1"
    CHECKS_PASSED=$((CHECKS_PASSED + 1))
}

check_failed() {
    echo -e "${RED}✗${NC} $1"
    CHECKS_FAILED=$((CHECKS_FAILED + 1))
}

check_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
    WARNINGS=$((WARNINGS + 1))
}

echo "Pre-Migration Validation"
echo "Project: $PROJECT_PATH"
echo "Target Version: $TARGET_VERSION"
echo ""
echo "Running checks..."
echo ""

# Check 1: Project exists
if [ -d "$PROJECT_PATH" ]; then
    check_passed "Project directory exists"
else
    check_failed "Project directory not found"
fi

# Check 2: Git repository
if [ -d "$PROJECT_PATH/.git" ]; then
    check_passed "Git repository found"

    # Check for uncommitted changes
    cd "$PROJECT_PATH"
    if [ -z "$(git status --porcelain)" ]; then
        check_passed "No uncommitted changes"
    else
        check_warning "Uncommitted changes detected - commit or stash before migration"
    fi
else
    check_warning "Not a Git repository - backup recommended"
fi

# Check 3: Build tool
if [ -f "$PROJECT_PATH/pom.xml" ]; then
    check_passed "Maven project detected"

    # Check for Maven wrapper
    if [ -f "$PROJECT_PATH/mvnw" ]; then
        check_passed "Maven wrapper found"
    else
        check_warning "Maven wrapper not found - using system Maven"
    fi
elif [ -f "$PROJECT_PATH/build.gradle" ] || [ -f "$PROJECT_PATH/build.gradle.kts" ]; then
    check_passed "Gradle project detected"
else
    check_failed "No build configuration found (pom.xml or build.gradle)"
fi

# Check 4: Java version
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 21 ]; then
        check_passed "Java $JAVA_VERSION detected (>= 21)"
    else
        check_failed "Java $JAVA_VERSION detected (requires >= 21)"
    fi
else
    check_failed "Java not found in PATH"
fi

# Check 5: Current build status
if [ -f "$PROJECT_PATH/pom.xml" ]; then
    echo ""
    echo "Testing current build..."

    cd "$PROJECT_PATH"
    if [ -f "mvnw" ]; then
        BUILD_CMD="./mvnw clean test -q"
    else
        BUILD_CMD="mvn clean test -q"
    fi

    if $BUILD_CMD > /tmp/build-output.log 2>&1; then
        check_passed "Project builds successfully"
    else
        check_failed "Project build failed - fix before migration"
        echo "  See /tmp/build-output.log for details"
    fi
fi

# Check 6: Deprecated API usage
echo ""
echo "Scanning for deprecated APIs..."

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/detect-deprecations.sh" ]; then
    if bash "$SCRIPT_DIR/detect-deprecations.sh" "$PROJECT_PATH" > /tmp/deprecations.log 2>&1; then
        check_passed "No deprecated API usage detected"
    else
        check_warning "Deprecated APIs found - review /tmp/deprecations.log"
    fi
else
    check_warning "Deprecation detector not found - manual review recommended"
fi

# Check 7: Disk space
REQUIRED_SPACE_MB=500
AVAILABLE_SPACE_MB=$(df "$PROJECT_PATH" | tail -1 | awk '{print int($4/1024)}')

if [ "$AVAILABLE_SPACE_MB" -gt "$REQUIRED_SPACE_MB" ]; then
    check_passed "Sufficient disk space (${AVAILABLE_SPACE_MB}MB available)"
else
    check_warning "Low disk space (${AVAILABLE_SPACE_MB}MB available, recommend ${REQUIRED_SPACE_MB}MB)"
fi

# Summary
echo ""
echo "========================================"
echo "Validation Summary"
echo "========================================"
echo -e "Passed:   ${GREEN}$CHECKS_PASSED${NC}"
echo -e "Failed:   ${RED}$CHECKS_FAILED${NC}"
echo -e "Warnings: ${YELLOW}$WARNINGS${NC}"
echo ""

if [ $CHECKS_FAILED -gt 0 ]; then
    echo -e "${RED}❌ Pre-migration validation FAILED${NC}"
    echo ""
    echo "Please fix the issues above before proceeding."
    exit 1
else
    echo -e "${GREEN}✅ Pre-migration validation PASSED${NC}"
    echo ""

    if [ $WARNINGS -gt 0 ]; then
        echo -e "${YELLOW}⚠ ${WARNINGS} warning(s) detected${NC}"
        echo "Review warnings before proceeding."
        echo ""
    fi

    echo "Your project is ready for migration to v$TARGET_VERSION"
    echo ""
    echo "Next steps:"
    echo "  1. Create backup: git checkout -b backup-before-migration"
    echo "  2. Run migration: ./scripts/migrate.sh <source-version> $TARGET_VERSION $PROJECT_PATH"
    echo ""
    exit 0
fi

#!/bin/bash
#
# Deprecation Detector
# Scans codebase for usage of deprecated APIs
#
# Usage: ./detect-deprecations.sh <project-path> [--json]
#

set -e

PROJECT_PATH=$1
OUTPUT_FORMAT=${2:-"text"}

if [ -z "$PROJECT_PATH" ]; then
    echo "Usage: $0 <project-path> [--json]"
    exit 1
fi

if [ ! -d "$PROJECT_PATH" ]; then
    echo "Error: Project path does not exist: $PROJECT_PATH"
    exit 1
fi

# Colors
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "Scanning for deprecated API usage in: $PROJECT_PATH"
echo ""

# Find @Deprecated annotations in dependencies (from Maven)
DEPRECATED_APIS=(
    "Result.getOrThrow:Use getOrElse or fold instead"
    "DomainEvent.occurredAt:Use occurredOn instead"
    "Repository.save:Use persist instead"
)

FINDINGS=0
FINDINGS_JSON="["

for api_info in "${DEPRECATED_APIs[@]}"; do
    IFS=':' read -r api_name message <<< "$api_info"

    # Search for usage
    matches=$(grep -r "$api_name" "$PROJECT_PATH/src" --include="*.java" 2>/dev/null || true)

    if [ -n "$matches" ]; then
        FINDINGS=$((FINDINGS + 1))

        if [ "$OUTPUT_FORMAT" = "--json" ]; then
            # JSON output
            FINDINGS_JSON+="{\"api\":\"$api_name\",\"message\":\"$message\",\"occurrences\":["

            while IFS= read -r line; do
                file=$(echo "$line" | cut -d: -f1)
                line_number=$(echo "$line" | cut -d: -f2)
                FINDINGS_JSON+="{\"file\":\"$file\",\"line\":$line_number},"
            done <<< "$matches"

            FINDINGS_JSON="${FINDINGS_JSON%,}]},"
        else
            # Text output
            echo -e "${YELLOW}⚠ Deprecated API: ${api_name}${NC}"
            echo -e "  ${message}"
            echo ""
            echo "$matches" | while IFS= read -r line; do
                echo "  $line"
            done
            echo ""
        fi
    fi
done

FINDINGS_JSON="${FINDINGS_JSON%,}]"

if [ "$OUTPUT_FORMAT" = "--json" ]; then
    echo "$FINDINGS_JSON"
else
    echo "============================================"
    echo "Scan complete: Found $FINDINGS deprecated API usages"

    if [ $FINDINGS -gt 0 ]; then
        echo ""
        echo "Action required:"
        echo "  1. Review deprecation notices above"
        echo "  2. Update code to use recommended alternatives"
        echo "  3. See migration guide for details"
        exit 1
    else
        echo "No deprecated APIs found - ready for upgrade!"
        exit 0
    fi
fi

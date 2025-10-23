#!/bin/bash

# SmilePile Validation Script
# Performs SmilePile-specific code quality and compliance checks
# Author: DevOps Agent
# Version: 1.0.0

# Color codes for output
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Error tracking
ERRORS=0
WARNINGS=0

# Get the script's directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}SmilePile Validation Script${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to print section header
print_section() {
    echo -e "\n${BLUE}[CHECK]${NC} $1"
    echo "----------------------------------------"
}

# Function to print error
print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
    ((ERRORS++))
}

# Function to print warning
print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
    ((WARNINGS++))
}

# Function to print success
print_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
}

# 1. Check for unrequested features
print_section "Checking for unrequested features..."

FORBIDDEN_FEATURES=("search" "filter" "sort" "favorite" "bookmark" "star")
FOUND_FORBIDDEN=false

for feature in "${FORBIDDEN_FEATURES[@]}"; do
    # Check iOS Swift files (excluding tests and comments)
    ios_matches=$(find "$PROJECT_ROOT/ios" -name "*.swift" -not -path "*/test*" -not -path "*Test*" -exec grep -l -i "\b$feature\b" {} \; 2>/dev/null)

    if [ ! -z "$ios_matches" ]; then
        for file in $ios_matches; do
            # Check if it's in a comment or string literal explaining NOT to do something
            context=$(grep -n -i "\b$feature\b" "$file" | grep -v "//" | grep -v "// DO NOT" | grep -v "// Don't" | grep -v "// Never" | head -3)
            if [ ! -z "$context" ]; then
                print_error "Found forbidden feature '$feature' in iOS file: ${file#$PROJECT_ROOT/}"
                echo "  Context: $context" | head -3
                FOUND_FORBIDDEN=true
            fi
        done
    fi

    # Check Android Kotlin files (excluding tests and comments)
    android_matches=$(find "$PROJECT_ROOT/android" -name "*.kt" -not -path "*/test*" -not -path "*Test*" -exec grep -l -i "\b$feature\b" {} \; 2>/dev/null)

    if [ ! -z "$android_matches" ]; then
        for file in $android_matches; do
            # Check if it's in a comment or string literal explaining NOT to do something
            context=$(grep -n -i "\b$feature\b" "$file" | grep -v "//" | grep -v "// DO NOT" | grep -v "// Don't" | grep -v "// Never" | head -3)
            if [ ! -z "$context" ]; then
                print_error "Found forbidden feature '$feature' in Android file: ${file#$PROJECT_ROOT/}"
                echo "  Context: $context" | head -3
                FOUND_FORBIDDEN=true
            fi
        done
    fi
done

if [ "$FOUND_FORBIDDEN" = false ]; then
    print_success "No unrequested features found"
fi

# 2. Check for emoji in code
print_section "Checking for emoji in code..."

EMOJI_FOUND=false

# Check iOS Swift files for emoji (excluding test files and UI strings)
ios_emoji=$(find "$PROJECT_ROOT/ios" -name "*.swift" -not -path "*/test*" -not -path "*Test*" -exec grep -l '[😀-🙏🌀-🗿🚀-🛿🤐-🤯🤰-🤿🥀-🥿🦀-🦿🧀-🧿🩀-🩿🪀-🪿🫀-🫿🬀-🬿🭀-🭿🮀-🮿🯀-🯿🰀-🰿]' {} \; 2>/dev/null)

if [ ! -z "$ios_emoji" ]; then
    for file in $ios_emoji; do
        print_error "Found emoji in iOS file: ${file#$PROJECT_ROOT/}"
        EMOJI_FOUND=true
    done
fi

# Check Android Kotlin files for emoji
android_emoji=$(find "$PROJECT_ROOT/android" -name "*.kt" -not -path "*/test*" -not -path "*Test*" -exec grep -l '[😀-🙏🌀-🗿🚀-🛿🤐-🤯🤰-🤿🥀-🥿🦀-🦿🧀-🧿🩀-🩿🪀-🪿🫀-🫿🬀-🬿🭀-🭿🮀-🮿🯀-🯿🰀-🰿]' {} \; 2>/dev/null)

if [ ! -z "$android_emoji" ]; then
    for file in $android_emoji; do
        print_error "Found emoji in Android file: ${file#$PROJECT_ROOT/}"
        EMOJI_FOUND=true
    done
fi

# Check recent commit messages for emoji
recent_commits=$(git log --oneline -10 2>/dev/null | grep -E '[😀-🙏🌀-🗿🚀-🛿🤐-🤯🤰-🤿🥀-🥿🦀-🦿🧀-🧿🩀-🩿🪀-🪿🫀-🫿🬀-🬿🭀-🭿🮀-🮿🯀-🯿🰀-🰿]')
if [ ! -z "$recent_commits" ]; then
    print_warning "Found emoji in recent commit messages:"
    echo "$recent_commits"
    EMOJI_FOUND=true
fi

if [ "$EMOJI_FOUND" = false ]; then
    print_success "No emoji found in code or commits"
fi

# 3. Check Photo ID patterns
print_section "Checking Photo ID patterns..."

PHOTO_ID_ISSUES=false

# Check iOS for PHAsset.localIdentifier usage
ios_photo_correct=$(find "$PROJECT_ROOT/ios" -name "*.swift" -exec grep -l "PHAsset\.localIdentifier" {} \; 2>/dev/null | wc -l)
ios_photo_incorrect=$(find "$PROJECT_ROOT/ios" -name "*.swift" -exec grep -l "photo.*id\|image.*id" {} \; 2>/dev/null | grep -v "PHAsset.localIdentifier" | wc -l)

if [ "$ios_photo_correct" -eq 0 ] && [ "$ios_photo_incorrect" -gt 0 ]; then
    print_warning "iOS: Found photo ID usage but not using PHAsset.localIdentifier pattern"
    PHOTO_ID_ISSUES=true
fi

# Check Android for Uri.toString usage
android_photo_correct=$(find "$PROJECT_ROOT/android" -name "*.kt" -exec grep -l "Uri\.toString()" {} \; 2>/dev/null | wc -l)
android_photo_incorrect=$(find "$PROJECT_ROOT/android" -name "*.kt" -exec grep -l "photo.*id\|image.*id" {} \; 2>/dev/null | grep -v "Uri.toString" | wc -l)

if [ "$android_photo_correct" -eq 0 ] && [ "$android_photo_incorrect" -gt 0 ]; then
    print_warning "Android: Found photo ID usage but not using Uri.toString() pattern"
    PHOTO_ID_ISSUES=true
fi

if [ "$PHOTO_ID_ISSUES" = false ]; then
    print_success "Photo ID patterns are correct"
fi

# 4. Check for direct database mutations
print_section "Checking for direct database mutations..."

DB_ISSUES=false

# iOS: Check for direct CoreData context.save() outside managers
ios_context_saves=$(find "$PROJECT_ROOT/ios" -name "*.swift" -not -path "*Manager*" -not -path "*Repository*" -exec grep -l "context\.save()" {} \; 2>/dev/null)

if [ ! -z "$ios_context_saves" ]; then
    for file in $ios_context_saves; do
        print_error "iOS: Direct CoreData save found outside manager: ${file#$PROJECT_ROOT/}"
        DB_ISSUES=true
    done
fi

# Android: Check for direct DAO calls outside repositories
android_dao_calls=$(find "$PROJECT_ROOT/android" -name "*.kt" -not -path "*Repository*" -not -path "*Dao*" -not -path "*Database*" -exec grep -l "@Dao\|dao\." {} \; 2>/dev/null)

if [ ! -z "$android_dao_calls" ]; then
    for file in $android_dao_calls; do
        # Filter out legitimate uses (like dependency injection)
        suspicious=$(grep -n "dao\." "$file" | grep -v "@Inject" | grep -v "private val" | grep -v "constructor")
        if [ ! -z "$suspicious" ]; then
            print_error "Android: Direct DAO call found outside repository: ${file#$PROJECT_ROOT/}"
            DB_ISSUES=true
        fi
    done
fi

if [ "$DB_ISSUES" = false ]; then
    print_success "No direct database mutations found"
fi

# 5. Check security patterns
print_section "Checking security patterns..."

SECURITY_ISSUES=false

# Android: Check for SecureActivity inheritance in main activities
main_activities=$(find "$PROJECT_ROOT/android" -name "*Activity.kt" -not -path "*/test*" 2>/dev/null)

for activity in $main_activities; do
    activity_name=$(basename "$activity" .kt)
    # Skip SecureActivity itself
    if [ "$activity_name" != "SecureActivity" ]; then
        extends_secure=$(grep -l ": SecureActivity" "$activity" 2>/dev/null)
        if [ -z "$extends_secure" ]; then
            # Check if it's a legitimate exception (like base classes)
            is_base=$(grep -l "abstract class\|open class" "$activity" 2>/dev/null)
            if [ -z "$is_base" ]; then
                print_warning "Android: Activity doesn't extend SecureActivity: ${activity#$PROJECT_ROOT/}"
                SECURITY_ISSUES=true
            fi
        fi
    fi
done

# Check for SecurePreferencesManager usage for sensitive data
sensitive_prefs=$(find "$PROJECT_ROOT/android" -name "*.kt" -exec grep -l "SharedPreferences\|getSharedPreferences" {} \; 2>/dev/null | grep -v "SecurePreferencesManager")

if [ ! -z "$sensitive_prefs" ]; then
    for file in $sensitive_prefs; do
        # Check if it's storing sensitive data
        sensitive=$(grep -n "password\|token\|secret\|key\|credential" "$file" 2>/dev/null)
        if [ ! -z "$sensitive" ]; then
            print_warning "Android: Potential sensitive data stored without SecurePreferencesManager: ${file#$PROJECT_ROOT/}"
            SECURITY_ISSUES=true
        fi
    done
fi

if [ "$SECURITY_ISSUES" = false ]; then
    print_success "Security patterns are correctly implemented"
fi

# 6. Check naming conventions
print_section "Checking naming conventions..."

NAMING_ISSUES=false

# iOS: Check for ViewModel suffix
ios_viewmodels=$(find "$PROJECT_ROOT/ios" -name "*.swift" -exec grep -l "class.*:.*ObservableObject\|@StateObject\|@ObservedObject" {} \; 2>/dev/null)

for vm in $ios_viewmodels; do
    filename=$(basename "$vm" .swift)
    if [[ ! "$filename" =~ ViewModel$ ]] && [[ ! "$filename" =~ Manager$ ]] && [[ ! "$filename" =~ Coordinator$ ]]; then
        # Check if the class name inside has ViewModel suffix
        has_vm_suffix=$(grep -E "class.*ViewModel.*:.*ObservableObject" "$vm" 2>/dev/null)
        if [ -z "$has_vm_suffix" ]; then
            print_warning "iOS: Potential ViewModel without proper suffix: ${vm#$PROJECT_ROOT/}"
            NAMING_ISSUES=true
        fi
    fi
done

# Android: Check for ViewModel and Repository suffixes
android_viewmodels=$(find "$PROJECT_ROOT/android" -name "*.kt" -exec grep -l "ViewModel()\|AndroidViewModel" {} \; 2>/dev/null)

for vm in $android_viewmodels; do
    filename=$(basename "$vm" .kt)
    if [[ ! "$filename" =~ ViewModel$ ]]; then
        print_warning "Android: ViewModel without proper suffix: ${vm#$PROJECT_ROOT/}"
        NAMING_ISSUES=true
    fi
done

android_repositories=$(find "$PROJECT_ROOT/android" -name "*.kt" -exec grep -l "interface.*Repository\|class.*Repository" {} \; 2>/dev/null)

for repo in $android_repositories; do
    filename=$(basename "$repo" .kt)
    if [[ ! "$filename" =~ Repository$ ]] && [[ ! "$filename" =~ RepositoryImpl$ ]]; then
        print_warning "Android: Repository without proper suffix: ${repo#$PROJECT_ROOT/}"
        NAMING_ISSUES=true
    fi
done

if [ "$NAMING_ISSUES" = false ]; then
    print_success "Naming conventions are followed correctly"
fi

# Final summary
echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Validation Summary${NC}"
echo -e "${BLUE}========================================${NC}"

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✓ All checks passed successfully!${NC}"
    exit 0
else
    echo -e "${RED}Errors: $ERRORS${NC}"
    echo -e "${YELLOW}Warnings: $WARNINGS${NC}"

    if [ $ERRORS -gt 0 ]; then
        echo -e "\n${RED}Validation failed with errors. Please fix the issues above.${NC}"
        exit 1
    else
        echo -e "\n${YELLOW}Validation completed with warnings. Review the issues above.${NC}"
        exit 0
    fi
fi
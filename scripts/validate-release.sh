#!/bin/bash

# SubControl Release Validation Script
# This script validates APK files for Obtainium compatibility

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if required tools are available
check_tools() {
    local tools_missing=false
    
    # Check for Android SDK tools
    if ! command -v aapt &> /dev/null; then
        print_error "aapt not found. Please install Android SDK build-tools."
        tools_missing=true
    fi
    
    if ! command -v jarsigner &> /dev/null; then
        print_error "jarsigner not found. Please install Java JDK."
        tools_missing=true
    fi
    
    if ! command -v zipalign &> /dev/null; then
        print_error "zipalign not found. Please install Android SDK build-tools."
        tools_missing=true
    fi
    
    if ! command -v apksigner &> /dev/null; then
        print_warning "apksigner not found. Some validations will be skipped."
    fi
    
    if $tools_missing; then
        exit 1
    fi
}

# Function to validate APK file exists
validate_apk_exists() {
    local apk_path=$1
    
    if [[ ! -f "$apk_path" ]]; then
        print_error "APK file not found: $apk_path"
        exit 1
    fi
    
    print_success "APK file found: $apk_path"
}

# Function to validate APK signature
validate_signature() {
    local apk_path=$1
    
    print_status "Validating APK signature..."
    
    # Use jarsigner to verify signature
    if jarsigner -verify -verbose -certs "$apk_path" > /dev/null 2>&1; then
        print_success "APK signature is valid"
    else
        print_error "APK signature is invalid or missing"
        return 1
    fi
    
    # Use apksigner if available for additional verification
    if command -v apksigner &> /dev/null; then
        if apksigner verify "$apk_path" > /dev/null 2>&1; then
            print_success "APK signature verified with apksigner"
        else
            print_error "APK signature verification failed with apksigner"
            return 1
        fi
    fi
    
    return 0
}

# Function to validate APK alignment
validate_alignment() {
    local apk_path=$1
    
    print_status "Validating APK alignment..."
    
    if zipalign -c 4 "$apk_path" > /dev/null 2>&1; then
        print_success "APK is properly aligned"
    else
        print_error "APK is not properly aligned"
        return 1
    fi
    
    return 0
}

# Function to extract and validate APK metadata
validate_metadata() {
    local apk_path=$1
    
    print_status "Extracting APK metadata..."
    
    # Extract basic APK information
    local apk_info=$(aapt dump badging "$apk_path" 2>/dev/null)
    
    if [[ -z "$apk_info" ]]; then
        print_error "Failed to extract APK metadata"
        return 1
    fi
    
    # Extract key information
    local package_name=$(echo "$apk_info" | grep "package:" | sed -E "s/.*name='([^']+)'.*/\1/")
    local version_code=$(echo "$apk_info" | grep "package:" | sed -E "s/.*versionCode='([^']+)'.*/\1/")
    local version_name=$(echo "$apk_info" | grep "package:" | sed -E "s/.*versionName='([^']+)'.*/\1/")
    local min_sdk=$(echo "$apk_info" | grep "sdkVersion:" | sed -E "s/.*'([^']+)'.*/\1/")
    local target_sdk=$(echo "$apk_info" | grep "targetSdkVersion:" | sed -E "s/.*'([^']+)'.*/\1/")
    
    echo
    echo "APK Metadata:"
    echo "  Package: $package_name"
    echo "  Version: $version_name ($version_code)"
    echo "  Min SDK: $min_sdk"
    echo "  Target SDK: $target_sdk"
    echo
    
    # Validate expected values
    if [[ "$package_name" != "com.subcontrol" ]]; then
        print_error "Unexpected package name: $package_name (expected: com.subcontrol)"
        return 1
    fi
    
    if [[ -z "$version_name" ]]; then
        print_error "Version name is empty"
        return 1
    fi
    
    if [[ -z "$version_code" ]]; then
        print_error "Version code is empty"
        return 1
    fi
    
    print_success "APK metadata is valid"
    return 0
}

# Function to validate APK permissions
validate_permissions() {
    local apk_path=$1
    
    print_status "Validating APK permissions..."
    
    # Extract permissions
    local permissions=$(aapt dump permissions "$apk_path" 2>/dev/null)
    
    if [[ -z "$permissions" ]]; then
        print_warning "No permissions found or failed to extract permissions"
        return 0
    fi
    
    echo
    echo "APK Permissions:"
    echo "$permissions"
    echo
    
    # Check for potentially problematic permissions
    if echo "$permissions" | grep -q "android.permission.INTERNET"; then
        print_warning "APK requests INTERNET permission (should be minimal for privacy-focused app)"
    fi
    
    if echo "$permissions" | grep -q "android.permission.ACCESS_NETWORK_STATE"; then
        print_warning "APK requests NETWORK_STATE permission"
    fi
    
    # Check for concerning permissions
    local concerning_perms=(
        "android.permission.ACCESS_FINE_LOCATION"
        "android.permission.ACCESS_COARSE_LOCATION"
        "android.permission.CAMERA"
        "android.permission.RECORD_AUDIO"
        "android.permission.READ_CONTACTS"
        "android.permission.READ_SMS"
        "android.permission.SEND_SMS"
        "android.permission.CALL_PHONE"
    )
    
    for perm in "${concerning_perms[@]}"; do
        if echo "$permissions" | grep -q "$perm"; then
            print_error "APK requests concerning permission: $perm"
            return 1
        fi
    done
    
    print_success "APK permissions are appropriate"
    return 0
}

# Function to validate APK size
validate_size() {
    local apk_path=$1
    
    print_status "Validating APK size..."
    
    local size_bytes=$(stat -c%s "$apk_path" 2>/dev/null || stat -f%z "$apk_path")
    local size_mb=$((size_bytes / 1024 / 1024))
    
    echo "APK Size: ${size_mb}MB"
    
    # Check if size is reasonable (less than 50MB for this type of app)
    if [[ $size_mb -gt 50 ]]; then
        print_warning "APK size is quite large (${size_mb}MB). Consider optimizing."
    else
        print_success "APK size is reasonable (${size_mb}MB)"
    fi
    
    return 0
}

# Function to validate APK name format
validate_name_format() {
    local apk_path=$1
    local filename=$(basename "$apk_path")
    
    print_status "Validating APK name format..."
    
    # Check if filename matches expected pattern: SubControl-v{version}.apk
    if [[ $filename =~ ^SubControl-v[0-9]+\.[0-9]+\.[0-9]+\.apk$ ]]; then
        print_success "APK name format is correct: $filename"
    else
        print_error "APK name format is incorrect: $filename"
        print_error "Expected format: SubControl-v{version}.apk (e.g., SubControl-v1.0.0.apk)"
        return 1
    fi
    
    return 0
}

# Function to validate Obtainium compatibility
validate_obtainium_compatibility() {
    local apk_path=$1
    
    print_status "Validating Obtainium compatibility..."
    
    # Check all requirements for Obtainium
    local compatible=true
    
    # Must be a valid APK
    if ! validate_signature "$apk_path"; then
        compatible=false
    fi
    
    # Must have proper metadata
    if ! validate_metadata "$apk_path"; then
        compatible=false
    fi
    
    # Must have proper name format
    if ! validate_name_format "$apk_path"; then
        compatible=false
    fi
    
    # Must be properly aligned
    if ! validate_alignment "$apk_path"; then
        compatible=false
    fi
    
    if $compatible; then
        print_success "APK is compatible with Obtainium"
    else
        print_error "APK is NOT compatible with Obtainium"
        return 1
    fi
    
    return 0
}

# Function to show summary
show_summary() {
    local apk_path=$1
    local validation_result=$2
    
    echo
    echo "=================================="
    echo "Validation Summary"
    echo "=================================="
    echo "APK: $(basename "$apk_path")"
    
    if [[ $validation_result -eq 0 ]]; then
        print_success "All validations passed"
        echo
        echo "This APK is ready for:"
        echo "  ✓ Installation on Android devices"
        echo "  ✓ Distribution via GitHub releases"
        echo "  ✓ Use with Obtainium"
        echo "  ✓ Manual installation"
    else
        print_error "Validation failed"
        echo
        echo "Please fix the issues above before releasing."
    fi
    
    echo
}

# Main script
main() {
    echo "SubControl Release Validation Script"
    echo "===================================="
    echo
    
    # Get APK path from argument or prompt
    local apk_path
    if [[ -n "$1" ]]; then
        apk_path="$1"
    else
        read -p "Enter path to APK file: " apk_path
    fi
    
    # Check if tools are available
    check_tools
    
    # Validate APK exists
    validate_apk_exists "$apk_path"
    
    # Run all validations
    local validation_result=0
    
    validate_signature "$apk_path" || validation_result=1
    validate_alignment "$apk_path" || validation_result=1
    validate_metadata "$apk_path" || validation_result=1
    validate_permissions "$apk_path" || validation_result=1
    validate_size "$apk_path" || validation_result=1
    validate_name_format "$apk_path" || validation_result=1
    
    # Show summary
    show_summary "$apk_path" $validation_result
    
    exit $validation_result
}

# Handle script interruption
trap 'print_error "Script interrupted"; exit 1' INT TERM

# Run main function
main "$@"
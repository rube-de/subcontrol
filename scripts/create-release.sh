#!/bin/bash

# SubControl Release Creation Script
# This script helps create a new release with proper versioning and validation

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

# Function to validate version format
validate_version() {
    local version=$1
    if [[ ! $version =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        print_error "Invalid version format. Use semantic versioning (e.g., 1.0.0)"
        return 1
    fi
    return 0
}

# Function to check if version already exists
check_version_exists() {
    local version=$1
    local tag="v$version"
    
    if git tag -l | grep -q "^$tag$"; then
        print_error "Version $version already exists as tag $tag"
        return 1
    fi
    return 0
}

# Function to validate current branch
validate_branch() {
    local current_branch=$(git branch --show-current)
    if [[ "$current_branch" != "main" ]]; then
        print_warning "Current branch is '$current_branch', not 'main'"
        read -p "Continue anyway? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            print_error "Release cancelled"
            exit 1
        fi
    fi
}

# Function to check working directory status
check_working_directory() {
    if [[ -n $(git status --porcelain) ]]; then
        print_error "Working directory is not clean. Please commit or stash changes first."
        git status --short
        exit 1
    fi
}

# Function to update version in files
update_version_in_files() {
    local version=$1
    local version_code=$2
    
    print_status "Updating version in build.gradle.kts..."
    
    # Update version in build.gradle.kts
    sed -i.bak "s/versionCode = [0-9]*/versionCode = $version_code/" app/build.gradle.kts
    sed -i.bak "s/versionName = \"[^\"]*\"/versionName = \"$version\"/" app/build.gradle.kts
    
    # Remove backup file
    rm -f app/build.gradle.kts.bak
    
    print_success "Version updated to $version (code: $version_code)"
}

# Function to calculate version code
calculate_version_code() {
    local version=$1
    IFS='.' read -r major minor patch <<< "$version"
    echo $((major * 10000 + minor * 100 + patch))
}

# Function to validate build
validate_build() {
    print_status "Validating build configuration..."
    
    # Check if gradlew is executable
    if [[ ! -x ./gradlew ]]; then
        print_error "gradlew is not executable"
        exit 1
    fi
    
    # Run a quick build check
    print_status "Running build validation..."
    ./gradlew assembleDebug --quiet
    
    if [[ $? -eq 0 ]]; then
        print_success "Build validation passed"
    else
        print_error "Build validation failed"
        exit 1
    fi
}

# Function to check GitHub secrets
check_github_secrets() {
    print_status "Checking GitHub repository configuration..."
    
    # Check if we're in a git repository
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        print_error "Not in a git repository"
        exit 1
    fi
    
    # Check if we have a remote origin
    if ! git remote get-url origin > /dev/null 2>&1; then
        print_error "No remote origin configured"
        exit 1
    fi
    
    local remote_url=$(git remote get-url origin)
    if [[ $remote_url == *"github.com"* ]]; then
        print_success "GitHub repository detected"
    else
        print_warning "Remote origin is not GitHub. Release workflow may not work."
    fi
}

# Function to create and push tag
create_and_push_tag() {
    local version=$1
    local tag="v$version"
    
    print_status "Creating tag $tag..."
    git tag -a "$tag" -m "Release version $version"
    
    print_status "Pushing tag to origin..."
    git push origin "$tag"
    
    print_success "Tag $tag created and pushed"
}

# Function to show release information
show_release_info() {
    local version=$1
    local tag="v$version"
    local remote_url=$(git remote get-url origin)
    
    # Extract repository info from URL
    local repo_path
    if [[ $remote_url == *"github.com"* ]]; then
        repo_path=$(echo "$remote_url" | sed -E 's/.*github\.com[:/]([^/]+\/[^/]+)(\.git)?$/\1/')
        
        echo
        print_success "Release initiated successfully!"
        echo
        echo "Release Details:"
        echo "  Version: $version"
        echo "  Tag: $tag"
        echo "  Repository: $repo_path"
        echo
        echo "Next Steps:"
        echo "  1. Monitor the GitHub Action at:"
        echo "     https://github.com/$repo_path/actions"
        echo
        echo "  2. Once complete, the release will be available at:"
        echo "     https://github.com/$repo_path/releases/tag/$tag"
        echo
        echo "  3. APK will be named: SubControl-v$version.apk"
        echo
        echo "  4. Users can add this to Obtainium using:"
        echo "     https://github.com/$repo_path"
        echo
    else
        print_success "Tag created and pushed to $remote_url"
    fi
}

# Main script
main() {
    echo "SubControl Release Creation Script"
    echo "=================================="
    echo
    
    # Check prerequisites
    check_working_directory
    validate_branch
    check_github_secrets
    
    # Get version from user
    if [[ -n "$1" ]]; then
        VERSION="$1"
    else
        read -p "Enter version number (e.g., 1.0.0): " VERSION
    fi
    
    # Validate version
    if ! validate_version "$VERSION"; then
        exit 1
    fi
    
    # Check if version already exists
    if ! check_version_exists "$VERSION"; then
        exit 1
    fi
    
    # Calculate version code
    VERSION_CODE=$(calculate_version_code "$VERSION")
    
    # Show confirmation
    echo
    echo "Release Configuration:"
    echo "  Version: $VERSION"
    echo "  Version Code: $VERSION_CODE"
    echo "  Tag: v$VERSION"
    echo
    
    read -p "Proceed with release? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_error "Release cancelled"
        exit 1
    fi
    
    # Update version in files
    update_version_in_files "$VERSION" "$VERSION_CODE"
    
    # Validate build
    validate_build
    
    # Generate and update changelog
    print_status "Generating changelog..."
    if [[ -x scripts/generate-changelog.sh ]]; then
        if [[ -f "CHANGELOG.md" ]]; then
            ./scripts/generate-changelog.sh --update CHANGELOG.md "$VERSION"
        else
            ./scripts/generate-changelog.sh --full CHANGELOG.md
        fi
    else
        print_warning "Changelog generation script not found or not executable"
    fi
    
    # Commit version changes and changelog
    print_status "Committing version changes and changelog..."
    git add app/build.gradle.kts
    if [[ -f "CHANGELOG.md" ]]; then
        git add CHANGELOG.md
    fi
    git commit -m "Bump version to $VERSION and update changelog"
    git push origin $(git branch --show-current)
    
    # Create and push tag
    create_and_push_tag "$VERSION"
    
    # Show release information
    show_release_info "$VERSION"
}

# Handle script interruption
trap 'print_error "Script interrupted"; exit 1' INT TERM

# Run main function
main "$@"
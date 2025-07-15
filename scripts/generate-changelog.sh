#!/bin/bash

# SubControl Changelog Generation Script
# This script generates changelog entries from git commits between releases

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

# Function to get the latest tag
get_latest_tag() {
    git describe --tags --abbrev=0 2>/dev/null || echo ""
}

# Function to get all tags sorted by version
get_all_tags() {
    git tag -l | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' | sort -V
}

# Function to categorize commit based on conventional commits
categorize_commit() {
    local commit_msg="$1"
    local commit_hash="$2"
    
    # Extract the type from conventional commit format
    if [[ $commit_msg =~ ^(feat|feature)(\([^)]+\))?: ]]; then
        echo "### Added"
        echo "- ${commit_msg#*: } (${commit_hash:0:7})"
    elif [[ $commit_msg =~ ^(fix|bugfix)(\([^)]+\))?: ]]; then
        echo "### Fixed"
        echo "- ${commit_msg#*: } (${commit_hash:0:7})"
    elif [[ $commit_msg =~ ^(chore|build|ci|docs)(\([^)]+\))?: ]]; then
        echo "### Changed"
        echo "- ${commit_msg#*: } (${commit_hash:0:7})"
    elif [[ $commit_msg =~ ^(refactor|perf|style)(\([^)]+\))?: ]]; then
        echo "### Changed"
        echo "- ${commit_msg#*: } (${commit_hash:0:7})"
    elif [[ $commit_msg =~ ^(test)(\([^)]+\))?: ]]; then
        echo "### Changed"
        echo "- ${commit_msg#*: } (${commit_hash:0:7})"
    elif [[ $commit_msg =~ ^(breaking|break)(\([^)]+\))?: ]]; then
        echo "### Breaking Changes"
        echo "- ${commit_msg#*: } (${commit_hash:0:7})"
    else
        # Try to categorize based on keywords in commit message
        local lower_msg=$(echo "$commit_msg" | tr '[:upper:]' '[:lower:]')
        
        if [[ $lower_msg =~ (add|new|implement|create|introduce) ]]; then
            echo "### Added"
            echo "- $commit_msg (${commit_hash:0:7})"
        elif [[ $lower_msg =~ (fix|bug|issue|resolve|patch) ]]; then
            echo "### Fixed"
            echo "- $commit_msg (${commit_hash:0:7})"
        elif [[ $lower_msg =~ (update|change|modify|improve|enhance) ]]; then
            echo "### Changed"
            echo "- $commit_msg (${commit_hash:0:7})"
        elif [[ $lower_msg =~ (remove|delete|drop|deprecate) ]]; then
            echo "### Removed"
            echo "- $commit_msg (${commit_hash:0:7})"
        elif [[ $lower_msg =~ (security|secure|vulnerability|cve) ]]; then
            echo "### Security"
            echo "- $commit_msg (${commit_hash:0:7})"
        else
            echo "### Changed"
            echo "- $commit_msg (${commit_hash:0:7})"
        fi
    fi
}

# Function to generate changelog for a version range
generate_changelog_section() {
    local from_tag="$1"
    local to_tag="$2"
    local version="$3"
    
    print_status "Generating changelog for $version..."
    
    # Get commits between tags
    local git_range
    if [[ -n "$from_tag" ]]; then
        git_range="$from_tag..$to_tag"
    else
        git_range="$to_tag"
    fi
    
    # Get commit messages with hashes
    local commits=$(git log --format="%H|%s" --no-merges --reverse $git_range 2>/dev/null)
    
    if [[ -z "$commits" ]]; then
        print_warning "No commits found for range $git_range"
        return
    fi
    
    # Generate changelog header
    local date=$(git log -1 --format="%ai" $to_tag | cut -d' ' -f1)
    echo "## [$version] - $date"
    echo
    
    # Process commits and categorize them
    local -A categories
    categories[Added]=""
    categories[Fixed]=""
    categories[Changed]=""
    categories[Removed]=""
    categories[Security]=""
    categories[Breaking Changes]=""
    
    while IFS='|' read -r commit_hash commit_msg; do
        if [[ -n "$commit_hash" && -n "$commit_msg" ]]; then
            # Skip release commits
            if [[ $commit_msg =~ ^(Release|Bump|Version|v[0-9]+\.[0-9]+\.[0-9]+) ]]; then
                continue
            fi
            
            # Get category and entry
            local category_output=$(categorize_commit "$commit_msg" "$commit_hash")
            local category=$(echo "$category_output" | head -1)
            local entry=$(echo "$category_output" | tail -1)
            
            # Add to appropriate category
            case "$category" in
                "### Added")
                    categories[Added]+="$entry"$'\n'
                    ;;
                "### Fixed")
                    categories[Fixed]+="$entry"$'\n'
                    ;;
                "### Changed")
                    categories[Changed]+="$entry"$'\n'
                    ;;
                "### Removed")
                    categories[Removed]+="$entry"$'\n'
                    ;;
                "### Security")
                    categories[Security]+="$entry"$'\n'
                    ;;
                "### Breaking Changes")
                    categories[Breaking Changes]+="$entry"$'\n'
                    ;;
            esac
        fi
    done <<< "$commits"
    
    # Output categories with content
    for category in "Breaking Changes" "Added" "Changed" "Fixed" "Removed" "Security"; do
        if [[ -n "${categories[$category]}" ]]; then
            echo "### $category"
            echo "${categories[$category]}"
        fi
    done
    
    echo
}

# Function to generate full changelog
generate_full_changelog() {
    local output_file="$1"
    
    print_status "Generating full changelog..."
    
    # Get all tags sorted by version
    local tags=($(get_all_tags))
    
    if [[ ${#tags[@]} -eq 0 ]]; then
        print_warning "No version tags found. Creating initial changelog."
        echo "# Changelog" > "$output_file"
        echo "" >> "$output_file"
        echo "All notable changes to this project will be documented in this file." >> "$output_file"
        echo "" >> "$output_file"
        echo "The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)," >> "$output_file"
        echo "and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)." >> "$output_file"
        echo "" >> "$output_file"
        return
    fi
    
    # Create changelog header
    {
        echo "# Changelog"
        echo
        echo "All notable changes to this project will be documented in this file."
        echo
        echo "The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),"
        echo "and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)."
        echo
    } > "$output_file"
    
    # Generate changelog for each version
    local prev_tag=""
    for tag in "${tags[@]}"; do
        local version=${tag#v}  # Remove 'v' prefix
        generate_changelog_section "$prev_tag" "$tag" "$version" >> "$output_file"
        prev_tag="$tag"
    done
    
    print_success "Changelog generated: $output_file"
}

# Function to generate changelog since last release
generate_incremental_changelog() {
    local latest_tag=$(get_latest_tag)
    local current_version="$1"
    
    if [[ -z "$latest_tag" ]]; then
        print_warning "No previous tags found. Generating changelog from beginning."
        latest_tag=""
    fi
    
    print_status "Generating changelog since $latest_tag..."
    
    # Generate changelog section for current version
    generate_changelog_section "$latest_tag" "HEAD" "$current_version"
}

# Function to update existing changelog
update_changelog() {
    local changelog_file="$1"
    local new_version="$2"
    
    if [[ ! -f "$changelog_file" ]]; then
        print_error "Changelog file not found: $changelog_file"
        return 1
    fi
    
    print_status "Updating changelog with version $new_version..."
    
    # Generate new changelog entry
    local temp_file=$(mktemp)
    generate_incremental_changelog "$new_version" > "$temp_file"
    
    # Insert new entry after the header
    local header_lines=$(grep -n "^## " "$changelog_file" | head -1 | cut -d: -f1)
    if [[ -z "$header_lines" ]]; then
        header_lines=$(wc -l < "$changelog_file")
        header_lines=$((header_lines + 1))
    fi
    
    # Create updated changelog
    local updated_changelog=$(mktemp)
    
    # Copy header
    head -n $((header_lines - 1)) "$changelog_file" > "$updated_changelog"
    
    # Add new entry
    cat "$temp_file" >> "$updated_changelog"
    
    # Add rest of changelog
    tail -n +$header_lines "$changelog_file" >> "$updated_changelog"
    
    # Replace original file
    mv "$updated_changelog" "$changelog_file"
    
    # Cleanup
    rm -f "$temp_file"
    
    print_success "Changelog updated: $changelog_file"
}

# Function to validate git repository
validate_git_repo() {
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        print_error "Not in a git repository"
        exit 1
    fi
    
    # Check if we have commits
    if ! git log --oneline -1 > /dev/null 2>&1; then
        print_error "No commits found in repository"
        exit 1
    fi
}

# Function to show help
show_help() {
    echo "SubControl Changelog Generation Script"
    echo
    echo "Usage: $0 [OPTIONS] [VERSION]"
    echo
    echo "Options:"
    echo "  -f, --full FILE          Generate full changelog to FILE"
    echo "  -u, --update FILE        Update existing changelog FILE with new version"
    echo "  -i, --incremental VERSION Generate changelog since last release for VERSION"
    echo "  -h, --help               Show this help message"
    echo
    echo "Examples:"
    echo "  $0 --full CHANGELOG.md               # Generate full changelog"
    echo "  $0 --update CHANGELOG.md 1.2.3      # Update changelog with version 1.2.3"
    echo "  $0 --incremental 1.2.3              # Show changelog since last release"
    echo "  $0 1.2.3                            # Same as --incremental"
    echo
}

# Main script
main() {
    # Validate git repository
    validate_git_repo
    
    # Parse arguments
    case "$1" in
        -f|--full)
            if [[ -z "$2" ]]; then
                print_error "Output file required for --full option"
                exit 1
            fi
            generate_full_changelog "$2"
            ;;
        -u|--update)
            if [[ -z "$2" ]] || [[ -z "$3" ]]; then
                print_error "Changelog file and version required for --update option"
                exit 1
            fi
            update_changelog "$2" "$3"
            ;;
        -i|--incremental)
            if [[ -z "$2" ]]; then
                print_error "Version required for --incremental option"
                exit 1
            fi
            generate_incremental_changelog "$2"
            ;;
        -h|--help)
            show_help
            ;;
        "")
            print_error "No arguments provided"
            show_help
            exit 1
            ;;
        *)
            # Assume version for incremental changelog
            generate_incremental_changelog "$1"
            ;;
    esac
}

# Handle script interruption
trap 'print_error "Script interrupted"; exit 1' INT TERM

# Run main function
main "$@"
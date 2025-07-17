# Learnings from Fixing SubControl Test Suite

This document captures key learnings from debugging and fixing test failures in the SubControl Android project.

## 1. Kotlin Object Singleton Mocking

**Issue**: Tests were failing because `EncryptionManager` is a Kotlin `object` (singleton), not a class.

**Learning**: When mocking Kotlin objects with MockK, you need to use `mockkObject()` instead of regular `mockk()`:

```kotlin
// Wrong - for classes
val encryptionManager = mockk<EncryptionManager>()

// Correct - for objects
mockkObject(EncryptionManager)
every { EncryptionManager.encrypt(any()) } returns byteArrayOf()
```

## 2. Coroutine Test Dispatcher Issues

**Issue**: Tests were failing with "Detected use of different schedulers" error.

**Learning**: When testing coroutines, ensure all components use the same test dispatcher:

```kotlin
@Test
fun `test with coroutines`() = runTest {
    // Pass the test scheduler's dispatcher to the repository
    repository = SubscriptionRepositoryImpl(dataStore, UnconfinedTestDispatcher(testScheduler))
    // ... test code
}
```

## 3. Flow Exception Handling

**Issue**: Repository catches exceptions and emits empty lists, but tests expected errors to propagate.

**Learning**: When testing Flows that handle exceptions internally:
- Check the actual implementation to understand error handling behavior
- Mock Flows that throw exceptions during collection, not during setup:

```kotlin
// Wrong - throws immediately during mock setup
coEvery { dataStore.data } throws Exception("Error")

// Correct - throws during Flow collection
coEvery { dataStore.data } returns flow { throw Exception("Error") }
```

## 4. BigDecimal Comparison

**Issue**: BigDecimal comparison failed due to scale differences (0 vs 0.00).

**Learning**: Use `compareTo()` for BigDecimal equality checks to ignore scale:

```kotlin
// Wrong - scale-sensitive
assertEquals(BigDecimal.ZERO, result)

// Correct - scale-insensitive
assertTrue(result.compareTo(BigDecimal.ZERO) == 0)
```

## 5. Proto DataStore Testing

**Issue**: Tests used non-existent `FakeAppData` class.

**Learning**: Use actual Proto-generated classes for testing:

```kotlin
private fun createAppData(subscriptions: List<Subscription>): AppData {
    val protoSubscriptions = subscriptions.map { it.toProto() }
    return AppData.newBuilder()
        .setSubscriptionList(
            SubscriptionList.newBuilder()
                .addAllSubscriptions(protoSubscriptions)
                .build()
        )
        .build()
}
```

## 6. Import Conflicts

**Issue**: Tests had wrong imports (kotlin.test instead of JUnit).

**Learning**: Be careful with auto-imports. Android projects typically use JUnit:

```kotlin
// Wrong
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// Correct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
```

## 7. Gradle DSL Format Differences

**Issue**: Version parser expected Groovy format but the project uses Kotlin DSL.

**Learning**: Support both Gradle formats when parsing build files:

```kotlin
// Support both formats:
// Groovy: versionName "1.0.0"
// Kotlin: versionName = "1.0.0"
val versionNameRegex = Regex("versionName\\s*=?\\s*\"([^\"]+)\"")
```

## 8. Test Data Validation

**Issue**: Changelog tests failed because commit hashes were too short.

**Learning**: Validate test data matches implementation expectations:
- Commit hashes need at least 7 characters
- Check substring operations won't exceed string length
- Ensure test data is realistic

## 9. Dependency Management

**Issue**: Missing material-icons-extended dependency caused compilation errors.

**Learning**: When fixing UI-related compilation errors:
- Check if the icon/component is from an extended library
- Add dependencies to the appropriate bundle in `libs.versions.toml`
- Ensure the dependency is included in the module's build.gradle

## 10. Flow Testing with Turbine

**Issue**: Understanding how to properly test Flow emissions and errors.

**Learning**: Turbine provides a clean API for Flow testing:

```kotlin
repository.getAllSubscriptions().test {
    val result = awaitItem()  // Get first emission
    assertEquals(expected, result)
    awaitComplete()  // Ensure Flow completes
}
```

## 11. Method Name Refactoring

**Issue**: Code was using old method names that had been refactored.

**Learning**: When refactoring method names:
- Use IDE's refactoring tools to catch all usages
- Run tests immediately after refactoring
- Check for string-based references that IDEs might miss

## 12. Test Organization

**Learning**: Good test organization practices observed:
- Test structure mirrors main source structure
- Each test class focuses on a single class/component
- Test names clearly describe what they're testing
- Use of `@Before` for common setup
- Proper use of mocking for dependencies

## Key Takeaways

1. **Always read the actual implementation** before writing or fixing tests
2. **Use proper test utilities** for the framework (MockK for Kotlin, Turbine for Flows)
3. **Understand the threading model** when testing coroutines
4. **Match test data to implementation requirements** (string lengths, formats, etc.)
5. **Be careful with auto-imports** - verify they're from the correct library
6. **Test both success and failure paths** including exception handling
7. **Keep tests focused and isolated** - one concept per test
8. **Use meaningful test names** that describe the scenario and expected outcome

## Tools and Libraries Used

- **MockK**: Kotlin mocking library with special support for objects, coroutines
- **Turbine**: Flow testing library for clean assertions
- **JUnit 4**: Test framework (not kotlin.test)
- **Coroutines Test**: Provides TestDispatcher and runTest
- **Protocol Buffers**: For data serialization testing

## Common Patterns

### Repository Testing Pattern
```kotlin
@Test
fun `repository operation succeeds`() = runTest {
    // Given
    repository = RepositoryImpl(dataStore, UnconfinedTestDispatcher(testScheduler))
    coEvery { dataStore.data } returns flowOf(testData)
    
    // When
    val result = repository.getData()
    
    // Then
    result.test {
        assertEquals(expected, awaitItem())
        awaitComplete()
    }
}
```

### ViewModel Testing Pattern
```kotlin
@Test
fun `viewmodel updates UI state`() = runTest {
    // Given
    every { mockRepository.getData() } returns flowOf(testData)
    
    // When
    viewModel = TestViewModel(mockRepository)
    
    // Then
    viewModel.uiState.test {
        val state = awaitItem()
        assertEquals(expected, state.data)
        assertFalse(state.isLoading)
    }
}
```

## Android GitHub Actions Setup

### Issue: zipalign not found
**Date:** 2025-01-15
**Error:** `Unable to locate executable file: /usr/local/lib/android/sdk/build-tools/29.0.3/zipalign`

**Root Cause:** 
The Android SDK and build-tools are not automatically installed in GitHub Actions runners. You need to explicitly set up the Android SDK and install the required build-tools version.

**Solution:**
1. Add `android-actions/setup-android@v3` action after Java setup
2. Install specific build-tools version using sdkmanager
3. Add build-tools to PATH for command access

**Fixed Configuration:**
```yaml
- name: Setup Android SDK
  uses: android-actions/setup-android@v3
  
- name: Install Android Build Tools
  run: |
    # Install build-tools that include zipalign and aapt
    sdkmanager "build-tools;34.0.0"
    
    # Add build-tools to PATH
    echo "$ANDROID_HOME/build-tools/34.0.0" >> $GITHUB_PATH
```

**Key Learnings:**
- GitHub Actions runners are minimal Ubuntu environments without Android SDK pre-installed
- Android SDK must be explicitly installed using `android-actions/setup-android@v3`
- Build tools (zipalign, aapt, etc.) are located in version-specific directories
- Always add build-tools to PATH for command access in subsequent steps
- The `android-actions/setup-android@v3` action handles SDK installation and license acceptance automatically
- Use consistent build-tools versions across all workflow steps

### Additional GitHub Actions Best Practices
- Use latest action versions (@v4 for most actions, @v3 for android-actions)
- Cache Gradle dependencies for 40-60% faster builds
- Java 17 is required for modern Android projects
- Always make gradlew executable with `chmod +x`
- The `r0adkll/sign-android-release@v1` action handles zipalign internally, but you still need build-tools for other commands like `aapt`

### Issue: r0adkll/sign-android-release@v1 zipalign path mismatch
**Date:** 2025-01-16
**Error:** `Unable to locate executable file: /usr/local/lib/android/sdk/build-tools/29.0.3/zipalign`

**Root Cause:**
The `r0adkll/sign-android-release@v1` action was looking for zipalign in an older build-tools version (29.0.3) while the workflow installed version 34.0.0.

**Solution:**
Switch to the `kevin-david/zipalign-sign-android-release@v2` fork which:
- Has better support for newer build-tools versions
- Allows explicit build-tools version specification via `BUILD_TOOLS_VERSION` environment variable
- Defaults to build-tools 33.0.0 (despite documentation claiming 35.0.0)

**Fixed Configuration:**
```yaml
- name: Sign APK
  id: sign_apk
  uses: kevin-david/zipalign-sign-android-release@v2
  with:
    releaseDirectory: app/build/outputs/apk/release
    signingKeyBase64: ${{ secrets.SIGNING_KEY }}
    alias: ${{ secrets.ALIAS }}
    keyStorePassword: ${{ secrets.KEY_STORE_PASSWORD }}
    keyPassword: ${{ secrets.KEY_PASSWORD }}
    zipAlign: true  # Actually zipalign the APK instead of just verifying
  env:
    BUILD_TOOLS_VERSION: "34.0.0"  # Must be set as env var, not parameter
```

**Key Learnings:**
- Some GitHub Actions may have hardcoded paths to older Android SDK versions
- Check for maintained forks when the original action has compatibility issues
- Always match the build-tools version between SDK installation and action usage
- The `kevin-david/zipalign-sign-android-release@v2` fork is actively maintained and more compatible with modern Android projects
- **IMPORTANT**: The build-tools version must be set as an environment variable (`BUILD_TOOLS_VERSION`), not as a parameter
- The `zipAlign: true` parameter is needed to actually zipalign the APK (default is false, which only verifies)
- Parameter names are case-sensitive - using `buildToolsVersion` as a parameter will cause an "Unexpected input(s)" error

### Issue: APK signing workflow failures in GitHub Actions
**Date:** 2025-01-16
**Error:** "Process completed with exit code 1" after signing step

**Root Causes:**
1. The output filename from the signing action might not match what we expect
2. Gradle release builds might be configured to require signing, preventing unsigned APK generation
3. The signing action's output path might be different than assumed

**Solutions:**
1. Use the action's output variable `${{ steps.sign_apk.outputs.signedReleaseFile }}` instead of hardcoded filenames
2. Add `signingConfig = null` to the release build type to ensure gradle produces unsigned APKs
3. Add debug logging to identify the actual output files

**Fixed Rename Step:**
```bash
SIGNED_APK="${{ steps.sign_apk.outputs.signedReleaseFile }}"
if [ -f "$SIGNED_APK" ]; then
    mv "$SIGNED_APK" "SubControl-v$VERSION.apk"
else
    # Fallback: find any signed APK
    SIGNED_FILE=$(find app/build/outputs/apk/release/ -name "*signed*.apk" -o -name "*release*.apk" | head -1)
    mv "$SIGNED_FILE" "SubControl-v$VERSION.apk"
fi
```

**Key Learnings:**
- Always use action output variables rather than assuming filenames
- Ensure gradle is configured to produce unsigned APKs when signing externally
- Add debug logging in CI/CD pipelines to diagnose file-related issues
- The cache error "Error: Cache service responded with 400" is just a warning and can be ignored

### Issue: GitHub Actions cache service 400 errors
**Date:** 2025-01-16
**Error:** "Cache service responded with 400" when restoring Gradle cache

**Root Causes:**
1. Cache key too long or contains invalid characters
2. GitHub Actions cache service limitations
3. Complex hash keys from multiple gradle files

**Solutions:**
1. Use simpler cache keys with specific file paths
2. Add `continue-on-error: true` to cache steps
3. Use more specific file paths in hashFiles() instead of wildcards
4. Add debugging to verify cache behavior

**Improved Cache Configuration:**
```yaml
- name: Cache Gradle dependencies
  uses: actions/cache@v4
  id: gradle-cache
  with:
    path: |
      ~/.gradle/caches
      ~/.gradle/wrapper
    # Simpler cache key to avoid 400 errors
    key: gradle-${{ runner.os }}-${{ hashFiles('gradle/wrapper/gradle-wrapper.properties') }}-${{ hashFiles('gradle/libs.versions.toml') }}
    restore-keys: |
      gradle-${{ runner.os }}-
  # Continue on cache errors since they're just warnings
  continue-on-error: true
```

**Key Learnings:**
- Cache errors with 400 status are typically non-fatal warnings
- Simpler cache keys reduce the chance of 400 errors
- Always use `continue-on-error: true` for cache steps in release workflows
- Add debugging logs to understand actual build failures vs cache warnings
- The gradle-build-action@v2 also manages its own caching independently

### Issue: APK verification fails with missing icon resource
**Date:** 2025-01-16
**Error:** "ERROR getting 'android:icon' attribute: attribute value reference does not exist"

**Root Cause:**
The AndroidManifest.xml was using `@android:mipmap/sym_def_app_icon` which is a system reference that doesn't exist in release builds, and the app had no actual icon resources in the mipmap directories.

**Solutions:**
1. Update AndroidManifest.xml to use standard icon references: `@mipmap/ic_launcher`
2. Modify the APK verification step to handle resource warnings gracefully
3. Filter aapt output to show only relevant information while continuing on warnings

**Fixed Verification Step:**
```bash
# Run aapt but continue on warnings about missing resources
$ANDROID_HOME/build-tools/34.0.0/aapt dump badging "$APK_PATH" 2>&1 | grep -E "(package:|sdkVersion:|targetSdkVersion:|application-label:|launchable-activity:)" || true

# The important verification is jarsigner
jarsigner -verify -verbose -certs "$APK_PATH"
```

**Key Learnings:**
- Always use standard app icon references, not system icons
- The aapt tool may show warnings for missing resources, but these don't affect APK validity
- The critical verification is jarsigner for signature validation
- Add proper app icons to avoid verification warnings in production

### Android App Icon Implementation
**Date:** 2025-01-16
**Task:** Create app icons for SubControl subscription manager

**Icon Design Approach:**
1. Created adaptive icons for Android 8.0+ (API 26+)
2. Used vector drawables for scalability and small file size
3. Designed a calendar with renewal arrows and dollar sign to represent subscription management

**Files Created:**
```
res/
├── drawable/
│   ├── ic_launcher_foreground.xml    # Foreground layer (icon design)
│   ├── ic_launcher_background.xml    # Background layer (white/subtle pattern)
│   └── ic_launcher_legacy.xml        # Legacy icon for older Android versions
└── mipmap-anydpi-v26/
    ├── ic_launcher.xml               # Adaptive icon configuration
    └── ic_launcher_round.xml         # Round adaptive icon configuration
```

**Key Design Elements:**
- Calendar shape representing scheduling and dates
- Circular arrows symbolizing renewal/recurring payments
- Dollar sign for financial/payment aspect
- Blue color scheme (#1976D2) for trust and professionalism
- Clean, modern design following Material Design guidelines

**Best Practices:**
- Use adaptive icons for Android 8.0+ for better device compatibility
- Vector drawables scale perfectly to any density
- Keep icon design simple and recognizable at small sizes
- Test icons on different launcher shapes (square, circle, squircle)
- Ensure sufficient contrast between foreground and background

### Issue: Bash regex syntax error in changelog script
**Date:** 2025-01-16
**Error:** "syntax error in conditional expression: unexpected token ')'"

**Root Cause:**
Bash regex patterns using `[[ $var =~ pattern ]]` have specific syntax requirements. The pattern `[^)]+` was causing syntax errors because the closing parenthesis inside the character class wasn't properly handled.

**Solution:**
1. Escape the closing parenthesis in character classes: `[^\)]`
2. Use `*` instead of `+` for zero or more matches: `[^\)]*`
3. Split complex patterns into multiple conditions with `||`

**Fixed Pattern:**
```bash
# Before (causes syntax error)
if [[ $commit_msg =~ ^(feat|feature)(\([^)]+\))?: ]]; then

# After (works correctly)
if [[ $commit_msg =~ ^(feat|feature)(\([^\)]*\))?: ]] || [[ $commit_msg =~ ^(feat|feature): ]]; then
```

**Key Learnings:**
- Bash regex syntax differs from standard regex in some cases
- Always escape special characters in character classes
- Test scripts with `bash -n script.sh` to check syntax
- Consider splitting complex regex patterns for better compatibility

### Issue: Detached HEAD state in GitHub Actions release workflow
**Date:** 2025-01-16
**Error:** "fatal: You are not currently on a branch" when pushing changelog updates

**Root Cause:**
When GitHub Actions checks out a tag (during release workflow), it creates a detached HEAD state. The command `git branch --show-current` returns empty, and attempting to push fails because there's no current branch.

**Solution:**
1. Check if in detached HEAD state before attempting to push
2. If detached, checkout the main branch first
3. Always specify the target branch explicitly when pushing

**Fixed Workflow:**
```yaml
# In checkout step
- uses: actions/checkout@v4
  with:
    fetch-depth: 0  # Fetch all history for changelog
    token: ${{ secrets.GITHUB_TOKEN }}

# In commit step
if [ -z "$(git branch --show-current)" ]; then
  echo "In detached HEAD state - checking out main branch"
  git fetch origin main
  git checkout main
  git pull origin main
fi

# Push to specific branch
git push origin main
```

**Key Learnings:**
- Tag checkouts in GitHub Actions create detached HEAD states
- Always check branch state before pushing in workflows
- Use `fetch-depth: 0` to get full history for changelog generation
- Explicitly specify target branch when pushing from workflows
- Consider if changelog updates should be part of release workflow or done separately

**Additional Fix - Version Update Conflicts:**
When the workflow updates version in build.gradle.kts, it creates local changes that prevent branch switching. Solution:
```bash
# Reset build.gradle.kts before switching branches
git checkout -- app/build.gradle.kts || true
```
This ensures only CHANGELOG.md is committed back to the repository, while version updates are used only for the build process.

### Issue: ANSI color codes appearing in GitHub release notes
**Date:** 2025-01-16
**Error:** Release notes showing escape sequences like `�[0;34m[INFO]�[0m`

**Root Cause:**
The changelog generation script was outputting ANSI color codes even when output was redirected to a file. These escape sequences were then included in the release notes.

**Solutions:**
1. Detect when output is not to a terminal using `[ -t 1 ]`
2. Check for NO_COLOR environment variable
3. Redirect status messages to stderr instead of stdout
4. Set NO_COLOR=1 when calling the script in workflows

**Fixed Script Logic:**
```bash
# Check if output is to a terminal
if [ -t 1 ] && [ -z "${NO_COLOR}" ]; then
    # Enable colors
    BLUE='\033[0;34m'
    NC='\033[0m'
else
    # Disable colors
    BLUE=''
    NC=''
fi

# Print status to stderr, not stdout
echo "[INFO] $1" >&2
```

**Workflow Usage:**
```bash
NO_COLOR=1 ./scripts/generate-changelog.sh --incremental "$VERSION" > changelog_entry.txt
```

**Key Learnings:**
- Always check if output is to a terminal before using ANSI colors
- Status messages should go to stderr, actual output to stdout
- Set NO_COLOR=1 environment variable in CI/CD pipelines
- Test scripts with redirected output to catch color code issues

### Issue: Untracked CHANGELOG.md blocking branch checkout
**Date:** 2025-01-16
**Error:** "The following untracked working tree files would be overwritten by checkout: CHANGELOG.md"

**Root Cause:**
When the workflow creates CHANGELOG.md for the first time in a detached HEAD state (tag checkout), the file is untracked. Attempting to checkout main branch fails if CHANGELOG.md already exists there.

**Solution:**
Use git stash with --include-untracked to handle all local changes:
```bash
# Stash all changes including untracked files
git add -A  # Stage everything first
git stash push -m "Temporary stash for branch switch" --include-untracked

# Switch branches safely
git fetch origin main
git checkout main
git pull origin main

# Restore the stashed changes
git stash pop || true  # Don't fail on conflicts

# Unstage files if needed
git reset HEAD CHANGELOG.md 2>/dev/null || true
```

This approach is more robust because:
- Git stash handles both tracked and untracked files
- It preserves file content even if conflicts occur
- Works regardless of whether files exist in the target branch

**Key Learnings:**
- Always check if files are tracked before switching branches
- Use `git ls-files --error-unmatch` to check if a file is tracked
- Temporarily save untracked files that might cause conflicts
- Clean up temporary files after restoration
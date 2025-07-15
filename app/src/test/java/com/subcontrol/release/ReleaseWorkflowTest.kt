package com.subcontrol.release

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Integration tests for the release workflow functionality.
 * These tests validate the complete release process including file operations,
 * version updates, and APK generation workflow.
 */
class ReleaseWorkflowTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var releaseWorkflow: ReleaseWorkflow

    @Before
    fun setUp() {
        releaseWorkflow = ReleaseWorkflow()
    }

    @Test
    fun `updateVersionInGradleFile should update version correctly`() {
        // Create a temporary gradle file
        val gradleFile = temporaryFolder.newFile("build.gradle.kts")
        gradleFile.writeText("""
            plugins {
                alias(libs.plugins.android.application)
            }
            
            android {
                defaultConfig {
                    versionCode = 1
                    versionName = "1.0.0"
                }
            }
        """.trimIndent())

        // Update version
        releaseWorkflow.updateVersionInGradleFile(gradleFile, "1.2.3", 10203)

        // Verify update
        val updatedContent = gradleFile.readText()
        assertTrue(updatedContent.contains("versionCode = 10203"))
        assertTrue(updatedContent.contains("versionName = \"1.2.3\""))
    }

    @Test
    fun `extractReleaseNotes should extract version-specific notes`() {
        // Create a temporary release notes file
        val releaseNotesFile = temporaryFolder.newFile("RELEASE_NOTES.md")
        releaseNotesFile.writeText("""
            # Release Notes

            ## [v1.2.3] - 2025-01-15

            ### Added
            - New feature for version 1.2.3
            - Another improvement

            ### Fixed
            - Bug fix for this version

            ## [v1.2.2] - 2025-01-14

            ### Fixed
            - Previous bug fix

            ## [v1.2.1] - 2025-01-13

            ### Added
            - Previous feature
        """.trimIndent())

        // Extract release notes for version 1.2.3
        val releaseNotes = releaseWorkflow.extractReleaseNotes(releaseNotesFile, "1.2.3")

        // Verify extraction
        assertTrue(releaseNotes.contains("New feature for version 1.2.3"))
        assertTrue(releaseNotes.contains("Another improvement"))
        assertTrue(releaseNotes.contains("Bug fix for this version"))
        assertFalse(releaseNotes.contains("Previous bug fix"))
        assertFalse(releaseNotes.contains("Previous feature"))
    }

    @Test
    fun `extractReleaseNotes should return default message when version not found`() {
        // Create a temporary release notes file
        val releaseNotesFile = temporaryFolder.newFile("RELEASE_NOTES.md")
        releaseNotesFile.writeText("""
            # Release Notes

            ## [v1.2.2] - 2025-01-14

            ### Fixed
            - Some bug fix
        """.trimIndent())

        // Extract release notes for version that doesn't exist
        val releaseNotes = releaseWorkflow.extractReleaseNotes(releaseNotesFile, "1.2.3")

        // Verify default message
        assertTrue(releaseNotes.contains("Release version 1.2.3 of SubControl"))
        assertTrue(releaseNotes.contains("Privacy-focused Android Subscription Manager"))
    }

    @Test
    fun `extractReleaseNotes should return default message when file doesn't exist`() {
        val nonExistentFile = File(temporaryFolder.root, "nonexistent.md")

        // Extract release notes from non-existent file
        val releaseNotes = releaseWorkflow.extractReleaseNotes(nonExistentFile, "1.2.3")

        // Verify default message
        assertTrue(releaseNotes.contains("Release version 1.2.3 of SubControl"))
        assertTrue(releaseNotes.contains("Privacy-focused Android Subscription Manager"))
    }

    @Test
    fun `validateApkName should validate correct APK names`() {
        // Test valid APK names
        assertTrue(releaseWorkflow.validateApkName("SubControl-v1.0.0.apk"))
        assertTrue(releaseWorkflow.validateApkName("SubControl-v1.2.3.apk"))
        assertTrue(releaseWorkflow.validateApkName("SubControl-v10.20.30.apk"))
        assertTrue(releaseWorkflow.validateApkName("SubControl-v999.999.999.apk"))

        // Test invalid APK names
        assertFalse(releaseWorkflow.validateApkName("SubControl-1.0.0.apk")) // Missing 'v'
        assertFalse(releaseWorkflow.validateApkName("SubControl-v1.0.apk")) // Missing patch version
        assertFalse(releaseWorkflow.validateApkName("SubControl-v1.0.0.0.apk")) // Too many version parts
        assertFalse(releaseWorkflow.validateApkName("subcontrol-v1.0.0.apk")) // Wrong case
        assertFalse(releaseWorkflow.validateApkName("SubControl-v1.0.0.aar")) // Wrong extension
        assertFalse(releaseWorkflow.validateApkName("OtherApp-v1.0.0.apk")) // Wrong app name
    }

    @Test
    fun `generateReleaseMetadata should create correct metadata`() {
        val metadata = releaseWorkflow.generateReleaseMetadata(
            version = "1.2.3",
            versionCode = 10203,
            apkName = "SubControl-v1.2.3.apk",
            releaseNotes = "Test release notes"
        )

        // Verify metadata structure
        assertEquals("1.2.3", metadata.version)
        assertEquals(10203, metadata.versionCode)
        assertEquals("SubControl-v1.2.3.apk", metadata.apkName)
        assertEquals("Test release notes", metadata.releaseNotes)
        assertEquals("v1.2.3", metadata.tagName)
        assertEquals("SubControl 1.2.3", metadata.releaseName)
        assertNotNull(metadata.timestamp)
    }

    @Test
    fun `calculateApkSize should return correct size`() {
        // Create a test file with known content
        val testFile = temporaryFolder.newFile("test.apk")
        testFile.writeText("Test content for APK size calculation")

        val size = releaseWorkflow.calculateApkSize(testFile)

        // Verify size calculation
        assertEquals(testFile.length(), size)
        assertTrue(size > 0)
    }

    @Test
    fun `validateVersionProgression should validate version increases`() {
        // Test valid progressions
        assertTrue(releaseWorkflow.validateVersionProgression("1.0.0", "1.0.1"))
        assertTrue(releaseWorkflow.validateVersionProgression("1.0.0", "1.1.0"))
        assertTrue(releaseWorkflow.validateVersionProgression("1.0.0", "2.0.0"))
        assertTrue(releaseWorkflow.validateVersionProgression("1.2.3", "1.2.4"))

        // Test invalid progressions
        assertFalse(releaseWorkflow.validateVersionProgression("1.0.1", "1.0.0")) // Downgrade
        assertFalse(releaseWorkflow.validateVersionProgression("1.0.0", "1.0.0")) // Same version
        assertFalse(releaseWorkflow.validateVersionProgression("2.0.0", "1.0.0")) // Major downgrade
    }

    @Test
    fun `isValidAndroidVersionCode should validate version code constraints`() {
        // Test valid version codes
        assertTrue(releaseWorkflow.isValidAndroidVersionCode(1))
        assertTrue(releaseWorkflow.isValidAndroidVersionCode(10203))
        assertTrue(releaseWorkflow.isValidAndroidVersionCode(999999))
        assertTrue(releaseWorkflow.isValidAndroidVersionCode(2100000000))

        // Test invalid version codes
        assertFalse(releaseWorkflow.isValidAndroidVersionCode(0))
        assertFalse(releaseWorkflow.isValidAndroidVersionCode(-1))
        assertFalse(releaseWorkflow.isValidAndroidVersionCode(2100000001))
    }

    @Test
    fun `createReleaseTag should generate correct tag format`() {
        assertEquals("v1.0.0", releaseWorkflow.createReleaseTag("1.0.0"))
        assertEquals("v1.2.3", releaseWorkflow.createReleaseTag("1.2.3"))
        assertEquals("v10.20.30", releaseWorkflow.createReleaseTag("10.20.30"))
    }

    @Test
    fun `createReleaseName should generate correct release name`() {
        assertEquals("SubControl 1.0.0", releaseWorkflow.createReleaseName("1.0.0"))
        assertEquals("SubControl 1.2.3", releaseWorkflow.createReleaseName("1.2.3"))
        assertEquals("SubControl 10.20.30", releaseWorkflow.createReleaseName("10.20.30"))
    }

    @Test
    fun `parseVersionFromTag should handle various tag formats`() {
        assertEquals("1.0.0", releaseWorkflow.parseVersionFromTag("v1.0.0"))
        assertEquals("1.2.3", releaseWorkflow.parseVersionFromTag("v1.2.3"))
        assertEquals("10.20.30", releaseWorkflow.parseVersionFromTag("v10.20.30"))
        assertEquals("1.0.0", releaseWorkflow.parseVersionFromTag("1.0.0"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseVersionFromTag should throw exception for invalid tag`() {
        releaseWorkflow.parseVersionFromTag("invalid-tag")
    }

    @Test
    fun `validateGradleFileUpdate should verify successful update`() {
        // Create a temporary gradle file
        val gradleFile = temporaryFolder.newFile("build.gradle.kts")
        gradleFile.writeText("""
            android {
                defaultConfig {
                    versionCode = 10203
                    versionName = "1.2.3"
                }
            }
        """.trimIndent())

        // Validate the update
        assertTrue(releaseWorkflow.validateGradleFileUpdate(gradleFile, "1.2.3", 10203))

        // Test with wrong version
        assertFalse(releaseWorkflow.validateGradleFileUpdate(gradleFile, "1.2.4", 10203))
        assertFalse(releaseWorkflow.validateGradleFileUpdate(gradleFile, "1.2.3", 10204))
    }
}

/**
 * Release workflow utility class that handles the complete release process.
 * This class provides methods for updating versions, extracting release notes,
 * and managing the overall release workflow.
 */
class ReleaseWorkflow {

    private val versionParser = VersionParser()

    /**
     * Updates the version information in a Gradle build file.
     * 
     * @param gradleFile The build.gradle.kts file to update
     * @param version The new version string (e.g., "1.2.3")
     * @param versionCode The new version code (e.g., 10203)
     */
    fun updateVersionInGradleFile(gradleFile: File, version: String, versionCode: Int) {
        val content = gradleFile.readText()
        val updatedContent = content
            .replace(Regex("versionCode\\s*=\\s*\\d+"), "versionCode = $versionCode")
            .replace(Regex("versionName\\s*=\\s*\"[^\"]*\""), "versionName = \"$version\"")
        
        gradleFile.writeText(updatedContent)
    }

    /**
     * Extracts release notes for a specific version from the RELEASE_NOTES.md file.
     * 
     * @param releaseNotesFile The RELEASE_NOTES.md file
     * @param version The version to extract notes for
     * @return The extracted release notes or a default message
     */
    fun extractReleaseNotes(releaseNotesFile: File, version: String): String {
        if (!releaseNotesFile.exists()) {
            return "Release version $version of SubControl - Privacy-focused Android Subscription Manager"
        }

        val content = releaseNotesFile.readText()
        val versionPattern = Regex("## \\[?v?$version\\]?[^\\n]*\\n([\\s\\S]*?)(?=## \\[?v?[\\d.]+\\]?|$)")
        val match = versionPattern.find(content)

        return if (match != null) {
            match.groupValues[1].trim()
        } else {
            // If no specific version found, use a default message
            "Release version $version of SubControl - Privacy-focused Android Subscription Manager"
        }
    }

    /**
     * Validates that an APK name follows the expected format.
     * 
     * @param apkName The APK filename to validate
     * @return true if the name is valid, false otherwise
     */
    fun validateApkName(apkName: String): Boolean {
        val pattern = Regex("^SubControl-v\\d+\\.\\d+\\.\\d+\\.apk$")
        return pattern.matches(apkName)
    }

    /**
     * Generates metadata for a release.
     * 
     * @param version The version string
     * @param versionCode The version code
     * @param apkName The APK filename
     * @param releaseNotes The release notes
     * @return A ReleaseMetadata object
     */
    fun generateReleaseMetadata(
        version: String,
        versionCode: Int,
        apkName: String,
        releaseNotes: String
    ): ReleaseMetadata {
        return ReleaseMetadata(
            version = version,
            versionCode = versionCode,
            apkName = apkName,
            releaseNotes = releaseNotes,
            tagName = "v$version",
            releaseName = "SubControl $version",
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Calculates the size of an APK file.
     * 
     * @param apkFile The APK file
     * @return The size in bytes
     */
    fun calculateApkSize(apkFile: File): Long {
        return apkFile.length()
    }

    /**
     * Validates that a new version is higher than the current version.
     * 
     * @param currentVersion The current version
     * @param newVersion The new version
     * @return true if the new version is higher, false otherwise
     */
    fun validateVersionProgression(currentVersion: String, newVersion: String): Boolean {
        return versionParser.compareVersions(currentVersion, newVersion) < 0
    }

    /**
     * Validates that a version code is within Android's valid range.
     * 
     * @param versionCode The version code to validate
     * @return true if valid, false otherwise
     */
    fun isValidAndroidVersionCode(versionCode: Int): Boolean {
        return versionCode in 1..2100000000
    }

    /**
     * Creates a release tag from a version string.
     * 
     * @param version The version string
     * @return The release tag (e.g., "v1.0.0")
     */
    fun createReleaseTag(version: String): String {
        return "v$version"
    }

    /**
     * Creates a release name from a version string.
     * 
     * @param version The version string
     * @return The release name (e.g., "SubControl 1.0.0")
     */
    fun createReleaseName(version: String): String {
        return "SubControl $version"
    }

    /**
     * Parses a version string from a git tag.
     * 
     * @param tag The git tag
     * @return The parsed version string
     */
    fun parseVersionFromTag(tag: String): String {
        return versionParser.parseVersion(tag)
    }

    /**
     * Validates that a Gradle file was successfully updated with the correct version.
     * 
     * @param gradleFile The Gradle file to validate
     * @param expectedVersion The expected version string
     * @param expectedVersionCode The expected version code
     * @return true if the update was successful, false otherwise
     */
    fun validateGradleFileUpdate(gradleFile: File, expectedVersion: String, expectedVersionCode: Int): Boolean {
        val content = gradleFile.readText()
        val actualVersion = versionParser.getVersionFromGradleFile(content)
        val actualVersionCode = versionParser.getVersionCodeFromGradleFile(content)
        
        return actualVersion == expectedVersion && actualVersionCode == expectedVersionCode
    }
}

/**
 * Data class representing release metadata.
 */
data class ReleaseMetadata(
    val version: String,
    val versionCode: Int,
    val apkName: String,
    val releaseNotes: String,
    val tagName: String,
    val releaseName: String,
    val timestamp: Long
)
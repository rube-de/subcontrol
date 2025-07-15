package com.subcontrol.release

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * Test class for version parsing and validation logic used in the release process.
 * These tests validate the version parsing logic used by the GitHub Actions workflow.
 */
class VersionParsingTest {

    private lateinit var versionParser: VersionParser

    @Before
    fun setUp() {
        versionParser = VersionParser()
    }

    @Test
    fun `parseVersion should extract version from valid tag`() {
        // Test valid version tags
        assertEquals("1.0.0", versionParser.parseVersion("v1.0.0"))
        assertEquals("1.2.3", versionParser.parseVersion("v1.2.3"))
        assertEquals("10.20.30", versionParser.parseVersion("v10.20.30"))
        assertEquals("0.0.1", versionParser.parseVersion("v0.0.1"))
    }

    @Test
    fun `parseVersion should handle tags without v prefix`() {
        // Test version tags without 'v' prefix
        assertEquals("1.0.0", versionParser.parseVersion("1.0.0"))
        assertEquals("2.1.0", versionParser.parseVersion("2.1.0"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseVersion should throw exception for invalid format`() {
        versionParser.parseVersion("invalid-version")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseVersion should throw exception for empty string`() {
        versionParser.parseVersion("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseVersion should throw exception for null input`() {
        versionParser.parseVersion(null)
    }

    @Test
    fun `calculateVersionCode should generate correct version codes`() {
        // Test version code calculation: MAJOR*10000 + MINOR*100 + PATCH
        assertEquals(10000, versionParser.calculateVersionCode("1.0.0"))
        assertEquals(10203, versionParser.calculateVersionCode("1.2.3"))
        assertEquals(20100, versionParser.calculateVersionCode("2.1.0"))
        assertEquals(100200, versionParser.calculateVersionCode("10.2.0"))
        assertEquals(1, versionParser.calculateVersionCode("0.0.1"))
    }

    @Test
    fun `isValidSemanticVersion should validate semantic version format`() {
        // Valid semantic versions
        assertTrue(versionParser.isValidSemanticVersion("1.0.0"))
        assertTrue(versionParser.isValidSemanticVersion("0.0.1"))
        assertTrue(versionParser.isValidSemanticVersion("10.20.30"))
        assertTrue(versionParser.isValidSemanticVersion("999.999.999"))

        // Invalid semantic versions
        assertFalse(versionParser.isValidSemanticVersion("1.0"))
        assertFalse(versionParser.isValidSemanticVersion("1.0.0.0"))
        assertFalse(versionParser.isValidSemanticVersion("1.0.0-alpha"))
        assertFalse(versionParser.isValidSemanticVersion("v1.0.0"))
        assertFalse(versionParser.isValidSemanticVersion("1.0.0-SNAPSHOT"))
    }

    @Test
    fun `compareVersions should correctly compare semantic versions`() {
        // Test version comparison
        assertTrue(versionParser.compareVersions("1.0.0", "1.0.1") < 0)
        assertTrue(versionParser.compareVersions("1.0.1", "1.0.0") > 0)
        assertTrue(versionParser.compareVersions("1.0.0", "1.0.0") == 0)
        
        assertTrue(versionParser.compareVersions("1.0.0", "1.1.0") < 0)
        assertTrue(versionParser.compareVersions("1.1.0", "1.0.0") > 0)
        
        assertTrue(versionParser.compareVersions("1.0.0", "2.0.0") < 0)
        assertTrue(versionParser.compareVersions("2.0.0", "1.0.0") > 0)
    }

    @Test
    fun `generateApkName should create correct APK filename`() {
        assertEquals("SubControl-v1.0.0.apk", versionParser.generateApkName("1.0.0"))
        assertEquals("SubControl-v1.2.3.apk", versionParser.generateApkName("1.2.3"))
        assertEquals("SubControl-v10.20.30.apk", versionParser.generateApkName("10.20.30"))
    }

    @Test
    fun `extractVersionFromApkName should extract version from APK filename`() {
        assertEquals("1.0.0", versionParser.extractVersionFromApkName("SubControl-v1.0.0.apk"))
        assertEquals("1.2.3", versionParser.extractVersionFromApkName("SubControl-v1.2.3.apk"))
        assertEquals("10.20.30", versionParser.extractVersionFromApkName("SubControl-v10.20.30.apk"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `extractVersionFromApkName should throw exception for invalid filename`() {
        versionParser.extractVersionFromApkName("invalid-filename.apk")
    }

    @Test
    fun `isPreRelease should identify pre-release versions`() {
        // These should be false for our release process (we don't support pre-releases)
        assertFalse(versionParser.isPreRelease("1.0.0"))
        assertFalse(versionParser.isPreRelease("1.2.3"))
        assertFalse(versionParser.isPreRelease("10.20.30"))
    }

    @Test
    fun `validateVersionCode should ensure version code is within valid range`() {
        // Version codes should be within Android's valid range (1 to 2100000000)
        assertTrue(versionParser.validateVersionCode(1))
        assertTrue(versionParser.validateVersionCode(10000))
        assertTrue(versionParser.validateVersionCode(999999999))
        assertTrue(versionParser.validateVersionCode(2100000000))
        
        assertFalse(versionParser.validateVersionCode(0))
        assertFalse(versionParser.validateVersionCode(-1))
        assertFalse(versionParser.validateVersionCode(2100000001))
    }

    @Test
    fun `getVersionFromGradleFile should extract version from build gradle content`() {
        val gradleContent = """
            plugins {
                id 'com.android.application'
            }
            
            android {
                defaultConfig {
                    versionCode 10203
                    versionName "1.2.3"
                }
            }
        """.trimIndent()
        
        assertEquals("1.2.3", versionParser.getVersionFromGradleFile(gradleContent))
    }

    @Test
    fun `getVersionCodeFromGradleFile should extract version code from build gradle content`() {
        val gradleContent = """
            plugins {
                id 'com.android.application'
            }
            
            android {
                defaultConfig {
                    versionCode 10203
                    versionName "1.2.3"
                }
            }
        """.trimIndent()
        
        assertEquals(10203, versionParser.getVersionCodeFromGradleFile(gradleContent))
    }
}

/**
 * Version parser utility class for handling version-related operations.
 * This class provides methods for parsing, validating, and manipulating version strings.
 */
class VersionParser {
    
    private val semanticVersionRegex = Regex("^(\\d+)\\.(\\d+)\\.(\\d+)$")
    private val tagVersionRegex = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)$")
    private val apkNameRegex = Regex("^SubControl-v(\\d+\\.\\d+\\.\\d+)\\.apk$")
    
    /**
     * Parses a version string from a git tag, removing the 'v' prefix if present.
     * 
     * @param tag The git tag (e.g., "v1.0.0" or "1.0.0")
     * @return The parsed version string (e.g., "1.0.0")
     * @throws IllegalArgumentException if the tag format is invalid
     */
    fun parseVersion(tag: String?): String {
        if (tag.isNullOrEmpty()) {
            throw IllegalArgumentException("Tag cannot be null or empty")
        }
        
        val match = tagVersionRegex.find(tag)
            ?: throw IllegalArgumentException("Invalid tag format: $tag")
        
        return "${match.groupValues[1]}.${match.groupValues[2]}.${match.groupValues[3]}"
    }
    
    /**
     * Calculates the Android version code from a semantic version string.
     * Formula: MAJOR*10000 + MINOR*100 + PATCH
     * 
     * @param version The semantic version string (e.g., "1.2.3")
     * @return The calculated version code (e.g., 10203)
     * @throws IllegalArgumentException if the version format is invalid
     */
    fun calculateVersionCode(version: String): Int {
        if (!isValidSemanticVersion(version)) {
            throw IllegalArgumentException("Invalid semantic version format: $version")
        }
        
        val parts = version.split(".")
        val major = parts[0].toInt()
        val minor = parts[1].toInt()
        val patch = parts[2].toInt()
        
        return major * 10000 + minor * 100 + patch
    }
    
    /**
     * Validates that a string follows semantic versioning format (X.Y.Z).
     * 
     * @param version The version string to validate
     * @return true if the version is valid semantic version, false otherwise
     */
    fun isValidSemanticVersion(version: String): Boolean {
        return semanticVersionRegex.matches(version)
    }
    
    /**
     * Compares two semantic version strings.
     * 
     * @param version1 The first version string
     * @param version2 The second version string
     * @return Negative if version1 < version2, positive if version1 > version2, 0 if equal
     */
    fun compareVersions(version1: String, version2: String): Int {
        val v1Parts = version1.split(".").map { it.toInt() }
        val v2Parts = version2.split(".").map { it.toInt() }
        
        for (i in 0 until maxOf(v1Parts.size, v2Parts.size)) {
            val v1Part = v1Parts.getOrElse(i) { 0 }
            val v2Part = v2Parts.getOrElse(i) { 0 }
            
            if (v1Part != v2Part) {
                return v1Part - v2Part
            }
        }
        
        return 0
    }
    
    /**
     * Generates the APK filename for a given version.
     * 
     * @param version The version string (e.g., "1.0.0")
     * @return The APK filename (e.g., "SubControl-v1.0.0.apk")
     */
    fun generateApkName(version: String): String {
        return "SubControl-v$version.apk"
    }
    
    /**
     * Extracts the version from an APK filename.
     * 
     * @param apkName The APK filename (e.g., "SubControl-v1.0.0.apk")
     * @return The extracted version string (e.g., "1.0.0")
     * @throws IllegalArgumentException if the filename format is invalid
     */
    fun extractVersionFromApkName(apkName: String): String {
        val match = apkNameRegex.find(apkName)
            ?: throw IllegalArgumentException("Invalid APK filename format: $apkName")
        
        return match.groupValues[1]
    }
    
    /**
     * Checks if a version is a pre-release version.
     * For SubControl, we don't support pre-releases, so this always returns false.
     * 
     * @param version The version string to check
     * @return false (SubControl doesn't support pre-releases)
     */
    fun isPreRelease(version: String): Boolean {
        return false
    }
    
    /**
     * Validates that a version code is within Android's valid range.
     * 
     * @param versionCode The version code to validate
     * @return true if the version code is valid, false otherwise
     */
    fun validateVersionCode(versionCode: Int): Boolean {
        return versionCode in 1..2100000000
    }
    
    /**
     * Extracts the version name from build.gradle file content.
     * 
     * @param gradleContent The content of the build.gradle file
     * @return The extracted version name
     */
    fun getVersionFromGradleFile(gradleContent: String): String {
        // Support both Groovy (versionName "1.2.3") and Kotlin DSL (versionName = "1.2.3") formats
        val versionNameRegex = Regex("versionName\\s*=?\\s*\"([^\"]+)\"")
        val match = versionNameRegex.find(gradleContent)
            ?: throw IllegalArgumentException("versionName not found in gradle file")
        
        return match.groupValues[1]
    }
    
    /**
     * Extracts the version code from build.gradle file content.
     * 
     * @param gradleContent The content of the build.gradle file
     * @return The extracted version code
     */
    fun getVersionCodeFromGradleFile(gradleContent: String): Int {
        // Support both Groovy (versionCode 123) and Kotlin DSL (versionCode = 123) formats
        val versionCodeRegex = Regex("versionCode\\s*=?\\s*(\\d+)")
        val match = versionCodeRegex.find(gradleContent)
            ?: throw IllegalArgumentException("versionCode not found in gradle file")
        
        return match.groupValues[1].toInt()
    }
}
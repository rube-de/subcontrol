package com.subcontrol.release

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Test class for changelog generation functionality.
 * These tests validate the changelog generation logic used by the release process.
 */
class ChangelogGenerationTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var changelogGenerator: ChangelogGenerator

    @Before
    fun setUp() {
        changelogGenerator = ChangelogGenerator()
    }

    @Test
    fun `categorizeCommit should categorize feat commits correctly`() {
        val commit = "feat: add new subscription management feature"
        val hash = "abc123def456"
        
        val result = changelogGenerator.categorizeCommit(commit, hash)
        
        assertEquals("Added", result.category)
        assertEquals("add new subscription management feature (abc123d)", result.entry)
    }

    @Test
    fun `categorizeCommit should categorize fix commits correctly`() {
        val commit = "fix: resolve crash when deleting subscription"
        val hash = "def456ghi789"
        
        val result = changelogGenerator.categorizeCommit(commit, hash)
        
        assertEquals("Fixed", result.category)
        assertEquals("resolve crash when deleting subscription (def456g)", result.entry)
    }

    @Test
    fun `categorizeCommit should categorize conventional commits with scope`() {
        val commit = "feat(ui): add dark mode support"
        val hash = "ghi789jkl012"
        
        val result = changelogGenerator.categorizeCommit(commit, hash)
        
        assertEquals("Added", result.category)
        assertEquals("add dark mode support (ghi789j)", result.entry)
    }

    @Test
    fun `categorizeCommit should categorize chore commits correctly`() {
        val commit = "chore: update dependencies"
        val hash = "jkl012mno345"
        
        val result = changelogGenerator.categorizeCommit(commit, hash)
        
        assertEquals("Changed", result.category)
        assertEquals("update dependencies (jkl012m)", result.entry)
    }

    @Test
    fun `categorizeCommit should categorize breaking changes`() {
        val commit = "breaking: remove deprecated API endpoints"
        val hash = "mno345pqr678"
        
        val result = changelogGenerator.categorizeCommit(commit, hash)
        
        assertEquals("Breaking Changes", result.category)
        assertEquals("remove deprecated API endpoints (mno345p)", result.entry)
    }

    @Test
    fun `categorizeCommit should categorize based on keywords when no conventional format`() {
        val commit = "Add new encryption algorithm"
        val hash = "pqr678stu901"
        
        val result = changelogGenerator.categorizeCommit(commit, hash)
        
        assertEquals("Added", result.category)
        assertEquals("Add new encryption algorithm (pqr678s)", result.entry)
    }

    @Test
    fun `categorizeCommit should categorize fix keywords`() {
        val commit = "Bug fix for notification scheduling"
        val hash = "stu901vwx234"
        
        val result = changelogGenerator.categorizeCommit(commit, hash)
        
        assertEquals("Fixed", result.category)
        assertEquals("Bug fix for notification scheduling (stu901v)", result.entry)
    }

    @Test
    fun `categorizeCommit should categorize security commits`() {
        val commit = "Security: fix vulnerability in data encryption"
        val hash = "vwx234yzz567"
        
        val result = changelogGenerator.categorizeCommit(commit, hash)
        
        assertEquals("Security", result.category)
        assertEquals("Security: fix vulnerability in data encryption (vwx234y)", result.entry)
    }

    @Test
    fun `categorizeCommit should default to Changed for unclear commits`() {
        val commit = "Some unclear commit message"
        val hash = "yzz567abc890"
        
        val result = changelogGenerator.categorizeCommit(commit, hash)
        
        assertEquals("Changed", result.category)
        assertEquals("Some unclear commit message (yzz567a)", result.entry)
    }

    @Test
    fun `generateChangelogHeader should create correct header format`() {
        val version = "1.2.3"
        val date = "2025-01-15"
        
        val header = changelogGenerator.generateChangelogHeader(version, date)
        
        assertEquals("## [1.2.3] - 2025-01-15", header)
    }

    @Test
    fun `parseVersion should extract version from tag`() {
        assertEquals("1.2.3", changelogGenerator.parseVersion("v1.2.3"))
        assertEquals("1.0.0", changelogGenerator.parseVersion("v1.0.0"))
        assertEquals("10.20.30", changelogGenerator.parseVersion("v10.20.30"))
    }

    @Test
    fun `parseVersion should handle tag without v prefix`() {
        assertEquals("1.2.3", changelogGenerator.parseVersion("1.2.3"))
        assertEquals("2.0.0", changelogGenerator.parseVersion("2.0.0"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseVersion should throw exception for invalid tag`() {
        changelogGenerator.parseVersion("invalid-tag")
    }

    @Test
    fun `filterReleaseCommits should exclude release commits`() {
        val commits = listOf(
            GitCommit("abc123", "feat: add new feature"),
            GitCommit("def456", "Release version 1.2.3"),
            GitCommit("ghi789", "fix: bug fix"),
            GitCommit("jkl012", "Bump version to 1.2.4"),
            GitCommit("mno345", "chore: update dependencies")
        )
        
        val filtered = changelogGenerator.filterReleaseCommits(commits)
        
        assertEquals(3, filtered.size)
        assertFalse(filtered.any { it.message.contains("Release") })
        assertFalse(filtered.any { it.message.contains("Bump version") })
    }

    @Test
    fun `groupCommitsByCategory should group commits correctly`() {
        val commits = listOf(
            GitCommit("abc1234", "feat: add feature A"),
            GitCommit("def4567", "feat: add feature B"),
            GitCommit("ghi7890", "fix: fix bug A"),
            GitCommit("jkl0123", "fix: fix bug B"),
            GitCommit("mno3456", "chore: update deps")
        )
        
        val grouped = changelogGenerator.groupCommitsByCategory(commits)
        
        assertEquals(2, grouped["Added"]?.size)
        assertEquals(2, grouped["Fixed"]?.size)
        assertEquals(1, grouped["Changed"]?.size)
    }

    @Test
    fun `formatChangelogSection should format correctly`() {
        val entries = listOf(
            "add new feature (abc123d)",
            "add another feature (def456g)"
        )
        
        val formatted = changelogGenerator.formatChangelogSection("Added", entries)
        
        val expected = """### Added
- add new feature (abc123d)
- add another feature (def456g)

"""
        
        assertEquals(expected, formatted)
    }

    @Test
    fun `isValidSemanticVersion should validate version strings`() {
        assertTrue(changelogGenerator.isValidSemanticVersion("1.0.0"))
        assertTrue(changelogGenerator.isValidSemanticVersion("1.2.3"))
        assertTrue(changelogGenerator.isValidSemanticVersion("10.20.30"))
        
        assertFalse(changelogGenerator.isValidSemanticVersion("1.0"))
        assertFalse(changelogGenerator.isValidSemanticVersion("1.0.0.0"))
        assertFalse(changelogGenerator.isValidSemanticVersion("v1.0.0"))
        assertFalse(changelogGenerator.isValidSemanticVersion("1.0.0-alpha"))
    }

    @Test
    fun `generateFullChangelog should create complete changelog`() {
        // Create temporary changelog file
        val changelogFile = temporaryFolder.newFile("CHANGELOG.md")
        
        // Mock git commits data
        val mockCommits = mapOf(
            "v1.0.0" to listOf(
                GitCommit("abc1234567", "feat: initial release"),
                GitCommit("def4567890", "feat: add subscription management")
            ),
            "v1.1.0" to listOf(
                GitCommit("ghi7890123", "feat: add notification system"),
                GitCommit("jkl0123456", "fix: resolve memory leak")
            )
        )
        
        changelogGenerator.generateFullChangelog(changelogFile, mockCommits)
        
        val content = changelogFile.readText()
        
        // Verify changelog structure
        assertTrue(content.contains("# Changelog"))
        assertTrue(content.contains("## [1.0.0]"))
        assertTrue(content.contains("## [1.1.0]"))
        assertTrue(content.contains("### Added"))
        assertTrue(content.contains("### Fixed"))
        assertTrue(content.contains("initial release"))
        assertTrue(content.contains("add notification system"))
        assertTrue(content.contains("resolve memory leak"))
    }

    @Test
    fun `updateChangelog should prepend new version`() {
        // Create existing changelog
        val changelogFile = temporaryFolder.newFile("CHANGELOG.md")
        changelogFile.writeText("""
            # Changelog
            
            ## [1.0.0] - 2025-01-14
            
            ### Added
            - Initial release
            
        """.trimIndent())
        
        // Mock new version data
        val newVersionCommits = listOf(
            GitCommit("abc1234567", "feat: add new feature"),
            GitCommit("def4567890", "fix: important bug fix")
        )
        
        changelogGenerator.updateChangelog(changelogFile, "1.1.0", newVersionCommits, "2025-01-15")
        
        val content = changelogFile.readText()
        
        // Verify new version is at the top
        val lines = content.lines()
        val firstVersionLine = lines.indexOfFirst { it.startsWith("## [") }
        assertTrue(lines[firstVersionLine].contains("## [1.1.0] - 2025-01-15"))
        
        // Verify old version is still there
        assertTrue(content.contains("## [1.0.0] - 2025-01-14"))
        assertTrue(content.contains("Initial release"))
    }
}

/**
 * Changelog generator utility class for handling changelog generation operations.
 * This class provides methods for categorizing commits, formatting changelog entries,
 * and managing changelog files.
 */
class ChangelogGenerator {

    private val semanticVersionRegex = Regex("^(\\d+)\\.(\\d+)\\.(\\d+)$")
    private val tagVersionRegex = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)$")

    /**
     * Categorizes a commit message and returns category and formatted entry.
     * 
     * @param commitMessage The commit message to categorize
     * @param commitHash The commit hash for reference
     * @return CommitCategory containing category and formatted entry
     */
    fun categorizeCommit(commitMessage: String, commitHash: String): CommitCategory {
        val shortHash = commitHash.substring(0, 7)
        
        // Check for conventional commit format
        when {
            commitMessage.matches(Regex("^(feat|feature)(\\([^)]+\\))?: (.+)")) -> {
                val message = commitMessage.replace(Regex("^(feat|feature)(\\([^)]+\\))?: "), "")
                return CommitCategory("Added", "$message ($shortHash)")
            }
            commitMessage.matches(Regex("^(fix|bugfix)(\\([^)]+\\))?: (.+)")) -> {
                val message = commitMessage.replace(Regex("^(fix|bugfix)(\\([^)]+\\))?: "), "")
                return CommitCategory("Fixed", "$message ($shortHash)")
            }
            commitMessage.matches(Regex("^(chore|build|ci|docs|refactor|perf|style|test)(\\([^)]+\\))?: (.+)")) -> {
                val message = commitMessage.replace(Regex("^(chore|build|ci|docs|refactor|perf|style|test)(\\([^)]+\\))?: "), "")
                return CommitCategory("Changed", "$message ($shortHash)")
            }
            commitMessage.matches(Regex("^(breaking|break)(\\([^)]+\\))?: (.+)")) -> {
                val message = commitMessage.replace(Regex("^(breaking|break)(\\([^)]+\\))?: "), "")
                return CommitCategory("Breaking Changes", "$message ($shortHash)")
            }
            else -> {
                // Categorize based on keywords
                val lowerMessage = commitMessage.toLowerCase()
                return when {
                    lowerMessage.contains(Regex("(security|secure|vulnerability|cve)")) -> 
                        CommitCategory("Security", "$commitMessage ($shortHash)")
                    lowerMessage.contains(Regex("(add|new|implement|create|introduce)")) -> 
                        CommitCategory("Added", "$commitMessage ($shortHash)")
                    lowerMessage.contains(Regex("(fix|bug|issue|resolve|patch)")) -> 
                        CommitCategory("Fixed", "$commitMessage ($shortHash)")
                    lowerMessage.contains(Regex("(update|change|modify|improve|enhance)")) -> 
                        CommitCategory("Changed", "$commitMessage ($shortHash)")
                    lowerMessage.contains(Regex("(remove|delete|drop|deprecate)")) -> 
                        CommitCategory("Removed", "$commitMessage ($shortHash)")
                    else -> 
                        CommitCategory("Changed", "$commitMessage ($shortHash)")
                }
            }
        }
    }

    /**
     * Generates a changelog header for a specific version.
     * 
     * @param version The version string
     * @param date The release date
     * @return Formatted changelog header
     */
    fun generateChangelogHeader(version: String, date: String): String {
        return "## [$version] - $date"
    }

    /**
     * Parses a version string from a git tag.
     * 
     * @param tag The git tag
     * @return The parsed version string
     * @throws IllegalArgumentException if the tag format is invalid
     */
    fun parseVersion(tag: String): String {
        val match = tagVersionRegex.find(tag)
            ?: throw IllegalArgumentException("Invalid tag format: $tag")
        
        return "${match.groupValues[1]}.${match.groupValues[2]}.${match.groupValues[3]}"
    }

    /**
     * Filters out release-related commits from a list of commits.
     * 
     * @param commits List of commits to filter
     * @return Filtered list without release commits
     */
    fun filterReleaseCommits(commits: List<GitCommit>): List<GitCommit> {
        return commits.filter { commit ->
            !commit.message.matches(Regex("^(Release|Bump|Version|v[0-9]+\\.[0-9]+\\.[0-9]+).*"))
        }
    }

    /**
     * Groups commits by their category.
     * 
     * @param commits List of commits to group
     * @return Map of category to list of commit entries
     */
    fun groupCommitsByCategory(commits: List<GitCommit>): Map<String, List<String>> {
        val categories = mutableMapOf<String, MutableList<String>>()
        
        commits.forEach { commit ->
            val categorized = categorizeCommit(commit.message, commit.hash)
            categories.getOrPut(categorized.category) { mutableListOf() }.add(categorized.entry)
        }
        
        return categories
    }

    /**
     * Formats a changelog section for a specific category.
     * 
     * @param category The category name
     * @param entries List of entries for this category
     * @return Formatted changelog section
     */
    fun formatChangelogSection(category: String, entries: List<String>): String {
        if (entries.isEmpty()) return ""
        
        val builder = StringBuilder()
        builder.appendLine("### $category")
        entries.forEach { entry ->
            builder.appendLine("- $entry")
        }
        builder.appendLine()
        
        return builder.toString()
    }

    /**
     * Validates that a string follows semantic versioning format.
     * 
     * @param version The version string to validate
     * @return true if valid, false otherwise
     */
    fun isValidSemanticVersion(version: String): Boolean {
        return semanticVersionRegex.matches(version)
    }

    /**
     * Generates a full changelog file from commit history.
     * 
     * @param changelogFile The file to write to
     * @param commitsPerVersion Map of version tags to their commits
     */
    fun generateFullChangelog(changelogFile: File, commitsPerVersion: Map<String, List<GitCommit>>) {
        val builder = StringBuilder()
        
        // Add header
        builder.appendLine("# Changelog")
        builder.appendLine()
        builder.appendLine("All notable changes to this project will be documented in this file.")
        builder.appendLine()
        builder.appendLine("The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),")
        builder.appendLine("and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).")
        builder.appendLine()
        
        // Add version sections
        commitsPerVersion.forEach { (tag, commits) ->
            val version = parseVersion(tag)
            val filteredCommits = filterReleaseCommits(commits)
            val groupedCommits = groupCommitsByCategory(filteredCommits)
            
            builder.appendLine(generateChangelogHeader(version, "2025-01-15")) // Mock date for testing
            builder.appendLine()
            
            // Add sections in order
            val sectionOrder = listOf("Breaking Changes", "Added", "Changed", "Fixed", "Removed", "Security")
            sectionOrder.forEach { category ->
                groupedCommits[category]?.let { entries ->
                    builder.append(formatChangelogSection(category, entries))
                }
            }
        }
        
        changelogFile.writeText(builder.toString())
    }

    /**
     * Updates an existing changelog file with a new version.
     * 
     * @param changelogFile The changelog file to update
     * @param newVersion The new version to add
     * @param newCommits The commits for the new version
     * @param releaseDate The release date
     */
    fun updateChangelog(changelogFile: File, newVersion: String, newCommits: List<GitCommit>, releaseDate: String) {
        val existingContent = changelogFile.readText()
        val lines = existingContent.lines().toMutableList()
        
        // Find where to insert the new version
        val insertIndex = lines.indexOfFirst { it.startsWith("## [") }
        val actualInsertIndex = if (insertIndex == -1) lines.size else insertIndex
        
        // Generate new version section
        val filteredCommits = filterReleaseCommits(newCommits)
        val groupedCommits = groupCommitsByCategory(filteredCommits)
        
        val newSection = mutableListOf<String>()
        newSection.add(generateChangelogHeader(newVersion, releaseDate))
        newSection.add("")
        
        // Add sections in order
        val sectionOrder = listOf("Breaking Changes", "Added", "Changed", "Fixed", "Removed", "Security")
        sectionOrder.forEach { category ->
            groupedCommits[category]?.let { entries ->
                newSection.addAll(formatChangelogSection(category, entries).lines())
            }
        }
        
        // Insert new section
        lines.addAll(actualInsertIndex, newSection)
        
        changelogFile.writeText(lines.joinToString("\n"))
    }
}

/**
 * Data class representing a git commit.
 */
data class GitCommit(
    val hash: String,
    val message: String
)

/**
 * Data class representing a categorized commit.
 */
data class CommitCategory(
    val category: String,
    val entry: String
)
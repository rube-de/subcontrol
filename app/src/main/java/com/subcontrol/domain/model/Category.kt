package com.subcontrol.domain.model

import java.time.LocalDateTime

/**
 * Domain model for a subscription category.
 */
data class Category(
    val id: String,
    val name: String,
    val color: String, // Hex color code
    val icon: String, // Material Design icon name
    val sortOrder: Int = 0,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        /**
         * Default categories for new users.
         */
        val defaultCategories = listOf(
            "Entertainment",
            "Software",
            "News & Media",
            "Education",
            "Health & Fitness",
            "Productivity",
            "Music",
            "Cloud Storage",
            "Gaming",
            "Other"
        )
    }
}
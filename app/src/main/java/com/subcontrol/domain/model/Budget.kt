package com.subcontrol.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Domain model for a budget.
 */
data class Budget(
    val id: String,
    val name: String,
    val monthlyLimit: BigDecimal,
    val currency: String,
    val includedCategories: List<String> = emptyList(), // Category IDs
    val includedSubscriptions: List<String> = emptyList(), // Subscription IDs
    val notificationsEnabled: Boolean = true,
    val notificationThreshold: Double = 0.8, // 80% threshold by default
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    /**
     * Calculates if the threshold has been exceeded based on current spending.
     */
    fun isThresholdExceeded(currentSpending: BigDecimal): Boolean {
        val thresholdAmount = monthlyLimit.multiply(BigDecimal(notificationThreshold))
        return currentSpending >= thresholdAmount
    }

    /**
     * Calculates if the budget limit has been exceeded.
     */
    fun isLimitExceeded(currentSpending: BigDecimal): Boolean {
        return currentSpending > monthlyLimit
    }

    /**
     * Calculates the remaining budget amount.
     */
    fun getRemainingAmount(currentSpending: BigDecimal): BigDecimal {
        return monthlyLimit.subtract(currentSpending).max(BigDecimal.ZERO)
    }

    /**
     * Calculates the percentage of budget used.
     */
    fun getUsagePercentage(currentSpending: BigDecimal): Double {
        if (monthlyLimit == BigDecimal.ZERO) return 0.0
        return currentSpending.divide(monthlyLimit, 4, BigDecimal.ROUND_HALF_UP)
            .toDouble()
            .coerceAtMost(1.0)
    }
}
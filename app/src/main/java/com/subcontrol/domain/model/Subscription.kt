package com.subcontrol.domain.model

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Domain model for a subscription.
 * 
 * This represents the business logic model for subscriptions,
 * independent of any data layer implementation.
 */
data class Subscription(
    val id: String,
    val name: String,
    val description: String = "",
    val cost: BigDecimal,
    val currency: String,
    val billingPeriod: BillingPeriod,
    val billingCycle: Int = 1,
    val startDate: LocalDate,
    val nextBillingDate: LocalDate,
    val trialEndDate: LocalDate? = null,
    val status: SubscriptionStatus,
    val notificationsEnabled: Boolean = true,
    val notificationDaysBefore: Int = 3,
    val category: String = "",
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val websiteUrl: String = "",
    val supportEmail: String = "",
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    /**
     * Checks if the subscription is currently in a trial period.
     */
    val isInTrial: Boolean
        get() = trialEndDate != null && LocalDate.now().isBefore(trialEndDate)

    /**
     * Checks if the subscription is currently active.
     */
    val isActive: Boolean
        get() = status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.TRIAL

    /**
     * Calculates the monthly cost equivalent for this subscription.
     */
    fun getMonthlyEquivalent(): BigDecimal {
        return when (billingPeriod) {
            BillingPeriod.DAILY -> cost.multiply(BigDecimal(30))
            BillingPeriod.WEEKLY -> cost.multiply(BigDecimal(4.33)) // ~4.33 weeks per month
            BillingPeriod.MONTHLY -> cost
            BillingPeriod.QUARTERLY -> cost.divide(BigDecimal(3))
            BillingPeriod.SEMI_ANNUALLY -> cost.divide(BigDecimal(6))
            BillingPeriod.ANNUALLY -> cost.divide(BigDecimal(12))
            BillingPeriod.CUSTOM -> cost.divide(BigDecimal(billingCycle))
        }
    }

    /**
     * Calculates the annual cost for this subscription.
     */
    fun getAnnualCost(): BigDecimal {
        return when (billingPeriod) {
            BillingPeriod.DAILY -> cost.multiply(BigDecimal(365))
            BillingPeriod.WEEKLY -> cost.multiply(BigDecimal(52))
            BillingPeriod.MONTHLY -> cost.multiply(BigDecimal(12))
            BillingPeriod.QUARTERLY -> cost.multiply(BigDecimal(4))
            BillingPeriod.SEMI_ANNUALLY -> cost.multiply(BigDecimal(2))
            BillingPeriod.ANNUALLY -> cost
            BillingPeriod.CUSTOM -> cost.multiply(BigDecimal(365 / billingCycle))
        }
    }
}

/**
 * Enumeration of billing periods.
 */
enum class BillingPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    SEMI_ANNUALLY,
    ANNUALLY,
    CUSTOM
}

/**
 * Enumeration of subscription statuses.
 */
enum class SubscriptionStatus {
    ACTIVE,
    TRIAL,
    PAUSED,
    CANCELLED,
    EXPIRED
}
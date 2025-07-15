package com.subcontrol.data.mapper

import com.subcontrol.data.model.proto.BillingPeriod as ProtoBillingPeriod
import com.subcontrol.data.model.proto.Subscription as ProtoSubscription
import com.subcontrol.data.model.proto.SubscriptionStatus as ProtoSubscriptionStatus
import com.subcontrol.domain.model.BillingPeriod
import com.subcontrol.domain.model.Subscription
import com.subcontrol.domain.model.SubscriptionStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Mapper functions for converting between domain and proto models.
 */
object SubscriptionMapper {

    /**
     * Converts proto subscription to domain subscription.
     */
    fun ProtoSubscription.toDomain(): Subscription {
        return Subscription(
            id = id,
            name = name,
            description = description,
            cost = BigDecimal(cost.toString()),
            currency = currency,
            billingPeriod = billingPeriod.toDomain(),
            billingCycle = billingCycle,
            startDate = Instant.ofEpochSecond(startDate).atZone(ZoneId.systemDefault()).toLocalDate(),
            nextBillingDate = Instant.ofEpochSecond(nextBillingDate).atZone(ZoneId.systemDefault()).toLocalDate(),
            trialEndDate = if (trialEndDate > 0) {
                Instant.ofEpochSecond(trialEndDate).atZone(ZoneId.systemDefault()).toLocalDate()
            } else null,
            status = status.toDomain(),
            notificationsEnabled = notificationsEnabled,
            notificationDaysBefore = notificationDaysBefore,
            category = category,
            tags = tagsList,
            notes = notes,
            websiteUrl = websiteUrl,
            supportEmail = supportEmail,
            createdAt = Instant.ofEpochSecond(createdAt).atZone(ZoneId.systemDefault()).toLocalDateTime(),
            updatedAt = Instant.ofEpochSecond(updatedAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
        )
    }

    /**
     * Converts domain subscription to proto subscription.
     */
    fun Subscription.toProto(): ProtoSubscription {
        return ProtoSubscription.newBuilder()
            .setId(id)
            .setName(name)
            .setDescription(description)
            .setCost(cost.toDouble())
            .setCurrency(currency)
            .setBillingPeriod(billingPeriod.toProto())
            .setBillingCycle(billingCycle)
            .setStartDate(startDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond())
            .setNextBillingDate(nextBillingDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond())
            .setTrialEndDate(trialEndDate?.atStartOfDay(ZoneId.systemDefault())?.toEpochSecond() ?: 0L)
            .setStatus(status.toProto())
            .setNotificationsEnabled(notificationsEnabled)
            .setNotificationDaysBefore(notificationDaysBefore)
            .setCategory(category)
            .addAllTags(tags)
            .setNotes(notes)
            .setWebsiteUrl(websiteUrl)
            .setSupportEmail(supportEmail)
            .setCreatedAt(createdAt.atZone(ZoneId.systemDefault()).toEpochSecond())
            .setUpdatedAt(updatedAt.atZone(ZoneId.systemDefault()).toEpochSecond())
            .build()
    }

    /**
     * Converts proto billing period to domain billing period.
     */
    private fun ProtoBillingPeriod.toDomain(): BillingPeriod {
        return when (this) {
            ProtoBillingPeriod.DAILY -> BillingPeriod.DAILY
            ProtoBillingPeriod.WEEKLY -> BillingPeriod.WEEKLY
            ProtoBillingPeriod.MONTHLY -> BillingPeriod.MONTHLY
            ProtoBillingPeriod.QUARTERLY -> BillingPeriod.QUARTERLY
            ProtoBillingPeriod.SEMI_ANNUALLY -> BillingPeriod.SEMI_ANNUALLY
            ProtoBillingPeriod.ANNUALLY -> BillingPeriod.ANNUALLY
            ProtoBillingPeriod.CUSTOM -> BillingPeriod.CUSTOM
            else -> BillingPeriod.MONTHLY // Default fallback
        }
    }

    /**
     * Converts domain billing period to proto billing period.
     */
    private fun BillingPeriod.toProto(): ProtoBillingPeriod {
        return when (this) {
            BillingPeriod.DAILY -> ProtoBillingPeriod.DAILY
            BillingPeriod.WEEKLY -> ProtoBillingPeriod.WEEKLY
            BillingPeriod.MONTHLY -> ProtoBillingPeriod.MONTHLY
            BillingPeriod.QUARTERLY -> ProtoBillingPeriod.QUARTERLY
            BillingPeriod.SEMI_ANNUALLY -> ProtoBillingPeriod.SEMI_ANNUALLY
            BillingPeriod.ANNUALLY -> ProtoBillingPeriod.ANNUALLY
            BillingPeriod.CUSTOM -> ProtoBillingPeriod.CUSTOM
        }
    }

    /**
     * Converts proto subscription status to domain subscription status.
     */
    private fun ProtoSubscriptionStatus.toDomain(): SubscriptionStatus {
        return when (this) {
            ProtoSubscriptionStatus.ACTIVE -> SubscriptionStatus.ACTIVE
            ProtoSubscriptionStatus.TRIAL -> SubscriptionStatus.TRIAL
            ProtoSubscriptionStatus.PAUSED -> SubscriptionStatus.PAUSED
            ProtoSubscriptionStatus.CANCELLED -> SubscriptionStatus.CANCELLED
            ProtoSubscriptionStatus.EXPIRED -> SubscriptionStatus.EXPIRED
            else -> SubscriptionStatus.ACTIVE // Default fallback
        }
    }

    /**
     * Converts domain subscription status to proto subscription status.
     */
    private fun SubscriptionStatus.toProto(): ProtoSubscriptionStatus {
        return when (this) {
            SubscriptionStatus.ACTIVE -> ProtoSubscriptionStatus.ACTIVE
            SubscriptionStatus.TRIAL -> ProtoSubscriptionStatus.TRIAL
            SubscriptionStatus.PAUSED -> ProtoSubscriptionStatus.PAUSED
            SubscriptionStatus.CANCELLED -> ProtoSubscriptionStatus.CANCELLED
            SubscriptionStatus.EXPIRED -> ProtoSubscriptionStatus.EXPIRED
        }
    }
}
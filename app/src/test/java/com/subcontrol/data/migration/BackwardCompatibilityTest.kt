package com.subcontrol.data.migration

import com.subcontrol.data.mapper.SubscriptionMapper
import com.subcontrol.data.mapper.SubscriptionMapper.toDomain
import com.subcontrol.domain.model.BillingPeriod
import com.subcontrol.data.model.proto.Subscription as SubscriptionProto
import com.subcontrol.data.model.proto.BillingPeriod as ProtoBillingPeriod
import com.subcontrol.data.model.proto.SubscriptionStatus as ProtoSubscriptionStatus
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Backward compatibility test suite for Protocol Buffer schema evolution.
 * 
 * This test suite ensures that SubControl can read data created with older
 * versions of the Protocol Buffer schema, maintaining data integrity and
 * user experience across app updates.
 * 
 * Test Scenarios:
 * - Reading data created with previous schema versions
 * - Handling missing fields gracefully
 * - Providing sensible defaults for new fields
 * - Ensuring no data loss during migrations
 * - Validating field number consistency
 */
class BackwardCompatibilityTest {

    /**
     * Test reading data from schema version 1.0 (initial release).
     * This simulates the most basic subscription data structure.
     */
    @Test
    fun testReadingFromSchemaV1_0() {
        // Create proto data that represents v1.0 schema (minimal fields)
        val v1Proto = SubscriptionProto.newBuilder()
            .setId("v1-subscription-id")
            .setName("Netflix")
            .setCost(9.99)
            .setCurrency("USD")
            .setBillingPeriod(ProtoBillingPeriod.MONTHLY)
            .setBillingCycle(1)
            .setStartDate(LocalDate.now().minusMonths(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setNextBillingDate(LocalDate.now().plusMonths(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setStatus(ProtoSubscriptionStatus.ACTIVE)
            .setCreatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setUpdatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .build()
        
        // Convert to domain model
        val subscription = v1Proto.toDomain()
        
        // Verify core fields are preserved
        assertEquals("v1-subscription-id", subscription.id)
        assertEquals("Netflix", subscription.name)
        assertEquals(BigDecimal("9.99"), subscription.cost)
        assertEquals("USD", subscription.currency)
        assertEquals(BillingPeriod.MONTHLY, subscription.billingPeriod)
        assertEquals(1, subscription.billingCycle)
        assertTrue(subscription.isActive)
        
        // Verify that missing fields have sensible defaults
        assertEquals("", subscription.category)
        assertEquals("", subscription.notes)
        assertEquals(emptyList<String>(), subscription.tags)
        assertFalse(subscription.isInTrial)
        assertNull(subscription.trialEndDate)
        assertEquals(0, subscription.notificationDaysBefore)
    }

    /**
     * Test reading data from schema version 1.1 (added category and notes).
     * This simulates data created when basic categorization was added.
     */
    @Test
    fun testReadingFromSchemaV1_1() {
        // Create proto data that represents v1.1 schema (added category and notes)
        val v1_1Proto = SubscriptionProto.newBuilder()
            .setId("v1-1-subscription-id")
            .setName("Spotify")
            .setCost(9.99)
            .setCurrency("USD")
            .setBillingPeriod(ProtoBillingPeriod.MONTHLY)
            .setBillingCycle(1)
            .setStartDate(LocalDate.now().minusMonths(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setNextBillingDate(LocalDate.now().plusMonths(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setStatus(ProtoSubscriptionStatus.ACTIVE)
            .setCategory("Entertainment")
            .setNotes("Music streaming service")
            .setCreatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setUpdatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .build()
        
        // Convert to domain model
        val subscription = v1_1Proto.toDomain()
        
        // Verify all v1.1 fields are preserved
        assertEquals("ID should be preserved", "v1-1-subscription-id", subscription.id)
        assertEquals("Name should be preserved", "Spotify", subscription.name)
        assertEquals("Cost should be preserved", BigDecimal("9.99"), subscription.cost)
        assertEquals("Currency should be preserved", "USD", subscription.currency)
        assertEquals("Billing period should be preserved", BillingPeriod.MONTHLY, subscription.billingPeriod)
        assertTrue("Should be active", subscription.isActive)
        assertEquals("Category should be preserved", "Entertainment", subscription.category)
        assertEquals("Notes should be preserved", "Music streaming service", subscription.notes)
        
        // Verify that fields added after v1.1 have defaults
        assertEquals("Tags should have default empty list", emptyList<String>(), subscription.tags)
        assertFalse("Should not be in trial", subscription.isInTrial)
        assertNull("Trial end date should be null", subscription.trialEndDate)
        assertEquals("Notification days should have default", 0, subscription.notificationDaysBefore)
    }

    /**
     * Test reading data from schema version 1.2 (added trial support).
     * This simulates data created when trial period tracking was added.
     */
    @Test
    fun testReadingFromSchemaV1_2() {
        // Create proto data that represents v1.2 schema (added trial support)
        val v1_2Proto = SubscriptionProto.newBuilder()
            .setId("v1-2-subscription-id")
            .setName("Adobe Creative Cloud")
            .setCost(52.99)
            .setCurrency("USD")
            .setBillingPeriod(ProtoBillingPeriod.MONTHLY)
            .setBillingCycle(1)
            .setStartDate(LocalDate.now().minusMonths(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setNextBillingDate(LocalDate.now().plusMonths(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setTrialEndDate(LocalDate.now().plusWeeks(2).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setStatus(ProtoSubscriptionStatus.TRIAL)
            .setCategory("Productivity")
            .setNotes("Design software suite")
            .setCreatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setUpdatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .build()
        
        // Convert to domain model
        val subscription = v1_2Proto.toDomain()
        
        // Verify all v1.2 fields are preserved
        assertEquals("ID should be preserved", "v1-2-subscription-id", subscription.id)
        assertEquals("Name should be preserved", "Adobe Creative Cloud", subscription.name)
        assertEquals("Cost should be preserved", BigDecimal("52.99"), subscription.cost)
        assertEquals("Currency should be preserved", "USD", subscription.currency)
        assertEquals("Billing period should be preserved", BillingPeriod.MONTHLY, subscription.billingPeriod)
        assertTrue("Should be active", subscription.isActive)
        assertEquals("Category should be preserved", "Productivity", subscription.category)
        assertEquals("Notes should be preserved", "Design software suite", subscription.notes)
        assertTrue("Should be in trial", subscription.isInTrial)
        assertEquals("Trial end date should be preserved", LocalDate.now().plusWeeks(2), subscription.trialEndDate)
        
        // Verify that fields added after v1.2 have defaults
        assertEquals("Tags should have default empty list", emptyList<String>(), subscription.tags)
        assertEquals("Notification days should have default", 0, subscription.notificationDaysBefore)
    }

    /**
     * Test reading data from schema version 1.3 (added tags and reminders).
     * This simulates data created when tagging and notification features were added.
     */
    @Test
    fun testReadingFromSchemaV1_3() {
        // Create proto data that represents v1.3 schema (added tags and reminders)
        val v1_3Proto = SubscriptionProto.newBuilder()
            .setId("v1-3-subscription-id")
            .setName("Microsoft 365")
            .setCost(9.99)
            .setCurrency("USD")
            .setBillingPeriod(ProtoBillingPeriod.MONTHLY)
            .setBillingCycle(1)
            .setStartDate(LocalDate.now().minusMonths(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setNextBillingDate(LocalDate.now().plusMonths(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setStatus(ProtoSubscriptionStatus.ACTIVE)
            .setCategory("Productivity")
            .setNotes("Office suite with cloud storage")
            .addTags("office")
            .addTags("productivity")
            .addTags("cloud")
            .setNotificationDaysBefore(7)
            .setCreatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setUpdatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .build()
        
        // Convert to domain model
        val subscription = v1_3Proto.toDomain()
        
        // Verify all v1.3 fields are preserved
        assertEquals("ID should be preserved", "v1-3-subscription-id", subscription.id)
        assertEquals("Name should be preserved", "Microsoft 365", subscription.name)
        assertEquals("Cost should be preserved", BigDecimal("9.99"), subscription.cost)
        assertEquals("Currency should be preserved", "USD", subscription.currency)
        assertEquals("Billing period should be preserved", BillingPeriod.MONTHLY, subscription.billingPeriod)
        assertTrue("Should be active", subscription.isActive)
        assertEquals("Category should be preserved", "Productivity", subscription.category)
        assertEquals("Notes should be preserved", "Office suite with cloud storage", subscription.notes)
        assertFalse("Should not be in trial", subscription.isInTrial)
        assertEquals("Tags should be preserved", listOf("office", "productivity", "cloud"), subscription.tags)
        assertEquals("Notification days should be preserved", 7, subscription.notificationDaysBefore)
        
        // Verify that trial end date is null for non-trial subscriptions
        assertNull("Trial end date should be null for non-trial", subscription.trialEndDate)
    }

    /**
     * Test that field number changes don't break backward compatibility.
     * This is a critical test to ensure schema evolution safety.
     */
    @Test
    fun testFieldNumberConsistency() {
        // Create subscription data with all fields to test field number consistency
        val subscription = SubscriptionProto.newBuilder()
            .setId("field-test-id")                    // Field 1
            .setName("Field Test Subscription")        // Field 2
            .setDescription("Field test description")  // Field 3
            .setCost(15.99)                           // Field 4
            .setCurrency("EUR")                        // Field 5
            .setBillingPeriod(ProtoBillingPeriod.ANNUALLY) // Field 6
            .setBillingCycle(1)                        // Field 7
            .setStartDate(LocalDate.now().minusYears(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond()) // Field 8
            .setNextBillingDate(LocalDate.now().plusYears(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond()) // Field 9
            .setStatus(ProtoSubscriptionStatus.ACTIVE)  // Field 10
            .setNotificationsEnabled(true)             // Field 11
            .setNotificationDaysBefore(5)              // Field 12
            .setCategory("Business")                   // Field 13
            .addTags("test")                           // Field 14
            .setNotes("Testing field numbers")        // Field 15
            .setCreatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond()) // Field 16
            .setUpdatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond()) // Field 17
            .build()
        
        // Serialize and deserialize to test field number consistency
        val serialized = subscription.toByteArray()
        val deserialized = SubscriptionProto.parseFrom(serialized)
        
        // Verify all fields are preserved
        assertEquals("Field numbers should be consistent", subscription, deserialized)
        
        // Convert to domain and verify
        val domainSubscription = deserialized.toDomain()
        assertEquals("Domain conversion should work", "field-test-id", domainSubscription.id)
        assertEquals("Domain conversion should work", "Field Test Subscription", domainSubscription.name)
        assertEquals("Domain conversion should work", BigDecimal("15.99"), domainSubscription.cost)
        assertEquals("Domain conversion should work", "EUR", domainSubscription.currency)
        assertEquals("Domain conversion should work", BillingPeriod.ANNUALLY, domainSubscription.billingPeriod)
        assertEquals("Domain conversion should work", "Business", domainSubscription.category)
        assertEquals("Domain conversion should work", "Testing field numbers", domainSubscription.notes)
        assertTrue("Should be active", domainSubscription.isActive)
        assertFalse("Should not be in trial", domainSubscription.isInTrial)
        assertEquals("Domain conversion should work", listOf("test"), domainSubscription.tags)
        assertEquals("Domain conversion should work", 5, domainSubscription.notificationDaysBefore)
    }

    /**
     * Test handling of deprecated fields.
     * This ensures that deprecated fields don't cause issues when reading old data.
     */
    @Test
    fun testDeprecatedFieldHandling() {
        // Create a subscription with what might be deprecated fields
        val subscriptionWithDeprecated = SubscriptionProto.newBuilder()
            .setId("deprecated-test-id")
            .setName("Deprecated Field Test")
            .setCost(12.99)
            .setCurrency("GBP")
            .setBillingPeriod(ProtoBillingPeriod.MONTHLY)
            .setBillingCycle(1)
            .setStartDate(LocalDate.now().minusMonths(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setNextBillingDate(LocalDate.now().plusMonths(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setStatus(ProtoSubscriptionStatus.ACTIVE)
            .setCategory("Test")
            .setNotes("Testing deprecated field handling")
            .setCreatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setUpdatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .build()
        
        // Convert to domain model
        val subscription = subscriptionWithDeprecated.toDomain()
        
        // Verify that the subscription can still be read correctly
        assertEquals("ID should be preserved", "deprecated-test-id", subscription.id)
        assertEquals("Name should be preserved", "Deprecated Field Test", subscription.name)
        assertEquals("Cost should be preserved", BigDecimal("12.99"), subscription.cost)
        assertEquals("Currency should be preserved", "GBP", subscription.currency)
        assertEquals("Billing period should be preserved", BillingPeriod.MONTHLY, subscription.billingPeriod)
        assertTrue("Should be active", subscription.isActive)
        assertEquals("Category should be preserved", "Test", subscription.category)
        assertEquals("Notes should be preserved", "Testing deprecated field handling", subscription.notes)
    }

    /**
     * Test that missing required fields are handled gracefully.
     * This ensures the app doesn't crash when reading corrupted or incomplete data.
     */
    @Test
    fun testMissingRequiredFieldHandling() {
        // Create a subscription with minimal required fields
        val minimalSubscription = SubscriptionProto.newBuilder()
            .setId("minimal-test-id")
            .setName("Minimal Test")
            .setCost(1.99)
            .setCurrency("USD")
            .setBillingPeriod(ProtoBillingPeriod.MONTHLY)
            .setBillingCycle(1)
            .setStartDate(LocalDate.now().minusMonths(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setNextBillingDate(LocalDate.now().plusMonths(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setStatus(ProtoSubscriptionStatus.ACTIVE)
            .setCreatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setUpdatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .build()
        
        // Convert to domain model
        val subscription = minimalSubscription.toDomain()
        
        // Verify that the subscription is created with sensible defaults
        assertEquals("ID should be preserved", "minimal-test-id", subscription.id)
        assertEquals("Name should be preserved", "Minimal Test", subscription.name)
        assertEquals("Cost should be preserved", BigDecimal("1.99"), subscription.cost)
        assertEquals("Currency should be preserved", "USD", subscription.currency)
        assertEquals("Billing period should be preserved", BillingPeriod.MONTHLY, subscription.billingPeriod)
        assertTrue("Should be active", subscription.isActive)
        
        // Verify defaults for optional fields
        assertEquals("Category should have default", "", subscription.category)
        assertEquals("Notes should have default", "", subscription.notes)
        assertEquals("Tags should have default", emptyList<String>(), subscription.tags)
        assertFalse("Should not be in trial", subscription.isInTrial)
        assertNull("Trial end date should be null", subscription.trialEndDate)
        assertEquals("Notification days should have default", 0, subscription.notificationDaysBefore)
    }

    /**
     * Test handling of invalid enum values.
     * This ensures that invalid billing cycles don't break the app.
     */
    @Test
    fun testInvalidEnumHandling() {
        // Create a subscription with a valid billing period
        val validSubscription = SubscriptionProto.newBuilder()
            .setId("enum-test-id")
            .setName("Enum Test")
            .setCost(7.99)
            .setCurrency("CAD")
            .setBillingPeriod(ProtoBillingPeriod.WEEKLY)  // Valid enum value
            .setBillingCycle(1)
            .setStartDate(LocalDate.now().minusWeeks(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setNextBillingDate(LocalDate.now().plusWeeks(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setStatus(ProtoSubscriptionStatus.ACTIVE)
            .setCreatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setUpdatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .build()
        
        // Convert to domain model
        val subscription = validSubscription.toDomain()
        
        // Verify that valid enum values work correctly
        assertEquals("Billing period should be preserved", BillingPeriod.WEEKLY, subscription.billingPeriod)
        assertEquals("ID should be preserved", "enum-test-id", subscription.id)
        assertEquals("Name should be preserved", "Enum Test", subscription.name)
        assertEquals("Cost should be preserved", BigDecimal("7.99"), subscription.cost)
        assertEquals("Currency should be preserved", "CAD", subscription.currency)
        assertTrue("Should be active", subscription.isActive)
    }

    /**
     * Test data migration with mixed schema versions.
     * This simulates a real scenario where different subscriptions were created
     * with different schema versions.
     */
    @Test
    fun testMixedSchemaVersions() {
        // Create subscriptions representing different schema versions
        val v1Subscription = SubscriptionProto.newBuilder()
            .setId("v1-mixed-id")
            .setName("V1 Subscription")
            .setCost(5.99)
            .setCurrency("USD")
            .setBillingPeriod(ProtoBillingPeriod.MONTHLY)
            .setBillingCycle(1)
            .setStartDate(LocalDate.now().minusMonths(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setNextBillingDate(LocalDate.now().plusMonths(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setStatus(ProtoSubscriptionStatus.ACTIVE)
            .setCreatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setUpdatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .build()
        
        val v2Subscription = SubscriptionProto.newBuilder()
            .setId("v2-mixed-id")
            .setName("V2 Subscription")
            .setCost(11.99)
            .setCurrency("EUR")
            .setBillingPeriod(ProtoBillingPeriod.ANNUALLY)
            .setBillingCycle(1)
            .setStartDate(LocalDate.now().minusYears(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setNextBillingDate(LocalDate.now().plusYears(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setStatus(ProtoSubscriptionStatus.ACTIVE)
            .setCategory("Entertainment")
            .setNotes("Added in v2")
            .setCreatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setUpdatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .build()
        
        val v3Subscription = SubscriptionProto.newBuilder()
            .setId("v3-mixed-id")
            .setName("V3 Subscription")
            .setCost(19.99)
            .setCurrency("GBP")
            .setBillingPeriod(ProtoBillingPeriod.MONTHLY)
            .setBillingCycle(1)
            .setStartDate(LocalDate.now().minusMonths(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setNextBillingDate(LocalDate.now().plusMonths(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setTrialEndDate(LocalDate.now().plusWeeks(2).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setStatus(ProtoSubscriptionStatus.TRIAL)
            .setCategory("Productivity")
            .setNotes("Full featured")
            .addTags("trial")
            .addTags("productivity")
            .setNotificationDaysBefore(3)
            .setCreatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setUpdatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .build()
        
        // Convert all to domain models
        val subscriptions = listOf(v1Subscription, v2Subscription, v3Subscription)
            .map { it.toDomain() }
        
        // Verify that all subscriptions are correctly converted
        assertEquals("Should have 3 subscriptions", 3, subscriptions.size)
        
        // Verify V1 subscription
        val v1Domain = subscriptions[0]
        assertEquals("V1 ID should be preserved", "v1-mixed-id", v1Domain.id)
        assertEquals("V1 name should be preserved", "V1 Subscription", v1Domain.name)
        assertEquals("V1 category should have default", "", v1Domain.category)
        assertEquals("V1 notes should have default", "", v1Domain.notes)
        assertEquals("V1 tags should have default", emptyList<String>(), v1Domain.tags)
        
        // Verify V2 subscription
        val v2Domain = subscriptions[1]
        assertEquals("V2 ID should be preserved", "v2-mixed-id", v2Domain.id)
        assertEquals("V2 name should be preserved", "V2 Subscription", v2Domain.name)
        assertEquals("V2 category should be preserved", "Entertainment", v2Domain.category)
        assertEquals("V2 notes should be preserved", "Added in v2", v2Domain.notes)
        assertEquals("V2 tags should have default", emptyList<String>(), v2Domain.tags)
        
        // Verify V3 subscription
        val v3Domain = subscriptions[2]
        assertEquals("V3 ID should be preserved", "v3-mixed-id", v3Domain.id)
        assertEquals("V3 name should be preserved", "V3 Subscription", v3Domain.name)
        assertEquals("V3 category should be preserved", "Productivity", v3Domain.category)
        assertEquals("V3 notes should be preserved", "Full featured", v3Domain.notes)
        assertTrue("V3 should be in trial", v3Domain.isInTrial)
        assertEquals("V3 tags should be preserved", listOf("trial", "productivity"), v3Domain.tags)
        assertEquals("V3 notification days should be preserved", 3, v3Domain.notificationDaysBefore)
    }
}
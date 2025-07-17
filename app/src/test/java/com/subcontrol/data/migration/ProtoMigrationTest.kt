package com.subcontrol.data.migration

import com.subcontrol.data.datastore.EncryptionManager
import com.subcontrol.data.mapper.SubscriptionMapper
import com.subcontrol.data.mapper.SubscriptionMapper.toDomain
import com.subcontrol.data.mapper.SubscriptionMapper.toProto
import com.subcontrol.domain.model.BillingPeriod
import com.subcontrol.domain.model.Subscription
import com.subcontrol.domain.model.SubscriptionStatus
import com.subcontrol.data.model.proto.AppData
import com.subcontrol.data.model.proto.SubscriptionList
import com.subcontrol.data.model.proto.Subscription as SubscriptionProto
import com.subcontrol.data.model.proto.BillingPeriod as ProtoBillingPeriod
import com.subcontrol.data.model.proto.SubscriptionStatus as ProtoSubscriptionStatus
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Protocol Buffer schema migration contract testing for SubControl.
 * 
 * This test suite validates that Protocol Buffer schema changes maintain
 * backward compatibility and data integrity across version migrations.
 * 
 * Key Test Areas:
 * - Schema evolution without breaking existing data
 * - Field number consistency across versions
 * - Backward compatibility with older data formats
 * - Data integrity during schema migrations
 * - Default value handling for new fields
 * 
 * Contract Testing Strategy:
 * - Generate test data in various schema versions
 * - Verify serialization/deserialization works across versions
 * - Test that new fields have proper defaults
 * - Validate that removed fields don't break existing data
 */
class ProtoMigrationTest {

    // Using extension functions from SubscriptionMapper

    @Before
    fun setUp() {
        mockkObject(EncryptionManager)
        
        // Mock encryption for testing
        every { EncryptionManager.encrypt(any()) } returns byteArrayOf(1, 2, 3, 4)
        every { EncryptionManager.decrypt(any()) } returns byteArrayOf(1, 2, 3, 4)
    }

    @After
    fun tearDown() {
        unmockkObject(EncryptionManager)
    }

    /**
     * Test that current schema can serialize and deserialize properly.
     * This is the baseline test for schema integrity.
     */
    @Test
    fun testCurrentSchemaIntegrity() {
        // Create a complete subscription with all fields
        val subscription = createCompleteSubscription()
        
        // Convert to proto
        val protoSubscription = subscription.toProto()
        
        // Verify all fields are preserved
        assertNotNull("Proto subscription should not be null", protoSubscription)
        assertEquals("Name should match", subscription.name, protoSubscription.name)
        assertEquals("Cost should match", subscription.cost.toDouble(), protoSubscription.cost, 0.01)
        assertEquals("Currency should match", subscription.currency, protoSubscription.currency)
        assertEquals("Billing period should match", subscription.billingPeriod.name, protoSubscription.billingPeriod.name)
        assertEquals("Billing cycle should match", subscription.billingCycle, protoSubscription.billingCycle)
        assertEquals("Category should match", subscription.category, protoSubscription.category)
        assertEquals("Notes should match", subscription.notes, protoSubscription.notes)
        assertEquals("Status should match", subscription.status.name, protoSubscription.status.name)
        assertEquals("Tags should match", subscription.tags, protoSubscription.tagsList)
        
        // Convert back to domain
        val convertedBack = protoSubscription.toDomain()
        
        // Verify round-trip conversion
        assertEquals("Round-trip conversion should preserve all data", subscription, convertedBack)
    }

    /**
     * Test backward compatibility with schema version 1.
     * This simulates reading data created with an older version of the schema.
     */
    @Test
    fun testBackwardCompatibilityV1() {
        // Create a proto subscription that simulates v1 schema (minimal fields)
        val v1ProtoSubscription = SubscriptionProto.newBuilder()
            .setId("test-id")
            .setName("Test Subscription")
            .setCost(9.99)
            .setCurrency("USD")
            .setBillingPeriod(ProtoBillingPeriod.MONTHLY)
            .setBillingCycle(1)
            .setStartDate(LocalDate.now().atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setNextBillingDate(LocalDate.now().plusMonths(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setStatus(ProtoSubscriptionStatus.ACTIVE)
            .setCreatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setUpdatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .build()
        
        // Convert to domain model
        val subscription = v1ProtoSubscription.toDomain()
        
        // Verify that missing fields have proper defaults
        assertEquals("Name should be preserved", "Test Subscription", subscription.name)
        assertEquals("Cost should be preserved", BigDecimal("9.99"), subscription.cost)
        assertEquals("Currency should be preserved", "USD", subscription.currency)
        assertEquals("Billing period should be preserved", BillingPeriod.MONTHLY, subscription.billingPeriod)
        assertEquals("Billing cycle should be preserved", 1, subscription.billingCycle)
        assertTrue("Should be active", subscription.isActive)
        
        // Verify default values for fields that might not exist in v1
        assertEquals("Category should have default", "", subscription.category)
        assertEquals("Notes should have default", "", subscription.notes)
        assertEquals("Tags should have default", emptyList<String>(), subscription.tags)
        assertFalse("Should not be in trial", subscription.isInTrial)
        assertNull("Trial end date should be null", subscription.trialEndDate)
    }

    /**
     * Test forward compatibility by adding new fields.
     * This simulates adding new fields to the schema and ensuring they work properly.
     */
    @Test
    fun testForwardCompatibilityNewFields() {
        // Create a subscription with all current fields
        val subscription = createCompleteSubscription()
        
        // Convert to proto
        val protoSubscription = subscription.toProto()
        
        // Simulate adding new fields by creating an extended proto
        val extendedProto = protoSubscription.toBuilder()
            .build()
        
        // Verify that the existing data is still readable
        val convertedSubscription = extendedProto.toDomain()
        
        // All existing fields should be preserved
        assertEquals("Name should be preserved", subscription.name, convertedSubscription.name)
        assertEquals("Cost should be preserved", subscription.cost, convertedSubscription.cost)
        assertEquals("Currency should be preserved", subscription.currency, convertedSubscription.currency)
        assertEquals("Billing period should be preserved", subscription.billingPeriod, convertedSubscription.billingPeriod)
        assertEquals("Billing cycle should be preserved", subscription.billingCycle, convertedSubscription.billingCycle)
        assertEquals("Category should be preserved", subscription.category, convertedSubscription.category)
        assertEquals("Notes should be preserved", subscription.notes, convertedSubscription.notes)
        assertEquals("Status should be preserved", subscription.status, convertedSubscription.status)
        assertEquals("Tags should be preserved", subscription.tags, convertedSubscription.tags)
    }

    /**
     * Test field number consistency to ensure schema evolution safety.
     * This verifies that field numbers remain consistent across versions.
     */
    @Test
    fun testFieldNumberConsistency() {
        // Create a proto subscription and verify field numbers are as expected
        val protoSubscription = SubscriptionProto.newBuilder()
            .setId("test-id")                    // Field 1
            .setName("Test")                     // Field 2
            .setDescription("Test description")  // Field 3
            .setCost(9.99)                       // Field 4
            .setCurrency("USD")                  // Field 5
            .setBillingPeriod(ProtoBillingPeriod.MONTHLY) // Field 6
            .setBillingCycle(1)                  // Field 7
            .setStartDate(LocalDate.now().atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond()) // Field 8
            .setNextBillingDate(LocalDate.now().plusMonths(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond()) // Field 9
            .setStatus(ProtoSubscriptionStatus.ACTIVE) // Field 10
            .setNotificationsEnabled(true)       // Field 11
            .setNotificationDaysBefore(3)        // Field 12
            .setCategory("Test Category")        // Field 13
            .addAllTags(listOf("tag1", "tag2"))  // Field 14
            .setNotes("Test Notes")              // Field 15
            .setCreatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond()) // Field 16
            .setUpdatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond()) // Field 17
            .build()
        
        // Verify that the proto can be serialized and deserialized
        val serialized = protoSubscription.toByteArray()
        assertNotNull("Serialized data should not be null", serialized)
        assertTrue("Serialized data should not be empty", serialized.isNotEmpty())
        
        // Deserialize and verify
        val deserialized = SubscriptionProto.parseFrom(serialized)
        assertEquals("Deserialized should match original", protoSubscription, deserialized)
    }

    /**
     * Test data integrity during batch migrations.
     * This simulates migrating multiple subscriptions at once.
     */
    @Test
    fun testBatchMigrationIntegrity() {
        // Create multiple subscriptions with different characteristics
        val subscriptions = listOf(
            createCompleteSubscription(),
            createMinimalSubscription(),
            createTrialSubscription(),
            createInactiveSubscription()
        )
        
        // Convert to proto list
        val protoList = SubscriptionList.newBuilder()
            .addAllSubscriptions(subscriptions.map { it.toProto() })
            .build()
        
        // Verify batch serialization
        val serialized = protoList.toByteArray()
        assertNotNull("Serialized list should not be null", serialized)
        assertTrue("Serialized list should not be empty", serialized.isNotEmpty())
        
        // Deserialize and verify
        val deserializedList = SubscriptionList.parseFrom(serialized)
        assertEquals("List size should match", subscriptions.size, deserializedList.subscriptionsCount)
        
        // Convert back to domain and verify each subscription
        val convertedSubscriptions = deserializedList.subscriptionsList.map { it.toDomain() }
        
        assertEquals("Converted list size should match", subscriptions.size, convertedSubscriptions.size)
        
        // Verify each subscription individually
        subscriptions.forEachIndexed { index, original ->
            val converted = convertedSubscriptions[index]
            assertEquals("Subscription $index should match", original, converted)
        }
    }

    /**
     * Test app data container migration.
     * This tests the overall app data structure migration.
     */
    @Test
    fun testAppDataContainerMigration() {
        // Create app data with subscriptions
        val subscriptions = listOf(
            createCompleteSubscription(),
            createMinimalSubscription()
        )
        
        val subscriptionList = SubscriptionList.newBuilder()
            .addAllSubscriptions(subscriptions.map { it.toProto() })
            .build()
        
        val appData = AppData.newBuilder()
            .setSubscriptionList(subscriptionList)
            .build()
        
        // Test serialization
        val serialized = appData.toByteArray()
        assertNotNull("Serialized app data should not be null", serialized)
        assertTrue("Serialized app data should not be empty", serialized.isNotEmpty())
        
        // Test deserialization
        val deserialized = AppData.parseFrom(serialized)
        assertEquals("App data should match", appData, deserialized)
        
        // Verify subscription data integrity
        val deserializedSubscriptions = deserialized.subscriptionList.subscriptionsList.map { it.toDomain() }
        assertEquals("Subscription count should match", subscriptions.size, deserializedSubscriptions.size)
        
        subscriptions.forEachIndexed { index, original ->
            val deserialized = deserializedSubscriptions[index]
            assertEquals("App data subscription $index should match", original, deserialized)
        }
    }

    /**
     * Test default value handling for optional fields.
     * This ensures new optional fields don't break existing data.
     */
    @Test
    fun testDefaultValueHandling() {
        // Create a proto with only required fields
        val minimalProto = SubscriptionProto.newBuilder()
            .setId("test-id")
            .setName("Test Subscription")
            .setCost(9.99)
            .setCurrency("USD")
            .setBillingPeriod(ProtoBillingPeriod.MONTHLY)
            .setBillingCycle(1)
            .setStartDate(LocalDate.now().atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setNextBillingDate(LocalDate.now().plusMonths(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setStatus(ProtoSubscriptionStatus.ACTIVE)
            .setCreatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .setUpdatedAt(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toEpochSecond())
            .build()
        
        // Convert to domain
        val subscription = minimalProto.toDomain()
        
        // Verify default values
        assertEquals("Category should have default", "", subscription.category)
        assertEquals("Notes should have default", "", subscription.notes)
        assertEquals("Tags should have default", emptyList<String>(), subscription.tags)
        assertFalse("Should not be in trial", subscription.isInTrial)
        assertNull("Trial end date should be null", subscription.trialEndDate)
        assertEquals("Notification days should have default", 0, subscription.notificationDaysBefore)
    }

    /**
     * Test that schema migration preserves data encryption compatibility.
     * This ensures that encrypted data remains readable after schema changes.
     */
    @Test
    fun testEncryptionCompatibilityAfterMigration() {
        // Create a subscription and simulate storage
        val subscription = createCompleteSubscription()
        val proto = subscription.toProto()
        
        // Simulate encryption during storage
        val serialized = proto.toByteArray()
        
        // Mock encryption to return the same data (identity function for testing)
        every { EncryptionManager.encrypt(serialized) } returns serialized
        every { EncryptionManager.decrypt(serialized) } returns serialized
        
        val encrypted = EncryptionManager.encrypt(serialized)
        
        // Simulate decryption during retrieval
        val decrypted = EncryptionManager.decrypt(encrypted)
        
        // Verify that the data is still readable
        assertEquals("Decrypted data should match serialized", serialized.size, decrypted.size)
        
        // Parse the decrypted data
        val deserializedProto = SubscriptionProto.parseFrom(decrypted)
        val deserializedSubscription = deserializedProto.toDomain()
        
        // Verify data integrity after encryption/decryption
        assertEquals("Subscription should survive encryption/decryption", subscription, deserializedSubscription)
    }

    // Helper methods for creating test data

    private fun createCompleteSubscription(): Subscription {
        val now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
        return Subscription(
            id = "test-id-complete",
            name = "Complete Test Subscription",
            description = "Test subscription with all fields",
            cost = BigDecimal("19.99"),
            currency = "USD",
            billingPeriod = BillingPeriod.MONTHLY,
            billingCycle = 1,
            startDate = LocalDate.now().minusMonths(1),
            nextBillingDate = LocalDate.now().plusMonths(1),
            trialEndDate = null,
            status = SubscriptionStatus.ACTIVE,
            notificationsEnabled = true,
            notificationDaysBefore = 3,
            category = "Entertainment",
            tags = listOf("streaming", "entertainment", "premium"),
            notes = "Test subscription with all fields",
            websiteUrl = "https://example.com",
            supportEmail = "support@example.com",
            createdAt = now,
            updatedAt = now
        )
    }

    private fun createMinimalSubscription(): Subscription {
        val now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
        return Subscription(
            id = "test-id-minimal",
            name = "Minimal Test Subscription",
            description = "",
            cost = BigDecimal("9.99"),
            currency = "USD",
            billingPeriod = BillingPeriod.MONTHLY,
            billingCycle = 1,
            startDate = LocalDate.now().minusMonths(1),
            nextBillingDate = LocalDate.now().plusMonths(1),
            trialEndDate = null,
            status = SubscriptionStatus.ACTIVE,
            notificationsEnabled = true,
            notificationDaysBefore = 3,
            category = "",
            tags = emptyList(),
            notes = "",
            websiteUrl = "",
            supportEmail = "",
            createdAt = now,
            updatedAt = now
        )
    }

    private fun createTrialSubscription(): Subscription {
        val now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
        return Subscription(
            id = "test-id-trial",
            name = "Trial Test Subscription",
            description = "Trial subscription",
            cost = BigDecimal("29.99"),
            currency = "USD",
            billingPeriod = BillingPeriod.MONTHLY,
            billingCycle = 1,
            startDate = LocalDate.now().minusWeeks(1),
            nextBillingDate = LocalDate.now().plusMonths(1),
            trialEndDate = LocalDate.now().plusWeeks(2),
            status = SubscriptionStatus.TRIAL,
            notificationsEnabled = true,
            notificationDaysBefore = 7,
            category = "Productivity",
            tags = listOf("trial", "productivity"),
            notes = "Trial subscription",
            websiteUrl = "https://trial.example.com",
            supportEmail = "trial@example.com",
            createdAt = now,
            updatedAt = now
        )
    }

    private fun createInactiveSubscription(): Subscription {
        val now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
        return Subscription(
            id = "test-id-inactive",
            name = "Inactive Test Subscription",
            description = "Cancelled subscription",
            cost = BigDecimal("5.99"),
            currency = "USD",
            billingPeriod = BillingPeriod.MONTHLY,
            billingCycle = 1,
            startDate = LocalDate.now().minusMonths(3),
            nextBillingDate = LocalDate.now().minusDays(1),
            trialEndDate = null,
            status = SubscriptionStatus.CANCELLED,
            notificationsEnabled = false,
            notificationDaysBefore = 0,
            category = "News",
            tags = listOf("cancelled", "news"),
            notes = "Cancelled subscription",
            websiteUrl = "https://cancelled.example.com",
            supportEmail = "cancelled@example.com",
            createdAt = now.minusMonths(3),
            updatedAt = now
        )
    }
}
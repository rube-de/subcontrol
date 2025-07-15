package com.subcontrol.domain.usecase.subscription

import com.subcontrol.domain.model.BillingPeriod
import com.subcontrol.domain.model.Subscription
import com.subcontrol.domain.model.SubscriptionStatus
import com.subcontrol.domain.repository.SubscriptionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SaveSubscriptionUseCaseTest {
    
    private val repository = mockk<SubscriptionRepository>()
    private lateinit var useCase: SaveSubscriptionUseCase
    
    private val validSubscription = Subscription(
        id = "",
        name = "Netflix",
        description = "Streaming service",
        cost = BigDecimal("9.99"),
        currency = "USD",
        billingPeriod = BillingPeriod.MONTHLY,
        billingCycle = 1,
        startDate = LocalDate.now().minusMonths(1),
        nextBillingDate = LocalDate.now().plusDays(5),
        trialEndDate = null,
        status = SubscriptionStatus.ACTIVE,
        notificationsEnabled = true,
        notificationDaysBefore = 3,
        category = "Entertainment",
        tags = listOf("streaming", "movies"),
        notes = "Monthly subscription",
        websiteUrl = "https://netflix.com",
        supportEmail = "support@netflix.com",
        createdAt = LocalDateTime.now().minusMonths(1),
        updatedAt = LocalDateTime.now()
    )
    
    @Before
    fun setup() {
        useCase = SaveSubscriptionUseCase(repository)
    }
    
    @Test
    fun `createSubscription creates new subscription successfully`() = runTest {
        // Given
        coEvery { repository.addSubscription(any()) } returns Result.success(Unit)
        
        // When
        val result = useCase.createSubscription(validSubscription)
        
        // Then
        assertTrue(result.isSuccess)
        val createdSubscription = result.getOrNull()!!
        assertNotEquals("", createdSubscription.id) // ID should be generated
        assertEquals(validSubscription.name, createdSubscription.name)
        assertEquals(validSubscription.cost, createdSubscription.cost)
        coVerify { repository.addSubscription(any()) }
    }
    
    @Test
    fun `createSubscription fails with invalid name`() = runTest {
        // Given
        val invalidSubscription = validSubscription.copy(name = "")
        
        // When
        val result = useCase.createSubscription(invalidSubscription)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Subscription name cannot be blank", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `createSubscription fails with negative cost`() = runTest {
        // Given
        val invalidSubscription = validSubscription.copy(cost = BigDecimal("-5.00"))
        
        // When
        val result = useCase.createSubscription(invalidSubscription)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Subscription cost cannot be negative", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `createSubscription fails with invalid currency`() = runTest {
        // Given
        val invalidSubscription = validSubscription.copy(currency = "US")
        
        // When
        val result = useCase.createSubscription(invalidSubscription)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Currency must be a valid 3-letter code", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `createSubscription fails with invalid billing cycle`() = runTest {
        // Given
        val invalidSubscription = validSubscription.copy(billingCycle = 0)
        
        // When
        val result = useCase.createSubscription(invalidSubscription)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Billing cycle must be positive", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `createSubscription fails with billing date too far in past`() = runTest {
        // Given
        val invalidSubscription = validSubscription.copy(
            nextBillingDate = LocalDate.now().minusDays(2)
        )
        
        // When
        val result = useCase.createSubscription(invalidSubscription)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Next billing date cannot be more than 1 day in the past", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `createSubscription fails with invalid notification days`() = runTest {
        // Given
        val invalidSubscription = validSubscription.copy(notificationDaysBefore = -1)
        
        // When
        val result = useCase.createSubscription(invalidSubscription)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Notification days before cannot be negative", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `createSubscription fails with invalid trial end date`() = runTest {
        // Given
        val invalidSubscription = validSubscription.copy(
            trialEndDate = validSubscription.startDate.minusDays(1)
        )
        
        // When
        val result = useCase.createSubscription(invalidSubscription)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Trial end date cannot be before start date", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `createSubscription fails with invalid email`() = runTest {
        // Given
        val invalidSubscription = validSubscription.copy(supportEmail = "invalid-email")
        
        // When
        val result = useCase.createSubscription(invalidSubscription)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Support email format is invalid", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `createSubscription fails with invalid URL`() = runTest {
        // Given
        val invalidSubscription = validSubscription.copy(websiteUrl = "not-a-url")
        
        // When
        val result = useCase.createSubscription(invalidSubscription)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Website URL format is invalid", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `updateSubscription updates existing subscription successfully`() = runTest {
        // Given
        val existingSubscription = validSubscription.copy(id = "existing-id")
        coEvery { repository.updateSubscription(any()) } returns Result.success(Unit)
        
        // When
        val result = useCase.updateSubscription(existingSubscription)
        
        // Then
        assertTrue(result.isSuccess)
        val updatedSubscription = result.getOrNull()!!
        assertEquals("existing-id", updatedSubscription.id)
        assertEquals(validSubscription.name, updatedSubscription.name)
        coVerify { repository.updateSubscription(any()) }
    }
    
    @Test
    fun `updateSubscription fails with blank id`() = runTest {
        // Given
        val subscriptionWithBlankId = validSubscription.copy(id = "")
        
        // When
        val result = useCase.updateSubscription(subscriptionWithBlankId)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Subscription ID is required for updates", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `updateBillingDate updates billing date successfully`() = runTest {
        // Given
        val subscriptionId = "test-id"
        val newBillingDate = LocalDate.now().plusDays(30)
        coEvery { repository.updateBillingDate(subscriptionId, newBillingDate) } returns Result.success(Unit)
        
        // When
        val result = useCase.updateBillingDate(subscriptionId, newBillingDate)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { repository.updateBillingDate(subscriptionId, newBillingDate) }
    }
    
    @Test
    fun `updateBillingDate fails with blank id`() = runTest {
        // Given
        val blankId = ""
        val newBillingDate = LocalDate.now().plusDays(30)
        
        // When
        val result = useCase.updateBillingDate(blankId, newBillingDate)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Subscription ID cannot be blank", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `updateBillingDate fails with past date`() = runTest {
        // Given
        val subscriptionId = "test-id"
        val pastDate = LocalDate.now().minusDays(1)
        
        // When
        val result = useCase.updateBillingDate(subscriptionId, pastDate)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Billing date cannot be in the past", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `updateStatus updates subscription status successfully`() = runTest {
        // Given
        val subscriptionId = "test-id"
        val newStatus = SubscriptionStatus.PAUSED
        coEvery { repository.updateStatus(subscriptionId, newStatus) } returns Result.success(Unit)
        
        // When
        val result = useCase.updateStatus(subscriptionId, newStatus)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { repository.updateStatus(subscriptionId, newStatus) }
    }
    
    @Test
    fun `cancelSubscription sets status to cancelled`() = runTest {
        // Given
        val subscriptionId = "test-id"
        coEvery { repository.updateStatus(subscriptionId, SubscriptionStatus.CANCELLED) } returns Result.success(Unit)
        
        // When
        val result = useCase.cancelSubscription(subscriptionId)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { repository.updateStatus(subscriptionId, SubscriptionStatus.CANCELLED) }
    }
    
    @Test
    fun `pauseSubscription sets status to paused`() = runTest {
        // Given
        val subscriptionId = "test-id"
        coEvery { repository.updateStatus(subscriptionId, SubscriptionStatus.PAUSED) } returns Result.success(Unit)
        
        // When
        val result = useCase.pauseSubscription(subscriptionId)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { repository.updateStatus(subscriptionId, SubscriptionStatus.PAUSED) }
    }
    
    @Test
    fun `reactivateSubscription sets status to active`() = runTest {
        // Given
        val subscriptionId = "test-id"
        coEvery { repository.updateStatus(subscriptionId, SubscriptionStatus.ACTIVE) } returns Result.success(Unit)
        
        // When
        val result = useCase.reactivateSubscription(subscriptionId)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { repository.updateStatus(subscriptionId, SubscriptionStatus.ACTIVE) }
    }
    
    @Test
    fun `repository failure propagates to use case result`() = runTest {
        // Given
        val error = RuntimeException("Repository error")
        coEvery { repository.addSubscription(any()) } returns Result.failure(error)
        
        // When
        val result = useCase.createSubscription(validSubscription)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Repository error", result.exceptionOrNull()?.message)
    }
}
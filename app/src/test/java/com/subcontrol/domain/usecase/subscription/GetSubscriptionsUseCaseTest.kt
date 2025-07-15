package com.subcontrol.domain.usecase.subscription

import com.subcontrol.domain.model.BillingPeriod
import com.subcontrol.domain.model.Subscription
import com.subcontrol.domain.model.SubscriptionStatus
import com.subcontrol.domain.repository.SubscriptionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows

class GetSubscriptionsUseCaseTest {
    
    private val repository = mockk<SubscriptionRepository>()
    private lateinit var useCase: GetSubscriptionsUseCase
    
    private val testSubscription = Subscription(
        id = "test-id",
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
        useCase = GetSubscriptionsUseCase(repository)
    }
    
    @Test
    fun `getAllSubscriptions returns all subscriptions from repository`() = runTest {
        // Given
        val subscriptions = listOf(testSubscription)
        every { repository.getAllSubscriptions() } returns flowOf(subscriptions)
        
        // When
        val result = useCase.getAllSubscriptions().first()
        
        // Then
        assertEquals(subscriptions, result)
        verify { repository.getAllSubscriptions() }
    }
    
    @Test
    fun `getActiveSubscriptions returns active subscriptions from repository`() = runTest {
        // Given
        val subscriptions = listOf(testSubscription)
        every { repository.getActiveSubscriptions() } returns flowOf(subscriptions)
        
        // When
        val result = useCase.getActiveSubscriptions().first()
        
        // Then
        assertEquals(subscriptions, result)
        verify { repository.getActiveSubscriptions() }
    }
    
    @Test
    fun `getSubscriptionById returns subscription when found`() = runTest {
        // Given
        val subscriptionId = "test-id"
        every { repository.getSubscriptionById(subscriptionId) } returns flowOf(testSubscription)
        
        // When
        val result = useCase.getSubscriptionById(subscriptionId).first()
        
        // Then
        assertEquals(testSubscription, result)
        verify { repository.getSubscriptionById(subscriptionId) }
    }
    
    @Test
    fun `getSubscriptionById returns null when not found`() = runTest {
        // Given
        val subscriptionId = "non-existent-id"
        every { repository.getSubscriptionById(subscriptionId) } returns flowOf(null)
        
        // When
        val result = useCase.getSubscriptionById(subscriptionId).first()
        
        // Then
        assertNull(result)
        verify { repository.getSubscriptionById(subscriptionId) }
    }
    
    @Test
    fun `getSubscriptionById throws exception for blank id`() {
        // Given
        val blankId = ""
        
        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            useCase.getSubscriptionById(blankId)
        }
    }
    
    @Test
    fun `getSubscriptionsByCategory returns subscriptions for category`() = runTest {
        // Given
        val category = "Entertainment"
        val subscriptions = listOf(testSubscription)
        every { repository.getSubscriptionsByCategory(category) } returns flowOf(subscriptions)
        
        // When
        val result = useCase.getSubscriptionsByCategory(category).first()
        
        // Then
        assertEquals(subscriptions, result)
        verify { repository.getSubscriptionsByCategory(category) }
    }
    
    @Test
    fun `getSubscriptionsByCategory throws exception for blank category`() {
        // Given
        val blankCategory = ""
        
        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            useCase.getSubscriptionsByCategory(blankCategory)
        }
    }
    
    @Test
    fun `searchSubscriptions returns matching subscriptions`() = runTest {
        // Given
        val query = "Netflix"
        val subscriptions = listOf(testSubscription)
        every { repository.searchSubscriptions(query) } returns flowOf(subscriptions)
        
        // When
        val result = useCase.searchSubscriptions(query).first()
        
        // Then
        assertEquals(subscriptions, result)
        verify { repository.searchSubscriptions(query) }
    }
    
    @Test
    fun `searchSubscriptions throws exception for blank query`() {
        // Given
        val blankQuery = ""
        
        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            useCase.searchSubscriptions(blankQuery)
        }
    }
    
    @Test
    fun `getUpcomingRenewals returns subscriptions with upcoming billing dates`() = runTest {
        // Given
        val subscriptions = listOf(
            testSubscription.copy(nextBillingDate = LocalDate.now().plusDays(5)),
            testSubscription.copy(
                id = "test-id-2",
                nextBillingDate = LocalDate.now().plusDays(10)
            )
        )
        every { repository.getActiveSubscriptions() } returns flowOf(subscriptions)
        
        // When
        val result = useCase.getUpcomingRenewals(7).first()
        
        // Then
        assertEquals(1, result.size)
        assertEquals("test-id", result[0].id)
        verify { repository.getActiveSubscriptions() }
    }
    
    @Test
    fun `getUpcomingRenewals throws exception for non-positive days`() {
        // Given
        val invalidDays = 0
        
        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            useCase.getUpcomingRenewals(invalidDays)
        }
    }
    
    @Test
    fun `getTrialSubscriptions returns only trial subscriptions`() = runTest {
        // Given
        val trialSubscription = testSubscription.copy(
            status = SubscriptionStatus.TRIAL,
            trialEndDate = LocalDate.now().plusDays(14)
        )
        val subscriptions = listOf(testSubscription, trialSubscription)
        every { repository.getActiveSubscriptions() } returns flowOf(subscriptions)
        
        // When
        val result = useCase.getTrialSubscriptions().first()
        
        // Then
        assertEquals(1, result.size)
        assertEquals(SubscriptionStatus.TRIAL, result[0].status)
        verify { repository.getActiveSubscriptions() }
    }
    
    @Test
    fun `getSubscriptionsGroupedByCategory groups subscriptions correctly`() = runTest {
        // Given
        val subscription1 = testSubscription.copy(category = "Entertainment")
        val subscription2 = testSubscription.copy(id = "test-id-2", category = "Productivity")
        val subscription3 = testSubscription.copy(id = "test-id-3", category = "")
        val subscriptions = listOf(subscription1, subscription2, subscription3)
        every { repository.getAllSubscriptions() } returns flowOf(subscriptions)
        
        // When
        val result = useCase.getSubscriptionsGroupedByCategory().first()
        
        // Then
        assertEquals(3, result.size)
        assertEquals(1, result["Entertainment"]?.size)
        assertEquals(1, result["Productivity"]?.size)
        assertEquals(1, result["Other"]?.size)
        verify { repository.getAllSubscriptions() }
    }
}
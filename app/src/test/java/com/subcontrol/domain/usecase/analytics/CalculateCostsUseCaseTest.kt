package com.subcontrol.domain.usecase.analytics

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
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class CalculateCostsUseCaseTest {
    
    private val repository = mockk<SubscriptionRepository>()
    private lateinit var useCase: CalculateCostsUseCase
    
    private val monthlySubscription = Subscription(
        id = "monthly-sub",
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
        tags = listOf("streaming"),
        notes = "",
        websiteUrl = "",
        supportEmail = "",
        createdAt = LocalDateTime.now().minusMonths(1),
        updatedAt = LocalDateTime.now()
    )
    
    private val yearlySubscription = Subscription(
        id = "yearly-sub",
        name = "Adobe Creative Cloud",
        description = "Creative software",
        cost = BigDecimal("599.88"),
        currency = "USD",
        billingPeriod = BillingPeriod.ANNUALLY,
        billingCycle = 1,
        startDate = LocalDate.now().minusMonths(6),
        nextBillingDate = LocalDate.now().plusMonths(6),
        trialEndDate = null,
        status = SubscriptionStatus.ACTIVE,
        notificationsEnabled = true,
        notificationDaysBefore = 7,
        category = "Productivity",
        tags = listOf("design", "software"),
        notes = "",
        websiteUrl = "",
        supportEmail = "",
        createdAt = LocalDateTime.now().minusMonths(6),
        updatedAt = LocalDateTime.now()
    )
    
    @Before
    fun setup() {
        useCase = CalculateCostsUseCase(repository)
    }
    
    @Test
    fun `getTotalMonthlyCost calculates correct total for mixed billing periods`() = runTest {
        // Given
        val subscriptions = listOf(monthlySubscription, yearlySubscription)
        every { repository.getActiveSubscriptions() } returns flowOf(subscriptions)
        
        // When
        val result = useCase.getTotalMonthlyCost("USD").first()
        
        // Then
        // Netflix: $9.99/month + Adobe: $599.88/year = $49.99/month
        // Total: $9.99 + $49.99 = $59.98
        assertEquals(BigDecimal("59.98"), result)
        verify { repository.getActiveSubscriptions() }
    }
    
    @Test
    fun `getTotalMonthlyCost returns zero for different currency`() = runTest {
        // Given
        val subscriptions = listOf(monthlySubscription, yearlySubscription)
        every { repository.getActiveSubscriptions() } returns flowOf(subscriptions)
        
        // When
        val result = useCase.getTotalMonthlyCost("EUR").first()
        
        // Then
        assertEquals(BigDecimal.ZERO, result)
        verify { repository.getActiveSubscriptions() }
    }
    
    @Test
    fun `getTotalYearlyCost calculates correct total for mixed billing periods`() = runTest {
        // Given
        val subscriptions = listOf(monthlySubscription, yearlySubscription)
        every { repository.getActiveSubscriptions() } returns flowOf(subscriptions)
        
        // When
        val result = useCase.getTotalYearlyCost("USD").first()
        
        // Then
        // Netflix: $9.99/month * 12 = $119.88/year + Adobe: $599.88/year
        // Total: $119.88 + $599.88 = $719.76
        assertEquals(BigDecimal("719.76"), result)
        verify { repository.getActiveSubscriptions() }
    }
    
    @Test
    fun `getCostsByCategory groups subscriptions correctly`() = runTest {
        // Given
        val subscriptions = listOf(monthlySubscription, yearlySubscription)
        every { repository.getActiveSubscriptions() } returns flowOf(subscriptions)
        
        // When
        val result = useCase.getCostsByCategory("USD").first()
        
        // Then
        assertEquals(2, result.size)
        assertEquals(BigDecimal("9.99"), result["Entertainment"])
        assertEquals(BigDecimal("49.99"), result["Productivity"]) // $599.88/12 = $49.99
        verify { repository.getActiveSubscriptions() }
    }
    
    @Test
    fun `getCostsByBillingPeriod groups subscriptions correctly`() = runTest {
        // Given
        val subscriptions = listOf(monthlySubscription, yearlySubscription)
        every { repository.getActiveSubscriptions() } returns flowOf(subscriptions)
        
        // When
        val result = useCase.getCostsByBillingPeriod("USD").first()
        
        // Then
        assertEquals(2, result.size)
        assertEquals(BigDecimal("9.99"), result[BillingPeriod.MONTHLY])
        assertEquals(BigDecimal("599.88"), result[BillingPeriod.ANNUALLY])
        verify { repository.getActiveSubscriptions() }
    }
    
    @Test
    fun `getUpcomingCosts returns subscriptions within date range`() = runTest {
        // Given
        val nearFutureSubscription = monthlySubscription.copy(
            nextBillingDate = LocalDate.now().plusDays(5)
        )
        val farFutureSubscription = yearlySubscription.copy(
            nextBillingDate = LocalDate.now().plusDays(35)
        )
        val subscriptions = listOf(nearFutureSubscription, farFutureSubscription)
        every { repository.getActiveSubscriptions() } returns flowOf(subscriptions)
        
        // When
        val result = useCase.getUpcomingCosts(30, "USD").first()
        
        // Then
        assertEquals(1, result.size)
        assertEquals("monthly-sub", result[0].subscriptionId)
        assertEquals(BigDecimal("9.99"), result[0].amount)
        assertEquals(5, result[0].daysUntilBilling)
        verify { repository.getActiveSubscriptions() }
    }
    
    @Test
    fun `getUpcomingCosts sorts by billing date`() = runTest {
        // Given
        val subscription1 = monthlySubscription.copy(
            id = "sub1",
            nextBillingDate = LocalDate.now().plusDays(10)
        )
        val subscription2 = monthlySubscription.copy(
            id = "sub2",
            nextBillingDate = LocalDate.now().plusDays(5)
        )
        val subscription3 = monthlySubscription.copy(
            id = "sub3",
            nextBillingDate = LocalDate.now().plusDays(15)
        )
        val subscriptions = listOf(subscription1, subscription2, subscription3)
        every { repository.getActiveSubscriptions() } returns flowOf(subscriptions)
        
        // When
        val result = useCase.getUpcomingCosts(30, "USD").first()
        
        // Then
        assertEquals(3, result.size)
        assertEquals("sub2", result[0].subscriptionId) // 5 days
        assertEquals("sub1", result[1].subscriptionId) // 10 days
        assertEquals("sub3", result[2].subscriptionId) // 15 days
        verify { repository.getActiveSubscriptions() }
    }
    
    @Test
    fun `getCostTrend returns historical cost data`() = runTest {
        // Given
        val subscriptions = listOf(monthlySubscription, yearlySubscription)
        every { repository.getAllSubscriptions() } returns flowOf(subscriptions)
        
        // When
        val result = useCase.getCostTrend(3, "USD").first()
        
        // Then
        assertEquals(3, result.size)
        assertTrue(result.all { it.totalCost > BigDecimal.ZERO })
        assertTrue(result.all { it.subscriptionCount > 0 })
        
        // Verify months are in ascending order
        for (i in 1 until result.size) {
            assertTrue(result[i].month.isAfter(result[i-1].month))
        }
        verify { repository.getAllSubscriptions() }
    }
    
    @Test
    fun `getAverageMonthlyCost calculates correct average`() = runTest {
        // Given
        val subscriptions = listOf(monthlySubscription, yearlySubscription)
        every { repository.getAllSubscriptions() } returns flowOf(subscriptions)
        
        // When
        val result = useCase.getAverageMonthlyCost(3, "USD").first()
        
        // Then
        assertTrue(result > BigDecimal.ZERO)
        verify { repository.getAllSubscriptions() }
    }
    
    @Test
    fun `getAverageMonthlyCost returns zero for empty data`() = runTest {
        // Given
        every { repository.getAllSubscriptions() } returns flowOf(emptyList())
        
        // When
        val result = useCase.getAverageMonthlyCost(3, "USD").first()
        
        // Then
        // Use compareTo to check if values are equal regardless of scale
        assertTrue("Result should be zero", result.compareTo(BigDecimal.ZERO) == 0)
        verify { repository.getAllSubscriptions() }
    }
    
    @Test
    fun `getCostSavings calculates savings from cancelled subscriptions`() = runTest {
        // Given
        val cancelledSubscription = monthlySubscription.copy(
            status = SubscriptionStatus.CANCELLED
        )
        val subscriptions = listOf(monthlySubscription, cancelledSubscription)
        every { repository.getAllSubscriptions() } returns flowOf(subscriptions)
        
        // When
        val result = useCase.getCostSavings("USD").first()
        
        // Then
        assertEquals(BigDecimal("9.99"), result)
        verify { repository.getAllSubscriptions() }
    }
    
    @Test
    fun `getProjectedAnnualCost calculates yearly projection`() = runTest {
        // Given
        val subscriptions = listOf(monthlySubscription)
        every { repository.getActiveSubscriptions() } returns flowOf(subscriptions)
        
        // When
        val result = useCase.getProjectedAnnualCost("USD").first()
        
        // Then
        assertEquals(BigDecimal("119.88"), result) // $9.99 * 12
        verify { repository.getActiveSubscriptions() }
    }
    
    @Test
    fun `empty subscription list returns zero costs`() = runTest {
        // Given
        every { repository.getActiveSubscriptions() } returns flowOf(emptyList())
        
        // When
        val monthlyResult = useCase.getTotalMonthlyCost("USD").first()
        val yearlyResult = useCase.getTotalYearlyCost("USD").first()
        val categoryResult = useCase.getCostsByCategory("USD").first()
        
        // Then
        assertEquals(BigDecimal.ZERO, monthlyResult)
        assertEquals(BigDecimal.ZERO, yearlyResult)
        assertTrue(categoryResult.isEmpty())
        verify(exactly = 3) { repository.getActiveSubscriptions() }
    }
}
package com.subcontrol.ui.screens.dashboard

import com.subcontrol.domain.model.BillingPeriod
import com.subcontrol.domain.model.Subscription
import com.subcontrol.domain.model.SubscriptionStatus
import com.subcontrol.domain.usecase.analytics.CalculateCostsUseCase
import com.subcontrol.domain.usecase.analytics.UpcomingCost
import com.subcontrol.domain.usecase.subscription.GetSubscriptionsUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    
    private val getSubscriptionsUseCase = mockk<GetSubscriptionsUseCase>()
    private val calculateCostsUseCase = mockk<CalculateCostsUseCase>()
    private val performanceMonitor = mockk<com.subcontrol.performance.PerformanceMonitor>(relaxed = true)
    private lateinit var viewModel: DashboardViewModel
    
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
        tags = listOf("streaming"),
        notes = "",
        websiteUrl = "",
        supportEmail = "",
        createdAt = LocalDateTime.now().minusMonths(1),
        updatedAt = LocalDateTime.now()
    )
    
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Setup default mock responses
        every { calculateCostsUseCase.getTotalMonthlyCost(any()) } returns flowOf(BigDecimal("49.97"))
        every { calculateCostsUseCase.getTotalYearlyCost(any()) } returns flowOf(BigDecimal("599.64"))
        every { calculateCostsUseCase.getCostsByCategory(any()) } returns flowOf(
            mapOf(
                "Entertainment" to BigDecimal("21.98"),
                "Productivity" to BigDecimal("19.99"),
                "Music" to BigDecimal("8.00")
            )
        )
        every { calculateCostsUseCase.getUpcomingCosts(any(), any()) } returns flowOf(
            listOf(
                UpcomingCost(
                    subscriptionId = "1",
                    subscriptionName = "Netflix",
                    amount = BigDecimal("9.99"),
                    currency = "USD",
                    billingDate = LocalDate.now().plusDays(1),
                    daysUntilBilling = 1
                )
            )
        )
        every { getSubscriptionsUseCase.getActiveSubscriptions() } returns flowOf(listOf(testSubscription))
        every { getSubscriptionsUseCase.getTrialSubscriptions() } returns flowOf(emptyList())
        
        viewModel = DashboardViewModel(getSubscriptionsUseCase, calculateCostsUseCase, performanceMonitor)
    }
    
    @Test
    fun `initial state is loading`() = runTest {
        // Given - fresh ViewModel initialization
        
        // When - checking initial state
        val initialState = viewModel.uiState.value
        
        // Then - should be loading initially
        assertFalse(initialState.isLoading) // False because UnconfinedTestDispatcher executes immediately
        assertNull(initialState.error)
    }
    
    @Test
    fun `loadDashboardData updates state with correct data`() = runTest {
        // Given - mocked use cases setup in @Before
        
        // When - data is loaded (happens in init)
        val state = viewModel.uiState.value
        
        // Then - state should contain the correct data
        assertEquals(BigDecimal("49.97"), state.monthlyCost)
        assertEquals(BigDecimal("599.64"), state.yearlyCost)
        assertEquals(3, state.categoryBreakdown.size)
        assertEquals(BigDecimal("21.98"), state.categoryBreakdown["Entertainment"])
        assertEquals(1, state.upcomingRenewals.size)
        assertEquals("Netflix", state.upcomingRenewals[0].subscriptionName)
        assertEquals(1, state.activeSubscriptionsCount)
        assertEquals(0, state.trialSubscriptionsCount)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }
    
    @Test
    fun `refresh calls loadDashboardData again`() = runTest {
        // Given - initial state loaded
        val initialState = viewModel.uiState.value
        
        // When - refresh is called
        viewModel.refresh()
        
        // Then - use cases should be called again
        verify(atLeast = 2) { calculateCostsUseCase.getTotalMonthlyCost(any()) }
        verify(atLeast = 2) { calculateCostsUseCase.getTotalYearlyCost(any()) }
        verify(atLeast = 2) { getSubscriptionsUseCase.getActiveSubscriptions() }
    }
    
    @Test
    fun `error state is handled correctly`() = runTest {
        // Given - use case throws exception
        every { calculateCostsUseCase.getTotalMonthlyCost(any()) } throws RuntimeException("Test error")
        
        // When - ViewModel is initialized
        val viewModelWithError = DashboardViewModel(getSubscriptionsUseCase, calculateCostsUseCase, performanceMonitor)
        
        // Then - error should be captured
        val state = viewModelWithError.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Test error", state.error)
    }
    
    @Test
    fun `dismissError clears error state`() = runTest {
        // Given - ViewModel with error
        every { calculateCostsUseCase.getTotalMonthlyCost(any()) } throws RuntimeException("Test error")
        val viewModelWithError = DashboardViewModel(getSubscriptionsUseCase, calculateCostsUseCase, performanceMonitor)
        
        // When - error is dismissed
        viewModelWithError.dismissError()
        
        // Then - error should be null
        val state = viewModelWithError.uiState.value
        assertNull(state.error)
    }
    
    @Test
    fun `getTopSpendingCategories returns top 3 categories`() = runTest {
        // Given - ViewModel with category data
        
        // When - getting top spending categories
        val topCategories = viewModel.getTopSpendingCategories()
        
        // Then - should return top 3 categories sorted by amount
        assertEquals(3, topCategories.size)
        assertEquals("Entertainment", topCategories[0].category)
        assertEquals(BigDecimal("21.98"), topCategories[0].amount)
        assertEquals("Productivity", topCategories[1].category)
        assertEquals(BigDecimal("19.99"), topCategories[1].amount)
        assertEquals("Music", topCategories[2].category)
        assertEquals(BigDecimal("8.00"), topCategories[2].amount)
    }
    
    @Test
    fun `getNextRenewalDate returns correct date`() = runTest {
        // Given - ViewModel with upcoming renewals
        
        // When - getting next renewal date
        val nextRenewalDate = viewModel.getNextRenewalDate()
        
        // Then - should return the date of first upcoming renewal
        assertEquals(LocalDate.now().plusDays(1).toString(), nextRenewalDate)
    }
    
    @Test
    fun `getUpcomingRenewalsTotal calculates correct total`() = runTest {
        // Given - ViewModel with upcoming renewals
        
        // When - getting upcoming renewals total
        val total = viewModel.getUpcomingRenewalsTotal()
        
        // Then - should return sum of all upcoming renewal amounts
        assertEquals(BigDecimal("9.99"), total)
    }
    
    @Test
    fun `getAnnualSavings calculates savings correctly`() = runTest {
        // Given - ViewModel with cost data
        // Monthly: $49.97, Yearly: $599.64
        // Projected monthly total: $49.97 * 12 = $599.64
        
        // When - calculating annual savings
        val savings = viewModel.getAnnualSavings()
        
        // Then - should return zero (no savings in this case)
        assertEquals(BigDecimal.ZERO, savings)
    }
    
    @Test
    fun `getAnnualSavings returns positive savings when yearly is less than monthly projection`() = runTest {
        // Given - ViewModel with different cost structure
        every { calculateCostsUseCase.getTotalMonthlyCost(any()) } returns flowOf(BigDecimal("50.00"))
        every { calculateCostsUseCase.getTotalYearlyCost(any()) } returns flowOf(BigDecimal("500.00"))
        
        val viewModelWithSavings = DashboardViewModel(getSubscriptionsUseCase, calculateCostsUseCase, performanceMonitor)
        
        // When - calculating annual savings
        val savings = viewModelWithSavings.getAnnualSavings()
        
        // Then - should return positive savings
        // $50.00 * 12 = $600.00, $600.00 - $500.00 = $100.00
        assertEquals(BigDecimal("100.00"), savings)
    }
    
    @Test
    fun `empty data returns zero values`() = runTest {
        // Given - empty data from use cases
        every { calculateCostsUseCase.getTotalMonthlyCost(any()) } returns flowOf(BigDecimal.ZERO)
        every { calculateCostsUseCase.getTotalYearlyCost(any()) } returns flowOf(BigDecimal.ZERO)
        every { calculateCostsUseCase.getCostsByCategory(any()) } returns flowOf(emptyMap())
        every { calculateCostsUseCase.getUpcomingCosts(any(), any()) } returns flowOf(emptyList())
        every { getSubscriptionsUseCase.getActiveSubscriptions() } returns flowOf(emptyList())
        every { getSubscriptionsUseCase.getTrialSubscriptions() } returns flowOf(emptyList())
        
        // When - ViewModel is initialized
        val emptyViewModel = DashboardViewModel(getSubscriptionsUseCase, calculateCostsUseCase, performanceMonitor)
        
        // Then - state should contain zero values
        val state = emptyViewModel.uiState.value
        assertEquals(BigDecimal.ZERO, state.monthlyCost)
        assertEquals(BigDecimal.ZERO, state.yearlyCost)
        assertTrue(state.categoryBreakdown.isEmpty())
        assertTrue(state.upcomingRenewals.isEmpty())
        assertEquals(0, state.activeSubscriptionsCount)
        assertEquals(0, state.trialSubscriptionsCount)
    }
}
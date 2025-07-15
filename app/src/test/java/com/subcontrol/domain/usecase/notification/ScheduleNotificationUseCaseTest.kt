package com.subcontrol.domain.usecase.notification

import com.subcontrol.data.notification.NotificationScheduler
import com.subcontrol.domain.model.BillingPeriod
import com.subcontrol.domain.model.Subscription
import com.subcontrol.domain.model.SubscriptionStatus
import com.subcontrol.domain.repository.SubscriptionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class ScheduleNotificationUseCaseTest {
    
    private val notificationScheduler = mockk<NotificationScheduler>()
    private val subscriptionRepository = mockk<SubscriptionRepository>()
    private lateinit var useCase: ScheduleNotificationUseCase
    
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
        trialEndDate = LocalDate.now().plusDays(14),
        status = SubscriptionStatus.TRIAL,
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
    
    @Before
    fun setup() {
        useCase = ScheduleNotificationUseCase(notificationScheduler, subscriptionRepository)
    }
    
    @Test
    fun `scheduleNotifications schedules both renewal and trial notifications`() = runTest {
        // Given
        coEvery { notificationScheduler.scheduleRenewalNotification(testSubscription) } returns Result.success(Unit)
        coEvery { notificationScheduler.scheduleTrialEndingNotification(testSubscription) } returns Result.success(Unit)
        
        // When
        val result = useCase.scheduleNotifications(testSubscription)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { notificationScheduler.scheduleRenewalNotification(testSubscription) }
        coVerify { notificationScheduler.scheduleTrialEndingNotification(testSubscription) }
    }
    
    @Test
    fun `scheduleNotifications skips notifications when disabled`() = runTest {
        // Given
        val subscriptionWithNotificationsDisabled = testSubscription.copy(notificationsEnabled = false)
        
        // When
        val result = useCase.scheduleNotifications(subscriptionWithNotificationsDisabled)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { notificationScheduler.scheduleRenewalNotification(any()) }
        coVerify(exactly = 0) { notificationScheduler.scheduleTrialEndingNotification(any()) }
    }
    
    @Test
    fun `scheduleNotifications only schedules renewal when no trial`() = runTest {
        // Given
        val subscriptionWithoutTrial = testSubscription.copy(trialEndDate = null)
        coEvery { notificationScheduler.scheduleRenewalNotification(subscriptionWithoutTrial) } returns Result.success(Unit)
        
        // When
        val result = useCase.scheduleNotifications(subscriptionWithoutTrial)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { notificationScheduler.scheduleRenewalNotification(subscriptionWithoutTrial) }
        coVerify(exactly = 0) { notificationScheduler.scheduleTrialEndingNotification(any()) }
    }
    
    @Test
    fun `scheduleNotifications fails when renewal scheduling fails`() = runTest {
        // Given
        val error = RuntimeException("Scheduling failed")
        coEvery { notificationScheduler.scheduleRenewalNotification(testSubscription) } returns Result.failure(error)
        
        // When
        val result = useCase.scheduleNotifications(testSubscription)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Scheduling failed", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `scheduleNotifications fails when trial scheduling fails`() = runTest {
        // Given
        val error = RuntimeException("Trial scheduling failed")
        coEvery { notificationScheduler.scheduleRenewalNotification(testSubscription) } returns Result.success(Unit)
        coEvery { notificationScheduler.scheduleTrialEndingNotification(testSubscription) } returns Result.failure(error)
        
        // When
        val result = useCase.scheduleNotifications(testSubscription)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Trial scheduling failed", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `scheduleAllNotifications schedules for all active subscriptions`() = runTest {
        // Given
        val subscriptions = listOf(testSubscription, testSubscription.copy(id = "test-id-2"))
        every { subscriptionRepository.getActiveSubscriptions() } returns flowOf(subscriptions)
        coEvery { notificationScheduler.scheduleRenewalNotification(any()) } returns Result.success(Unit)
        coEvery { notificationScheduler.scheduleTrialEndingNotification(any()) } returns Result.success(Unit)
        
        // When
        val result = useCase.scheduleAllNotifications()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
        coVerify(exactly = 2) { notificationScheduler.scheduleRenewalNotification(any()) }
        coVerify(exactly = 2) { notificationScheduler.scheduleTrialEndingNotification(any()) }
    }
    
    @Test
    fun `scheduleAllNotifications handles partial failures`() = runTest {
        // Given
        val subscription1 = testSubscription.copy(id = "test-id-1")
        val subscription2 = testSubscription.copy(id = "test-id-2")
        val subscriptions = listOf(subscription1, subscription2)
        every { subscriptionRepository.getActiveSubscriptions() } returns flowOf(subscriptions)
        coEvery { notificationScheduler.scheduleRenewalNotification(subscription1) } returns Result.success(Unit)
        coEvery { notificationScheduler.scheduleTrialEndingNotification(subscription1) } returns Result.success(Unit)
        coEvery { notificationScheduler.scheduleRenewalNotification(subscription2) } returns Result.failure(RuntimeException("Error"))
        
        // When
        val result = useCase.scheduleAllNotifications()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()) // Only one subscription scheduled successfully
    }
    
    @Test
    fun `cancelNotifications cancels notifications for subscription`() = runTest {
        // Given
        val subscriptionId = "test-id"
        coEvery { notificationScheduler.cancelNotifications(subscriptionId) } returns Result.success(Unit)
        
        // When
        val result = useCase.cancelNotifications(subscriptionId)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { notificationScheduler.cancelNotifications(subscriptionId) }
    }
    
    @Test
    fun `cancelNotifications fails with blank id`() = runTest {
        // Given
        val blankId = ""
        
        // When
        val result = useCase.cancelNotifications(blankId)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Subscription ID cannot be blank", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `rescheduleNotifications reschedules notifications for subscription`() = runTest {
        // Given
        coEvery { notificationScheduler.rescheduleNotifications(testSubscription) } returns Result.success(Unit)
        
        // When
        val result = useCase.rescheduleNotifications(testSubscription)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { notificationScheduler.rescheduleNotifications(testSubscription) }
    }
    
    @Test
    fun `cancelAllNotifications cancels notifications for all subscriptions`() = runTest {
        // Given
        val subscriptions = listOf(
            testSubscription.copy(id = "test-id-1"),
            testSubscription.copy(id = "test-id-2")
        )
        every { subscriptionRepository.getAllSubscriptions() } returns flowOf(subscriptions)
        coEvery { notificationScheduler.cancelNotifications(any()) } returns Result.success(Unit)
        
        // When
        val result = useCase.cancelAllNotifications()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
        coVerify(exactly = 2) { notificationScheduler.cancelNotifications(any()) }
    }
    
    @Test
    fun `scheduleUpcomingRenewalNotifications schedules for upcoming renewals only`() = runTest {
        // Given
        val upcomingSubscription = testSubscription.copy(
            id = "upcoming",
            nextBillingDate = LocalDate.now().plusDays(3)
        )
        val farSubscription = testSubscription.copy(
            id = "far",
            nextBillingDate = LocalDate.now().plusDays(15)
        )
        val subscriptions = listOf(upcomingSubscription, farSubscription)
        every { subscriptionRepository.getActiveSubscriptions() } returns flowOf(subscriptions)
        coEvery { notificationScheduler.scheduleRenewalNotification(any()) } returns Result.success(Unit)
        coEvery { notificationScheduler.scheduleTrialEndingNotification(any()) } returns Result.success(Unit)
        
        // When
        val result = useCase.scheduleUpcomingRenewalNotifications(7)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()) // Only upcoming subscription scheduled
    }
    
    @Test
    fun `scheduleUpcomingRenewalNotifications fails with non-positive days`() = runTest {
        // Given
        val invalidDays = 0
        
        // When
        val result = useCase.scheduleUpcomingRenewalNotifications(invalidDays)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Days must be positive", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `scheduleEndingTrialNotifications schedules for ending trials only`() = runTest {
        // Given
        val endingTrialSubscription = testSubscription.copy(
            id = "ending-trial",
            trialEndDate = LocalDate.now().plusDays(2)
        )
        val longTrialSubscription = testSubscription.copy(
            id = "long-trial",
            trialEndDate = LocalDate.now().plusDays(15)
        )
        val subscriptions = listOf(endingTrialSubscription, longTrialSubscription)
        every { subscriptionRepository.getActiveSubscriptions() } returns flowOf(subscriptions)
        coEvery { notificationScheduler.scheduleTrialEndingNotification(any()) } returns Result.success(Unit)
        
        // When
        val result = useCase.scheduleEndingTrialNotifications(7)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()) // Only ending trial subscription scheduled
    }
    
    @Test
    fun `scheduleEndingTrialNotifications fails with non-positive days`() = runTest {
        // Given
        val invalidDays = -1
        
        // When
        val result = useCase.scheduleEndingTrialNotifications(invalidDays)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Days must be positive", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `exception handling propagates errors correctly`() = runTest {
        // Given
        val error = RuntimeException("Repository error")
        every { subscriptionRepository.getActiveSubscriptions() } throws error
        
        // When
        val result = useCase.scheduleAllNotifications()
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Repository error", result.exceptionOrNull()?.message)
    }
}
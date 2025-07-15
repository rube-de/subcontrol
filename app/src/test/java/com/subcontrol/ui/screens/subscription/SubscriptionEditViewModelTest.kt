package com.subcontrol.ui.screens.subscription

import app.cash.turbine.test
import com.subcontrol.domain.model.BillingPeriod
import com.subcontrol.domain.model.Category
import com.subcontrol.domain.model.Subscription
import com.subcontrol.domain.model.SubscriptionStatus
import com.subcontrol.domain.repository.CategoryRepository
import com.subcontrol.domain.repository.SubscriptionRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Unit tests for SubscriptionEditViewModel.
 * 
 * Tests the ViewModel's form validation, state management,
 * and save functionality for subscription editing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionEditViewModelTest {

    @MockK
    private lateinit var subscriptionRepository: SubscriptionRepository

    @MockK
    private lateinit var categoryRepository: CategoryRepository

    private lateinit var viewModel: SubscriptionEditViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    private val testSubscription = Subscription(
        id = "test-id",
        name = "Netflix",
        description = "Streaming service",
        cost = BigDecimal("9.99"),
        currency = "USD",
        billingPeriod = BillingPeriod.MONTHLY,
        billingCycle = 1,
        startDate = LocalDate.of(2023, 1, 1),
        nextBillingDate = LocalDate.of(2023, 2, 1),
        trialEndDate = null,
        status = SubscriptionStatus.ACTIVE,
        notificationsEnabled = true,
        notificationDaysBefore = 3,
        category = "Entertainment",
        tags = listOf("video", "streaming"),
        notes = "Family plan",
        websiteUrl = "https://netflix.com",
        supportEmail = "support@netflix.com",
        createdAt = LocalDateTime.of(2023, 1, 1, 10, 0),
        updatedAt = LocalDateTime.of(2023, 1, 1, 10, 0)
    )

    private val testCategory = Category(
        id = "cat-id",
        name = "Entertainment",
        color = "#FF5722",
        icon = "tv",
        sortOrder = 0,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        
        // Default mock behavior
        coEvery { categoryRepository.getAllCategories() } returns flowOf(listOf(testCategory))
        
        viewModel = SubscriptionEditViewModel(subscriptionRepository, categoryRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has default values`() = runTest {
        // When & Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.name)
            assertEquals("", state.description)
            assertEquals("", state.cost)
            assertEquals("USD", state.currency)
            assertEquals(BillingPeriod.MONTHLY, state.billingPeriod)
            assertEquals(LocalDate.now(), state.startDate)
            assertFalse(state.isEditMode)
            assertTrue(state.notificationsEnabled)
            assertEquals("3", state.notificationDaysBefore)
        }
    }

    @Test
    fun `loadSubscription sets edit mode and populates fields`() = runTest {
        // Given
        coEvery { subscriptionRepository.getSubscriptionById("test-id") } returns flowOf(testSubscription)

        // When
        viewModel.loadSubscription("test-id")

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue("Should be in edit mode", state.isEditMode)
            assertEquals(testSubscription.name, state.name)
            assertEquals(testSubscription.description, state.description)
            assertEquals(testSubscription.cost.toString(), state.cost)
            assertEquals(testSubscription.currency, state.currency)
            assertEquals(testSubscription.billingPeriod, state.billingPeriod)
            assertEquals(testSubscription.category, state.category)
            assertEquals(testSubscription.tags.joinToString(", "), state.tags)
            assertEquals(testSubscription, state.originalSubscription)
        }
    }

    @Test
    fun `loadSubscription handles non-existing subscription`() = runTest {
        // Given
        coEvery { subscriptionRepository.getSubscriptionById("non-existing") } returns flowOf(null)

        // When
        viewModel.loadSubscription("non-existing")

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse("Should not be loading", state.isLoading)
            assertTrue("Should have error", state.error?.contains("not found") == true)
        }
    }

    @Test
    fun `updateName validates required field`() = runTest {
        // When
        viewModel.updateName("")

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.name)
            assertEquals("Name is required", state.nameError)
        }
    }

    @Test
    fun `updateName clears error for valid input`() = runTest {
        // Given - Set error first
        viewModel.updateName("")

        // When
        viewModel.updateName("Netflix")

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Netflix", state.name)
            assertNull("Name error should be cleared", state.nameError)
        }
    }

    @Test
    fun `updateCost validates numeric input`() = runTest {
        // When
        viewModel.updateCost("invalid")

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("invalid", state.cost)
            assertEquals("Invalid cost format", state.costError)
        }
    }

    @Test
    fun `updateCost validates positive numbers`() = runTest {
        // When
        viewModel.updateCost("-5.99")

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("-5.99", state.cost)
            assertEquals("Cost cannot be negative", state.costError)
        }
    }

    @Test
    fun `updateCost accepts valid positive numbers`() = runTest {
        // When
        viewModel.updateCost("9.99")

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("9.99", state.cost)
            assertNull("Cost error should be cleared", state.costError)
        }
    }

    @Test
    fun `updateBillingPeriod updates state`() = runTest {
        // When
        viewModel.updateBillingPeriod(BillingPeriod.ANNUALLY)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(BillingPeriod.ANNUALLY, state.billingPeriod)
        }
    }

    @Test
    fun `updateBillingCycle validates for custom period`() = runTest {
        // Given
        viewModel.updateBillingPeriod(BillingPeriod.CUSTOM)

        // When
        viewModel.updateBillingCycle("0")

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("0", state.billingCycle)
            assertEquals("Billing cycle must be positive", state.billingCycleError)
        }
    }

    @Test
    fun `updateNotificationDaysBefore validates numeric input`() = runTest {
        // When
        viewModel.updateNotificationDaysBefore("invalid")

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("invalid", state.notificationDaysBefore)
            assertEquals("Invalid days format", state.notificationDaysError)
        }
    }

    @Test
    fun `updateNotificationDaysBefore validates non-negative numbers`() = runTest {
        // When
        viewModel.updateNotificationDaysBefore("-1")

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("-1", state.notificationDaysBefore)
            assertEquals("Days before cannot be negative", state.notificationDaysError)
        }
    }

    @Test
    fun `clearError clears error state`() = runTest {
        // Given - Set an error first
        viewModel.updateName("")

        // When
        viewModel.clearError()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull("Error should be cleared", state.error)
            // Note: Field validation errors remain, only general error is cleared
        }
    }

    @Test
    fun `saveSubscription validates form before saving`() = runTest {
        // Given - Invalid form
        viewModel.updateName("")
        viewModel.updateCost("")

        // When
        viewModel.saveSubscription()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse("Should not be saving with invalid form", state.isSaving)
            assertEquals("Name is required", state.nameError)
            assertEquals("Cost is required", state.costError)
        }
    }

    @Test
    fun `saveSubscription creates new subscription when not in edit mode`() = runTest {
        // Given - Valid form
        viewModel.updateName("Netflix")
        viewModel.updateCost("9.99")
        coEvery { subscriptionRepository.addSubscription(any()) } returns Result.success(Unit)

        // When
        viewModel.saveSubscription()

        // Then
        coVerify { subscriptionRepository.addSubscription(any()) }
        
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue("Should be saved", state.isSaved)
            assertFalse("Should not be saving", state.isSaving)
        }
    }

    @Test
    fun `saveSubscription updates existing subscription in edit mode`() = runTest {
        // Given - Load existing subscription first
        coEvery { subscriptionRepository.getSubscriptionById("test-id") } returns flowOf(testSubscription)
        viewModel.loadSubscription("test-id")
        
        // Update some fields
        viewModel.updateName("Netflix Premium")
        coEvery { subscriptionRepository.updateSubscription(any()) } returns Result.success(Unit)

        // When
        viewModel.saveSubscription()

        // Then
        coVerify { subscriptionRepository.updateSubscription(any()) }
        
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue("Should be saved", state.isSaved)
        }
    }

    @Test
    fun `saveSubscription handles repository failure`() = runTest {
        // Given - Valid form
        viewModel.updateName("Netflix")
        viewModel.updateCost("9.99")
        val errorMessage = "Save failed"
        coEvery { subscriptionRepository.addSubscription(any()) } returns Result.failure(Exception(errorMessage))

        // When
        viewModel.saveSubscription()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse("Should not be saved", state.isSaved)
            assertFalse("Should not be saving", state.isSaving)
            assertTrue("Should have error", state.error?.contains(errorMessage) == true)
        }
    }

    @Test
    fun `categories are loaded on initialization`() = runTest {
        // Given - Mock returns categories
        val categories = listOf(testCategory)
        coEvery { categoryRepository.getAllCategories() } returns flowOf(categories)

        // When
        val newViewModel = SubscriptionEditViewModel(subscriptionRepository, categoryRepository)

        // Then
        newViewModel.categories.test {
            val result = awaitItem()
            assertEquals(listOf("Entertainment"), result)
        }
    }

    @Test
    fun `isFormValid returns true for valid form`() = runTest {
        // Given - Valid form
        viewModel.updateName("Netflix")
        viewModel.updateCost("9.99")

        // When & Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue("Form should be valid", state.isFormValid)
        }
    }

    @Test
    fun `isFormValid returns false for invalid form`() = runTest {
        // Given - Invalid form (empty name)
        viewModel.updateName("")
        viewModel.updateCost("9.99")

        // When & Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse("Form should be invalid", state.isFormValid)
        }
    }
}
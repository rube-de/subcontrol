package com.subcontrol.data.repository

import app.cash.turbine.test
import com.subcontrol.domain.model.BillingPeriod
import com.subcontrol.domain.model.Subscription
import com.subcontrol.domain.model.SubscriptionStatus
import androidx.datastore.core.DataStore
import com.subcontrol.data.mapper.SubscriptionMapper.toProto
import com.subcontrol.data.model.proto.AppData
import com.subcontrol.data.model.proto.SubscriptionList
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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
 * Unit tests for SubscriptionRepositoryImpl.
 * 
 * Tests the repository implementation to ensure proper data handling
 * and business logic enforcement.
 */
class SubscriptionRepositoryImplTest {

    @MockK
    private lateinit var dataStore: DataStore<AppData>

    private lateinit var repository: SubscriptionRepositoryImpl

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

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }
    
    private fun createAppData(subscriptions: List<Subscription>): AppData {
        val protoSubscriptions = subscriptions.map { it.toProto() }
        return AppData.newBuilder()
            .setSubscriptionList(
                SubscriptionList.newBuilder()
                    .addAllSubscriptions(protoSubscriptions)
                    .build()
            )
            .build()
    }

    @Test
    fun `getAllSubscriptions returns all subscriptions`() = runTest {
        // Given
        repository = SubscriptionRepositoryImpl(dataStore, UnconfinedTestDispatcher(testScheduler))
        val subscriptions = listOf(testSubscription)
        coEvery { dataStore.data } returns flowOf(createAppData(subscriptions))

        // When & Then
        repository.getAllSubscriptions().test {
            val result = awaitItem()
            assertEquals(subscriptions, result)
            awaitComplete()
        }
    }

    @Test
    fun `getActiveSubscriptions returns only active subscriptions`() = runTest {
        // Given
        repository = SubscriptionRepositoryImpl(dataStore, UnconfinedTestDispatcher(testScheduler))
        val activeSubscription = testSubscription.copy(status = SubscriptionStatus.ACTIVE)
        val cancelledSubscription = testSubscription.copy(
            id = "cancelled-id",
            status = SubscriptionStatus.CANCELLED
        )
        val subscriptions = listOf(activeSubscription, cancelledSubscription)
        coEvery { dataStore.data } returns flowOf(createAppData(subscriptions))

        // When & Then
        repository.getActiveSubscriptions().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(activeSubscription, result[0])
            awaitComplete()
        }
    }

    @Test
    fun `getSubscriptionById returns correct subscription`() = runTest {
        // Given
        repository = SubscriptionRepositoryImpl(dataStore, UnconfinedTestDispatcher(testScheduler))
        val subscriptions = listOf(testSubscription)
        coEvery { dataStore.data } returns flowOf(createAppData(subscriptions))

        // When & Then
        repository.getSubscriptionById("test-id").test {
            val result = awaitItem()
            assertEquals(testSubscription, result)
            awaitComplete()
        }
    }

    @Test
    fun `getSubscriptionById returns null for non-existing id`() = runTest {
        // Given
        repository = SubscriptionRepositoryImpl(dataStore, UnconfinedTestDispatcher(testScheduler))
        val subscriptions = listOf(testSubscription)
        coEvery { dataStore.data } returns flowOf(createAppData(subscriptions))

        // When & Then
        repository.getSubscriptionById("non-existing-id").test {
            val result = awaitItem()
            assertNull(result)
            awaitComplete()
        }
    }

    @Test
    fun `getSubscriptionsByCategory returns filtered subscriptions`() = runTest {
        // Given
        repository = SubscriptionRepositoryImpl(dataStore, UnconfinedTestDispatcher(testScheduler))
        val entertainmentSubscription = testSubscription.copy(category = "Entertainment")
        val softwareSubscription = testSubscription.copy(
            id = "software-id",
            category = "Software"
        )
        val subscriptions = listOf(entertainmentSubscription, softwareSubscription)
        coEvery { dataStore.data } returns flowOf(createAppData(subscriptions))

        // When & Then
        repository.getSubscriptionsByCategory("Entertainment").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(entertainmentSubscription, result[0])
            awaitComplete()
        }
    }

    @Test
    fun `searchSubscriptions returns matching subscriptions`() = runTest {
        // Given
        repository = SubscriptionRepositoryImpl(dataStore, UnconfinedTestDispatcher(testScheduler))
        val netflixSubscription = testSubscription.copy(name = "Netflix")
        val spotifySubscription = testSubscription.copy(
            id = "spotify-id",
            name = "Spotify"
        )
        val subscriptions = listOf(netflixSubscription, spotifySubscription)
        coEvery { dataStore.data } returns flowOf(createAppData(subscriptions))

        // When & Then
        repository.searchSubscriptions("net").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(netflixSubscription, result[0])
            awaitComplete()
        }
    }

    @Test
    fun `searchSubscriptions is case insensitive`() = runTest {
        // Given
        repository = SubscriptionRepositoryImpl(dataStore, UnconfinedTestDispatcher(testScheduler))
        val netflixSubscription = testSubscription.copy(name = "Netflix")
        val subscriptions = listOf(netflixSubscription)
        coEvery { dataStore.data } returns flowOf(createAppData(subscriptions))

        // When & Then
        repository.searchSubscriptions("NETFLIX").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(netflixSubscription, result[0])
            awaitComplete()
        }
    }

    @Test
    fun `addSubscription succeeds with valid data`() = runTest {
        // Given
        repository = SubscriptionRepositoryImpl(dataStore, UnconfinedTestDispatcher(testScheduler))
        coEvery { dataStore.updateData(any<suspend (AppData) -> AppData>()) } returns createAppData(listOf(testSubscription))

        // When
        val result = repository.addSubscription(testSubscription)

        // Then
        assertTrue("Add subscription should succeed", result.isSuccess)
        coVerify { dataStore.updateData(any()) }
    }

    @Test
    fun `updateSubscription succeeds with valid data`() = runTest {
        // Given
        repository = SubscriptionRepositoryImpl(dataStore, UnconfinedTestDispatcher(testScheduler))
        val updatedSubscription = testSubscription.copy(name = "Netflix Premium")
        coEvery { dataStore.data } returns flowOf(createAppData(listOf(testSubscription)))
        coEvery { dataStore.updateData(any<suspend (AppData) -> AppData>()) } returns createAppData(listOf(updatedSubscription))

        // When
        val result = repository.updateSubscription(updatedSubscription)

        // Then
        assertTrue("Update subscription should succeed", result.isSuccess)
        coVerify { dataStore.updateData(any()) }
    }

    @Test
    fun `deleteSubscription succeeds with existing id`() = runTest {
        // Given
        repository = SubscriptionRepositoryImpl(dataStore, UnconfinedTestDispatcher(testScheduler))
        coEvery { dataStore.data } returns flowOf(createAppData(listOf(testSubscription)))
        coEvery { dataStore.updateData(any<suspend (AppData) -> AppData>()) } returns createAppData(emptyList())

        // When
        val result = repository.deleteSubscription("test-id")

        // Then
        assertTrue("Delete subscription should succeed", result.isSuccess)
        coVerify { dataStore.updateData(any()) }
    }

    @Test
    fun `deleteAllSubscriptions clears all data`() = runTest {
        // Given
        repository = SubscriptionRepositoryImpl(dataStore, UnconfinedTestDispatcher(testScheduler))
        coEvery { dataStore.updateData(any<suspend (AppData) -> AppData>()) } returns createAppData(emptyList())

        // When
        val result = repository.deleteAllSubscriptions()

        // Then
        assertTrue("Delete all subscriptions should succeed", result.isSuccess)
        coVerify { dataStore.updateData(any()) }
    }

    @Test
    fun `updateBillingDate updates next billing date`() = runTest {
        // Given
        repository = SubscriptionRepositoryImpl(dataStore, UnconfinedTestDispatcher(testScheduler))
        val newBillingDate = LocalDate.of(2023, 3, 1)
        val updatedSubscription = testSubscription.copy(nextBillingDate = newBillingDate)
        coEvery { dataStore.data } returns flowOf(createAppData(listOf(testSubscription)))
        coEvery { dataStore.updateData(any<suspend (AppData) -> AppData>()) } returns createAppData(listOf(updatedSubscription))

        // When
        val result = repository.updateBillingDate("test-id", newBillingDate)

        // Then
        assertTrue("Update billing date should succeed", result.isSuccess)
        coVerify { dataStore.updateData(any()) }
    }

    @Test
    fun `updateStatus updates subscription status`() = runTest {
        // Given
        repository = SubscriptionRepositoryImpl(dataStore, UnconfinedTestDispatcher(testScheduler))
        val newStatus = SubscriptionStatus.PAUSED
        val updatedSubscription = testSubscription.copy(status = newStatus)
        coEvery { dataStore.data } returns flowOf(createAppData(listOf(testSubscription)))
        coEvery { dataStore.updateData(any<suspend (AppData) -> AppData>()) } returns createAppData(listOf(updatedSubscription))

        // When
        val result = repository.updateStatus("test-id", newStatus)

        // Then
        assertTrue("Update status should succeed", result.isSuccess)
        coVerify { dataStore.updateData(any()) }
    }

    @Test
    fun `updateStatus returns failure for non-existing subscription`() = runTest {
        // Given
        repository = SubscriptionRepositoryImpl(dataStore, UnconfinedTestDispatcher(testScheduler))
        coEvery { dataStore.data } returns flowOf(createAppData(emptyList()))

        // When
        val result = repository.updateStatus("non-existing-id", SubscriptionStatus.PAUSED)

        // Then
        assertFalse("Update status should fail for non-existing subscription", result.isSuccess)
    }

    @Test
    fun `repository handles datastore exceptions gracefully`() = runTest {
        // Given
        repository = SubscriptionRepositoryImpl(dataStore, UnconfinedTestDispatcher(testScheduler))
        coEvery { dataStore.data } returns flow { throw Exception("DataStore error") }

        // When & Then
        repository.getAllSubscriptions().test {
            // Repository catches exceptions and emits empty list
            val result = awaitItem()
            assertEquals(emptyList<Subscription>(), result)
            awaitComplete()
        }
    }
}

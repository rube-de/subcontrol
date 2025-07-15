package com.subcontrol.di

import com.subcontrol.data.repository.BudgetRepositoryImpl
import com.subcontrol.data.repository.CategoryRepositoryImpl
import com.subcontrol.data.repository.PreferencesRepositoryImpl
import com.subcontrol.data.repository.SubscriptionRepositoryImpl
import com.subcontrol.domain.repository.BudgetRepository
import com.subcontrol.domain.repository.CategoryRepository
import com.subcontrol.domain.repository.PreferencesRepository
import com.subcontrol.domain.repository.SubscriptionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for binding repository implementations to their interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds the subscription repository implementation.
     */
    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(
        subscriptionRepositoryImpl: SubscriptionRepositoryImpl
    ): SubscriptionRepository

    /**
     * Binds the category repository implementation.
     */
    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        categoryRepositoryImpl: CategoryRepositoryImpl
    ): CategoryRepository

    /**
     * Binds the budget repository implementation.
     */
    @Binds
    @Singleton
    abstract fun bindBudgetRepository(
        budgetRepositoryImpl: BudgetRepositoryImpl
    ): BudgetRepository

    /**
     * Binds the preferences repository implementation.
     */
    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        preferencesRepositoryImpl: PreferencesRepositoryImpl
    ): PreferencesRepository
}
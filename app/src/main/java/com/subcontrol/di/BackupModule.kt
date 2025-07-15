package com.subcontrol.di

import android.content.Context
import com.subcontrol.data.manager.BackupManager
import com.subcontrol.domain.usecase.BackupUseCase
import com.subcontrol.domain.usecase.RestoreUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing backup and restore dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object BackupModule {
    
    /**
     * Provides BackupManager singleton instance.
     */
    @Provides
    @Singleton
    fun provideBackupManager(
        @ApplicationContext context: Context,
        backupUseCase: BackupUseCase,
        restoreUseCase: RestoreUseCase
    ): BackupManager {
        return BackupManager(context, backupUseCase, restoreUseCase)
    }
}
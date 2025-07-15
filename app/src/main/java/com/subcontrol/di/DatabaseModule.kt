package com.subcontrol.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.subcontrol.data.datastore.AppDataSerializer
import com.subcontrol.data.model.proto.AppData
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database and data storage dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val Context.appDataStore: DataStore<AppData> by dataStore(
        fileName = "app_data.pb",
        serializer = AppDataSerializer
    )

    /**
     * Provides the encrypted DataStore for application data.
     *
     * @param context Application context
     * @return DataStore instance for AppData
     */
    @Provides
    @Singleton
    fun provideAppDataStore(@ApplicationContext context: Context): DataStore<AppData> {
        return context.appDataStore
    }
}
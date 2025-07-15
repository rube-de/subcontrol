package com.subcontrol

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for SubControl.
 * 
 * This class serves as the entry point for Hilt dependency injection
 * and application-wide initialization.
 */
@HiltAndroidApp
class SubControlApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize any application-wide components here
        // Note: Avoid heavy operations in onCreate to ensure fast app startup
    }
}
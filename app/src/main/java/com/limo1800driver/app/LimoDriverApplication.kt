package com.limo1800driver.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application class for Limo Driver app
 * Initializes Hilt, logging, and notification system
 */
@HiltAndroidApp
class LimoDriverApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        Timber.tag("LimoDriverApp").d("Application initialized")
    }
}


package com.limo1800driver.app.di

import android.content.Context
import com.limo1800driver.app.data.network.error.ErrorHandler
import com.limo1800driver.app.data.storage.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Application module for dependency injection
 * Provides application-level dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    /**
     * Provide ErrorHandler
     */
    @Provides
    @Singleton
    fun provideErrorHandler(
        @ApplicationContext context: Context
    ): ErrorHandler {
        return ErrorHandler(context)
    }
    
    /**
     * Provide TokenManager
     */
    @Provides
    @Singleton
    fun provideTokenManager(
        @ApplicationContext context: Context
    ): TokenManager {
        return TokenManager(context)
    }
}


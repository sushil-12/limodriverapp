package com.limo1800driver.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Repository module for dependency injection
 * Repositories are provided via @Inject constructor, no need for manual providers
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // Repositories are provided via @Inject constructor, no need for manual providers
}


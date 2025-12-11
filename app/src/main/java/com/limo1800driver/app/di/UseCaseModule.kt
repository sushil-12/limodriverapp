package com.limo1800driver.app.di

import com.limo1800driver.app.domain.usecase.auth.ResendOTPUseCase
import com.limo1800driver.app.domain.usecase.auth.SendVerificationCodeUseCase
import com.limo1800driver.app.domain.usecase.auth.VerifyOTPUseCase
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Use case module for dependency injection
 * Provides use case instances
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    // Use cases are provided via @Inject constructor, no need for manual providers
}


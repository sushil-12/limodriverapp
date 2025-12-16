package com.limo1800driver.app.di

import com.limo1800driver.app.rideinprogress.GoogleDirectionsApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RideInProgressModule {
    /**
     * Directions API is hosted on the same base URL as Places (`https://maps.googleapis.com`),
     * so we reuse the existing `@Named("places")` Retrofit instance.
     */
    @Provides
    @Singleton
    fun provideGoogleDirectionsApi(
        @Named("places") retrofit: Retrofit
    ): GoogleDirectionsApi = retrofit.create(GoogleDirectionsApi::class.java)
}



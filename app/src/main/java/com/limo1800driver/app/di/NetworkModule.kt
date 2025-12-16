package com.limo1800driver.app.di

import android.app.NotificationManager
import android.content.Context
import com.limo1800driver.app.data.model.BooleanIntTypeAdapter
import com.limo1800driver.app.data.model.SafeDoubleTypeAdapter
import com.limo1800driver.app.data.network.NetworkConfig
import com.limo1800driver.app.data.network.interceptors.AuthInterceptor
import com.limo1800driver.app.data.network.interceptors.LoggingInterceptor
import com.limo1800driver.app.data.network.interceptors.RetryInterceptor
import com.limo1800driver.app.data.network.interceptors.LoadingInterceptor
import com.limo1800driver.app.data.network.LoadingStateManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Network module for dependency injection
 * Provides network-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    /**
     * Provide custom Gson instance with BooleanIntTypeAdapter and custom serializers
     * serializeNulls() ensures all fields are included in JSON (matches web format)
     */
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .serializeNulls() // Include null fields in JSON (matches web format)
            .registerTypeAdapter(Boolean::class.java, BooleanIntTypeAdapter())
            .registerTypeAdapter(Boolean::class.javaObjectType, BooleanIntTypeAdapter())
            // Some backend fields come back as "" for numeric values (e.g. amenities_rates.price)
            .registerTypeAdapter(Double::class.java, SafeDoubleTypeAdapter())
            .registerTypeAdapter(Double::class.javaObjectType, SafeDoubleTypeAdapter())
            .create()
    }
    
    /**
     * Provide LoadingStateManager
     */
    @Provides
    @Singleton
    fun provideLoadingStateManager(): LoadingStateManager {
        return LoadingStateManager()
    }
    
    /**
     * Provide main API OkHttp client
     */
    @Provides
    @Singleton
    @Named("main")
    fun provideMainOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: LoggingInterceptor,
        retryInterceptor: RetryInterceptor,
        loadingInterceptor: LoadingInterceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (com.limo1800driver.app.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loadingInterceptor) // Add loading interceptor first to track all requests
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(logging)
            .addInterceptor(retryInterceptor)
            .connectTimeout(NetworkConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(NetworkConfig.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(NetworkConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
    
    /**
     * Provide Google Places API OkHttp client
     */
    @Provides
    @Singleton
    @Named("places")
    fun providePlacesOkHttpClient(
        loggingInterceptor: LoggingInterceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (com.limo1800driver.app.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(logging)
            .connectTimeout(NetworkConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(NetworkConfig.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(NetworkConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
    
    /**
     * Provide main API Retrofit instance
     */
    @Provides
    @Singleton
    @Named("main")
    fun provideMainRetrofit(
        @Named("main") okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(NetworkConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * Provide Google Places API Retrofit instance
     */
    @Provides
    @Singleton
    @Named("places")
    fun providePlacesRetrofit(
        @Named("places") okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(NetworkConfig.GOOGLE_PLACES_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /**
     * Provide Chat API Retrofit instance (chat server lives on a different host than the main API).
     */
    @Provides
    @Singleton
    @Named("chat")
    fun provideChatRetrofit(
        @Named("main") okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(NetworkConfig.CHAT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * Provide DriverAuthApi
     */
    @Provides
    @Singleton
    fun provideDriverAuthApi(
        @Named("main") retrofit: Retrofit
    ): com.limo1800driver.app.data.api.DriverAuthApi {
        return retrofit.create(com.limo1800driver.app.data.api.DriverAuthApi::class.java)
    }
    
    /**
     * Provide DriverRegistrationApi
     */
    @Provides
    @Singleton
    fun provideDriverRegistrationApi(
        @Named("main") retrofit: Retrofit
    ): com.limo1800driver.app.data.api.DriverRegistrationApi {
        return retrofit.create(com.limo1800driver.app.data.api.DriverRegistrationApi::class.java)
    }
    
    /**
     * Provide DriverDashboardApi
     */
    @Provides
    @Singleton
    fun provideDriverDashboardApi(
        @Named("main") retrofit: Retrofit
    ): com.limo1800driver.app.data.api.DriverDashboardApi {
        return retrofit.create(com.limo1800driver.app.data.api.DriverDashboardApi::class.java)
    }

    /**
     * Provide NotificationManager
     */
    @Provides
    @Singleton
    fun provideNotificationManager(
        @ApplicationContext context: Context
    ): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
}


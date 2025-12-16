package com.fantasyfootball.analyzer.di

import com.fantasyfootball.analyzer.data.remote.ESPNApiService
import com.fantasyfootball.analyzer.data.remote.ESPNApiServiceFixed
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module for network dependencies.
 * Provides Retrofit, OkHttp, and API service instances with proper configuration.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    private const val ESPN_BASE_URL = "https://site.api.espn.com/apis/site/v2/"
    // Alternative working base URL for core sports data:
    // private const val ESPN_BASE_URL = "https://sports.core.api.espn.com/v2/"
    private const val CONNECT_TIMEOUT = 30L
    private const val READ_TIMEOUT = 30L
    private const val WRITE_TIMEOUT = 30L
    
    /**
     * Provides HTTP logging interceptor for debugging network requests.
     */
    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }
    
    /**
     * Provides request/response interceptor for adding common headers and handling errors.
     */
    @Provides
    @Singleton
    fun provideRequestInterceptor(): okhttp3.Interceptor {
        return okhttp3.Interceptor { chain ->
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "FantasyFootballAnalyzer/1.0")
            
            val request = requestBuilder.build()
            val response = chain.proceed(request)
            
            // Log response for debugging
            if (!response.isSuccessful) {
                android.util.Log.w(
                    "NetworkModule", 
                    "HTTP ${response.code} for ${request.url}: ${response.message}"
                )
            }
            
            response
        }
    }
    
    /**
     * Provides configured OkHttp client with interceptors and timeouts.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        requestInterceptor: okhttp3.Interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(requestInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
    
    /**
     * Provides Gson instance with custom configuration for ESPN API responses.
     */
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .serializeNulls()
            .create()
    }
    
    /**
     * Provides Retrofit instance configured for ESPN API.
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ESPN_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * Provides ESPN API service instance.
     */
    @Provides
    @Singleton
    fun provideESPNApiService(retrofit: Retrofit): ESPNApiService {
        return retrofit.create(ESPNApiService::class.java)
    }
    
    /**
     * Provides fixed ESPN API service instance with working endpoints.
     */
    @Provides
    @Singleton
    fun provideESPNApiServiceFixed(retrofit: Retrofit): ESPNApiServiceFixed {
        return retrofit.create(ESPNApiServiceFixed::class.java)
    }
}
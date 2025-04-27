package com.soccertips.predictx.di

import android.app.Application
import android.content.Context
import androidx.work.WorkerFactory
import com.soccertips.predictx.data.local.AppDatabase
import com.soccertips.predictx.data.local.dao.FavoriteDao
import com.soccertips.predictx.network.ApiService
import com.soccertips.predictx.network.Constants
import com.soccertips.predictx.network.FixtureDetailsService
import com.soccertips.predictx.network.NetworkUtils
import com.soccertips.predictx.notification.HiltWorkerFactory
import com.soccertips.predictx.notification.NotificationBuilder
import com.soccertips.predictx.repository.FirebaseRepository
import com.soccertips.predictx.repository.PredictionRepository
import com.soccertips.predictx.repository.PreloadRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(application: Application): Context {
        return application.applicationContext
    }

    @Named("defaultBaseUrl")
    @Provides
    fun provideDefaultBaseUrl() = Constants.DEFAULT_BASE_URL
    private const val CACHE_SIZE = 10 * 1024 * 1024 // 10 MB
    private const val CACHE_MAX_AGE = 2 * 60 * 60 // 2 hours

    @Provides
    @Singleton
    fun provideCache(context: Context): Cache {
        val cacheDir = File(context.cacheDir, "http-cache")
        return Cache(cacheDir, CACHE_SIZE.toLong())
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        return logging
    }


    @Provides
    @Singleton
    @Named("cacheInterceptor")
    fun provideCacheInterceptor(context: Context): Interceptor {
        return Interceptor { chain ->
            var request = chain.request()
            request = if (NetworkUtils.isOnline(context)) {
                request.newBuilder().header("Cache-Control", "public, max-age=$CACHE_MAX_AGE")
                    .build()
            } else {
                request.newBuilder().header(
                    "Cache-Control",
                    "public, only-if-cached, max-stale=${7 * 24 * 60 * 60}"
                ).build()
            }
            chain.proceed(request)
        }
    }

    @Provides
    @Singleton
    @Named("defaultOkHttpClient")
    fun provideDefaultOkHttpClient(
        context: Context,
        loggingInterceptor: HttpLoggingInterceptor,
        @Named("cacheInterceptor") cacheInterceptor: Interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .cache(provideCache(context))
            .addInterceptor(loggingInterceptor)
            .addNetworkInterceptor(cacheInterceptor)
            .build()
    }


    // Configuration for AppDatabase and FavoriteItemDao
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    // Configuration for ApiService and PredictionRepository
    @Provides
    @Singleton
    @Named("defaultRetrofit")
    fun provideDefaultRetrofit(
        @Named("defaultOkHttpClient") okHttpClient: OkHttpClient,
        @Named("defaultBaseUrl") baseUrl: String
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideApiService(
        @Named("defaultRetrofit") retrofit: Retrofit
    ): ApiService =
        retrofit.create(ApiService::class.java)

    @Provides
    @Singleton
    fun providePredictionRepository(
        apiService: ApiService,
    ): PredictionRepository =
        PredictionRepository(apiService, lazy {  PreloadRepository.getInstance() })

    @Provides
    @Singleton
    fun providePreloadRepository(
        firebaseRepository: FirebaseRepository
    ): PreloadRepository {
        return PreloadRepository.createInstance(
            firebaseRepository
        )
    }


    // Configuration for FixtureDetailsService and FixtureDetailsRepository
    @Provides
    @Singleton
    @Named("fixtureDetailsHeaderInterceptor")
    fun provideHeaderInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("x-apisports-key", Constants.API_KEY)
                .addHeader("x-apisports-host", Constants.API_HOST)
                .build()
            chain.proceed(request)
        }
    }

    @Provides
    @Singleton
    @Named("fixtureDetailsOkHttpClient")
    fun provideOkHttpClient(
        context: Context,
        loggingInterceptor: HttpLoggingInterceptor,
        @Named("fixtureDetailsHeaderInterceptor") headerInterceptor: Interceptor
    ): OkHttpClient {
        val cacheDir = File(context.cacheDir, "http_cache")
        val cache = Cache(cacheDir, CACHE_SIZE.toLong())

        val customCacheInterceptor = Interceptor { chain ->
            var request = chain.request()

            // Get the request URL to determine which cache time to apply
            val url: HttpUrl = request.url
            val requestBuilder = request.newBuilder()

            // Set different cache times based on the endpoint
            when {
                url.toString().contains("fixtures") -> {
                    // Cache for fixtures endpoints for 1 hour (medium cache)
                    requestBuilder.header(
                        "Cache-Control",
                        "public, max-age=${Constants.CACHE_MAX_AGE_SHORT}"
                    )
                }

                url.toString().contains("predictions") -> {
                    // Cache predictions for 24 hours (long cache)
                    requestBuilder.header(
                        "Cache-Control",
                        "public, max-age=${Constants.CACHE_MAX_AGE_VERY_LONG}"
                    )
                }

                url.toString().contains("standings") -> {
                    // Cache standings for 10 minutes (short cache)
                    requestBuilder.header(
                        "Cache-Control",
                        "public, max-age=${Constants.CACHE_MAX_AGE_LONG}"
                    )
                }

                else -> {
                    // Default: cache for 1 hour
                    requestBuilder.header(
                        "Cache-Control",
                        "public, max-age=${Constants.CACHE_MAX_AGE_LONG}"
                    )
                }
            }
            // Proceed with the request after adding headers
            chain.proceed(requestBuilder.build())
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(headerInterceptor)
            .addInterceptor(customCacheInterceptor) // Add custom cache interceptor
            .cache(cache) // Apply cache to the client
            .build()
    }

    // Provide Retrofit with OkHttpClient and Cache
    @Provides
    @Singleton
    @Named("fixtureDetailsRetrofit")
    fun provideRetrofit(@Named("fixtureDetailsOkHttpClient") client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(client) // Use the OkHttp client with cache
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Provide FixtureDetailsService
    @Provides
    @Singleton
    fun provideFixtureDetailsService(@Named("fixtureDetailsRetrofit") retrofit: Retrofit): FixtureDetailsService {
        return retrofit.create(FixtureDetailsService::class.java)
    }

    @Provides
    @Singleton
    fun provideNotificationBuilder(@ApplicationContext context: Context): NotificationBuilder {
        return NotificationBuilder(context)
    }

    @Provides
    @Singleton
    fun provideWorkerFactory(hiltWorkerFactory: HiltWorkerFactory): WorkerFactory {
        return hiltWorkerFactory
    }

}
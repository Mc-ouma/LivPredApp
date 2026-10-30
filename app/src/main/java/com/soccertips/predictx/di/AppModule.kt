package com.soccertips.predictx.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.work.Configuration
import androidx.work.WorkerFactory
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.soccertips.predictx.data.local.AppDatabase
import com.soccertips.predictx.data.local.dao.FavoriteDao
import com.soccertips.predictx.network.ApiService
import com.soccertips.predictx.network.Constants
import com.soccertips.predictx.network.DnsFailureInterceptor
import com.soccertips.predictx.network.FixtureDetailsService
import com.soccertips.predictx.network.NetworkUtils
import com.soccertips.predictx.network.SocketTaggingInterceptor
import com.soccertips.predictx.notification.BettingSuccessChecker
import com.soccertips.predictx.notification.BettingSuccessScheduler
import com.soccertips.predictx.notification.HiltWorkerFactory
import com.soccertips.predictx.notification.NotificationBuilder
import com.soccertips.predictx.notification.NotificationScheduler
import com.soccertips.predictx.repository.ApiConfigProvider
import com.soccertips.predictx.repository.CategoryRepository
import com.soccertips.predictx.repository.FirebaseRepository
import com.soccertips.predictx.repository.PredictionRepository
import com.soccertips.predictx.repository.PreloadRepository
import com.soccertips.predictx.util.NetworkTaggingInitializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Named
import javax.inject.Singleton
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApiConfigProvider(): ApiConfigProvider {
        return ApiConfigProvider()
    }

    @Provides
    @Singleton
    fun provideContext(application: Application): Context {
        return application.applicationContext
    }

    @Provides
    @Singleton
    fun provideWorkManagerConfiguration(workerFactory: HiltWorkerFactory): Configuration {
        return Configuration.Builder().setWorkerFactory(workerFactory).build()
    }

    @Named("defaultBaseUrl")
    @Provides
    fun provideDefaultBaseUrl() = Constants.API_BASE_URL
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
    fun provideConnectionPool(): ConnectionPool {
        return ConnectionPool(8, 5, java.util.concurrent.TimeUnit.MINUTES)
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(
            if (com.soccertips.predictx.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        )
        return logging
    }

    @Provides
    @Singleton
    @Named("offlineRequestInterceptor")
    fun provideOfflineRequestInterceptor(context: Context): Interceptor {
        return Interceptor { chain ->
            var request = chain.request()
            if (!NetworkUtils.isOnline(context)) {
                request = request.newBuilder()
                    .header(
                        "Cache-Control",
                        "public, only-if-cached, max-stale=${7 * 24 * 60 * 60}"
                    )
                    .build()
            }
            chain.proceed(request)
        }
    }

    @Provides
    @Singleton
    @Named("cacheInterceptor")
    fun provideCacheInterceptor(context: Context): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            if (NetworkUtils.isOnline(context)) {
                response.newBuilder()
                    .removeHeader("Pragma")
                    .header("Cache-Control", "public, max-age=$CACHE_MAX_AGE")
                    .build()
            } else {
                response.newBuilder()
                    .removeHeader("Pragma")
                    .header(
                        "Cache-Control",
                        "public, only-if-cached, max-stale=${7 * 24 * 60 * 60}"
                    )
                    .build()
            }
        }
    }

    @Provides
    @Singleton
    @Named("defaultOkHttpClient")
    fun provideDefaultOkHttpClient(
        context: Context,
        loggingInterceptor: HttpLoggingInterceptor,
        @Named("cacheInterceptor") cacheInterceptor: Interceptor,
        @Named("offlineRequestInterceptor") offlineInterceptor: Interceptor,
        socketTaggingInterceptor: SocketTaggingInterceptor,
        dnsFailureInterceptor: DnsFailureInterceptor,
        fallbackDns: DnsFailureInterceptor.FallbackDns,
        connectionPool: ConnectionPool
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectionPool(connectionPool)
            .dns(fallbackDns) // Use custom DNS with fallback mechanism
            .cache(provideCache(context))
            .addInterceptor(offlineInterceptor)
            .addInterceptor(dnsFailureInterceptor) // Add DNS failure handling
            .addInterceptor(socketTaggingInterceptor)
            .addInterceptor(loggingInterceptor)
            .addNetworkInterceptor(cacheInterceptor)
            .connectTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
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
        @Named("defaultBaseUrl") baseUrl: String,
        gson: Gson
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideApiService(@Named("defaultRetrofit") retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)

    @Provides
    @Singleton
    fun providePredictionRepository(
        apiService: ApiService,
        apiConfigProvider: ApiConfigProvider,
        @ApplicationContext context: Context
    ): PredictionRepository =
        PredictionRepository(
            apiService,
            lazy { PreloadRepository.getInstance() },
            context,
            apiConfigProvider
        )

    @Provides
    @Singleton
    fun providePreloadRepository(
        firebaseRepository: FirebaseRepository,
        categoryRepository: dagger.Lazy<CategoryRepository>,
        networkUtils: com.soccertips.predictx.util.NetworkUtils
    ): PreloadRepository {
        return PreloadRepository.createInstance(firebaseRepository, categoryRepository, networkUtils)
    }

    // Configuration for FixtureDetailsService and FixtureDetailsRepository

    @Provides
    @Singleton
    @Named("fixtureDetailsHeaderInterceptor")
    fun provideHeaderInterceptor(apiConfigProvider: ApiConfigProvider): Interceptor {
        return Interceptor { chain ->
            val apiKey = apiConfigProvider.getApiKey()
            val apiHost = apiConfigProvider.getApiHost()

            Timber.d("Using API Key: $apiKey, Host: $apiHost")

            val request =
                chain.request()
                    .newBuilder()
                    .addHeader("x-apisports-key", apiKey)
                    .addHeader("x-apisports-host", apiHost)
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
        @Named("fixtureDetailsHeaderInterceptor") headerInterceptor: Interceptor,
        @Named("offlineRequestInterceptor") offlineInterceptor: Interceptor,
        socketTaggingInterceptor: SocketTaggingInterceptor,
        dnsFailureInterceptor: DnsFailureInterceptor,
        fallbackDns: DnsFailureInterceptor.FallbackDns,
        connectionPool: ConnectionPool
    ): OkHttpClient {
        val cacheDir = File(context.cacheDir, "http_cache")
        val cache = Cache(cacheDir, CACHE_SIZE.toLong())

        val customCacheInterceptor = Interceptor { chain ->
            val request = chain.request()
            val url: HttpUrl = request.url
            val response = chain.proceed(request)
            val responseBuilder = response.newBuilder().removeHeader("Pragma")

            // Set different cache times on the response based on the endpoint
            when {
                url.toString().contains("fixtures") -> {
                    responseBuilder.header(
                        "Cache-Control",
                        "public, max-age=${Constants.CACHE_MAX_AGE_SHORT}"
                    )
                }

                url.toString().contains("predictions") -> {
                    responseBuilder.header(
                        "Cache-Control",
                        "public, max-age=${Constants.CACHE_MAX_AGE_VERY_LONG}"
                    )
                }

                url.toString().contains("standings") -> {
                    responseBuilder.header(
                        "Cache-Control",
                        "public, max-age=${Constants.CACHE_MAX_AGE_LONG}"
                    )
                }

                else -> {
                    responseBuilder.header(
                        "Cache-Control",
                        "public, max-age=${Constants.CACHE_MAX_AGE_LONG}"
                    )
                }
            }
            responseBuilder.build()
        }

        return OkHttpClient.Builder()
            .connectionPool(connectionPool)
            .dns(fallbackDns) // Use custom DNS with fallback mechanism
            .cache(cache)
            .addInterceptor(offlineInterceptor)
            .addInterceptor(dnsFailureInterceptor) // Add DNS failure handling
            .addInterceptor(socketTaggingInterceptor)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(headerInterceptor)
            .addNetworkInterceptor(customCacheInterceptor)
            .connectTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    // Provide Retrofit with OkHttpClient and Cache
    @Provides
    @Singleton
    @Named("fixtureDetailsRetrofit")
    fun provideRetrofit(
        @Named("fixtureDetailsOkHttpClient") client: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.API_BASE_URL_VALUE)
            .client(client) // Use the OkHttp client with cache
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    // Provide FixtureDetailsService
    @Provides
    @Singleton
    fun provideFixtureDetailsService(
        @Named("fixtureDetailsRetrofit") retrofit: Retrofit
    ): FixtureDetailsService {
        return retrofit.create(FixtureDetailsService::class.java)
    }

    @Provides
    @Singleton
    fun provideNotificationBuilder(@ApplicationContext context: Context): NotificationBuilder {
        return NotificationBuilder(context)
    }

    @Provides
    @Singleton
    fun provideNotificationScheduler(@ApplicationContext context: Context): NotificationScheduler {
        return NotificationScheduler(context)
    }

    @Provides
    @Singleton
    fun provideBettingSuccessChecker(
        @ApplicationContext context: Context,
        predictionRepository: PredictionRepository,
        notificationBuilder: NotificationBuilder,
        firebaseRepository: FirebaseRepository
    ): BettingSuccessChecker {
        return BettingSuccessChecker(
            context,
            predictionRepository,
            notificationBuilder,
            firebaseRepository
        )
    }

    @Provides
    @Singleton
    fun provideBettingSuccessScheduler(
        @ApplicationContext context: Context,
        bettingSuccessChecker: BettingSuccessChecker
    ): BettingSuccessScheduler {
        return BettingSuccessScheduler(context, bettingSuccessChecker)
    }

    @Provides
    @Singleton
    fun provideRealTimeResultMonitor(
        @ApplicationContext context: Context,
        firebaseRepository: FirebaseRepository,
        predictionRepository: PredictionRepository,
        bettingSuccessChecker: BettingSuccessChecker
    ): com.soccertips.predictx.notification.RealTimeResultMonitor {
        return com.soccertips.predictx.notification.RealTimeResultMonitor(
            context,
            firebaseRepository,
            predictionRepository,
            bettingSuccessChecker
        )
    }

    @Provides
    @Singleton
    fun provideWorkerFactory(hiltWorkerFactory: HiltWorkerFactory): WorkerFactory {
        return hiltWorkerFactory
    }

    @Provides
    @Singleton
    fun provideSocketTaggingInterceptor(): SocketTaggingInterceptor {
        return SocketTaggingInterceptor()
    }

    @Provides
    @Singleton
    fun provideNetworkTaggingInitializer(): NetworkTaggingInitializer {
        return NetworkTaggingInitializer()
    }

    @Provides
    @Singleton
    fun provideDevicePerformanceManager(): com.soccertips.predictx.util.DevicePerformanceManager {
        return com.soccertips.predictx.util.DevicePerformanceManager()
    }

    @Provides
    @Singleton
    fun provideDnsFailureInterceptor(context: Context): DnsFailureInterceptor {
        return DnsFailureInterceptor(context)
    }

    @Provides
    @Singleton
    fun provideFallbackDns(): DnsFailureInterceptor.FallbackDns {
        return DnsFailureInterceptor.FallbackDns()
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideReviewManager(@ApplicationContext context: Context): ReviewManager {
        return ReviewManagerFactory.create(context)
    }

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics {
        return FirebaseAnalytics.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideFirebaseRemoteConfig(): FirebaseRemoteConfig {
        return FirebaseRemoteConfig.getInstance()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return com.google.gson.GsonBuilder()
            .registerTypeAdapter(
                com.soccertips.predictx.data.model.RootResponse::class.java,
                com.soccertips.predictx.data.model.RootResponseDeserializer()
            )
            .create()
    }

    /**
     * Provides a custom Coil ImageLoader with optimized caching configuration.
     * Memory cache: 25% of available app memory
     * Disk cache: 100 MB
     * This improves image loading performance and reduces network calls.
     */
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        @Named("defaultOkHttpClient") okHttpClient: OkHttpClient
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // Use 25% of available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100 * 1024 * 1024) // 100 MB
                    .build()
            }
            .okHttpClient(okHttpClient) // Reuse our configured OkHttp client
            .respectCacheHeaders(false) // Use our own cache policy
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .allowRgb565(true) // Optimize bitmap memory footprint
            .apply {
                // Enable debug logging in debug builds
                if (com.soccertips.predictx.BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }
}

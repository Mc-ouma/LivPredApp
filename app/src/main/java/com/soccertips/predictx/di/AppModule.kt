package com.soccertips.predictx.di

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkerFactory
import com.soccertips.predictx.data.local.AppDatabase
import com.soccertips.predictx.data.local.dao.FavoriteDao
import com.soccertips.predictx.network.ApiService
import com.soccertips.predictx.network.Constants
import com.soccertips.predictx.network.DnsFailureInterceptor
import com.soccertips.predictx.network.FixtureDetailsService
import com.soccertips.predictx.network.NetworkUtils
import com.soccertips.predictx.network.SocketTaggingInterceptor
import com.soccertips.predictx.notification.HiltWorkerFactory
import com.soccertips.predictx.notification.NotificationBuilder
import com.soccertips.predictx.notification.NotificationScheduler
import com.soccertips.predictx.repository.ApiConfigProvider
import com.soccertips.predictx.repository.FirebaseRepository
import com.soccertips.predictx.repository.PredictionRepository
import com.soccertips.predictx.repository.PreloadRepository
import com.soccertips.predictx.util.NetworkTaggingInitializer
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
import timber.log.Timber
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Named
import javax.inject.Singleton
import kotlin.random.Random
import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar

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
    fun provideWorkManagerConfiguration(
        workerFactory: HiltWorkerFactory
    ): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
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
        @Named("cacheInterceptor") cacheInterceptor: Interceptor,
        socketTaggingInterceptor: SocketTaggingInterceptor,
        dnsFailureInterceptor: DnsFailureInterceptor,
        fallbackDns: DnsFailureInterceptor.FallbackDns
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .dns(fallbackDns) // Use custom DNS with fallback mechanism
            .cache(provideCache(context))
            .addInterceptor(dnsFailureInterceptor) // Add DNS failure handling
            .addInterceptor(socketTaggingInterceptor)
            .addInterceptor(loggingInterceptor)
            .addNetworkInterceptor(cacheInterceptor)
            .addInterceptor(provideBrowserEmulationInterceptor(context))
            .cookieJar(AntiBot403CookieJar()) // Add persistent cookie jar
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    /**
     * Creates an advanced interceptor that makes API requests look like they're coming from a real browser
     * with techniques to bypass aggressive bot detection
     */
    @Provides
    @Singleton
    fun provideBrowserEmulationInterceptor(context: Context): Interceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()

        // Common Chrome on Android user agent
        val androidUserAgent = "Mozilla/5.0 (Linux; Android 13; SM-S908B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36"

        // User agents for desktop browsers
        val desktopUserAgents = listOf(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Safari/605.1.15",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"
        )

        // Choose between mobile and desktop user agent based on domain
        val userAgent = if (url.contains("dailypredictz.com")) {
            // For this specific domain, always use a mobile user agent
            androidUserAgent
        } else {
            // For other domains, randomize between desktop user agents
            desktopUserAgents[Random.nextInt(desktopUserAgents.size)]
        }

        // Create referrer URL - typically use the domain's homepage
        val domain = try {
            URI(url).host
        } catch (e: Exception) {
            null
        }

        val referrer = if (domain != null) {
            // If the request is going to dailypredictz.com, use the website itself as referrer
            if (domain.contains("dailypredictz.com")) {
                "https://dailypredictz.com/"
            } else {
                "https://www.google.com/search?q=${URLEncoder.encode(domain, StandardCharsets.UTF_8.toString())}"
            }
        } else {
            "https://www.google.com/"
        }

        // Generate a consistent browser fingerprint for the session
        val sessionId = DeviceSessionManager.getSessionId(context)

        // Build the request with browser-like headers
        val newRequest = originalRequest.newBuilder()
            .header("User-Agent", userAgent)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Accept-Encoding", "gzip, deflate, br")
            .header("Referer", referrer)
            .apply { if (domain != null) header("Origin", "https://$domain") }
            .header("Connection", "keep-alive")
            .header("DNT", "1")
            .header("Upgrade-Insecure-Requests", "1")
            .header("Sec-Ch-Ua", "\"Google Chrome\";v=\"115\", \"Chromium\";v=\"115\"")
            .header("Sec-Ch-Ua-Mobile", if (userAgent == androidUserAgent) "?1" else "?0")
            .header("Sec-Ch-Ua-Platform", if (userAgent == androidUserAgent) "\"Android\"" else "\"Windows\"")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "same-origin")
            .header("Sec-Fetch-User", "?1")
            .header("X-Requested-With", "com.soccertips.predictx")
            .header("Cache-Control", "max-age=0")
            .header("X-Client-Session-Id", sessionId)
            .removeHeader("Host") // Let OkHttp set this automatically
            .build()

        // Add randomized delay to simulate human behavior (between 500ms and 3s)
        // For the specific domain that's giving 403s, always add a delay
        if (url.contains("dailypredictz.com") || Random.nextInt(5) == 0) {
            val delayMillis = Random.nextLong(500, 3000)
            try {
                Thread.sleep(delayMillis)
            } catch (e: InterruptedException) {
                // Ignore
            }
        }

        val response = chain.proceed(newRequest)

        // Return the response
        response
    }

    /**
     * Cookie jar implementation that persists cookies between requests
     * to maintain session state, which helps bypass bot detection
     */
    class AntiBot403CookieJar : CookieJar {
        private val cookieStore = ConcurrentHashMap<String, List<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies

            // Also sync with WebView CookieManager to maintain consistent state
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)

            for (cookie in cookies) {
                val cookieString = "${cookie.name}=${cookie.value}; domain=${cookie.domain}"
                cookieManager.setCookie(url.toString(), cookieString)
            }

            cookieManager.flush()
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val cookies = cookieStore[url.host] ?: emptyList()

            // Also check WebView cookies to maintain consistent state
            val cookieManager = CookieManager.getInstance()
            val cookieString = cookieManager.getCookie(url.toString())

            if (!cookieString.isNullOrBlank()) {
                // Process WebView cookies if needed
            }

            return cookies
        }
    }

    /**
     * Manages device session IDs to maintain consistent browser fingerprints
     */
    object DeviceSessionManager {
        private const val PREFS_NAME = "device_session_prefs"
        private const val KEY_SESSION_ID = "browser_session_id"

        fun getSessionId(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            var sessionId = prefs.getString(KEY_SESSION_ID, null)

            if (sessionId == null) {
                sessionId = UUID.randomUUID().toString()
                prefs.edit().putString(KEY_SESSION_ID, sessionId).apply()
            }

            return sessionId
        }
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
        apiConfigProvider: ApiConfigProvider,
        @ApplicationContext context: Context
    ): PredictionRepository =
        PredictionRepository(apiService, lazy { PreloadRepository.getInstance() }, context, apiConfigProvider)

    @Provides
    @Singleton
    fun providePreloadRepository(
        firebaseRepository: FirebaseRepository,
        networkUtils: com.soccertips.predictx.util.NetworkUtils
    ): PreloadRepository {
        return PreloadRepository.createInstance(
            firebaseRepository,
            networkUtils
        )
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

            val request = chain.request().newBuilder()
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
        socketTaggingInterceptor: SocketTaggingInterceptor,
        dnsFailureInterceptor: DnsFailureInterceptor,
        fallbackDns: DnsFailureInterceptor.FallbackDns
    ): OkHttpClient {
        val cacheDir = File(context.cacheDir, "http_cache")
        val cache = Cache(cacheDir, CACHE_SIZE.toLong())

        val customCacheInterceptor = Interceptor { chain ->
            var request = chain.request()
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
            .dns(fallbackDns) // Use custom DNS with fallback mechanism
            .addInterceptor(dnsFailureInterceptor) // Add DNS failure handling
            .addInterceptor(socketTaggingInterceptor)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(headerInterceptor)
            .addInterceptor(customCacheInterceptor)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .cache(cache)
            .build()
    }

    // Provide Retrofit with OkHttpClient and Cache
    @Provides
    @Singleton
    @Named("fixtureDetailsRetrofit")
    fun provideRetrofit(@Named("fixtureDetailsOkHttpClient") client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.API_BASE_URL_VALUE)
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
    fun provideNotificationScheduler(@ApplicationContext context: Context): NotificationScheduler {
        return NotificationScheduler(context)
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
    fun provideDnsFailureInterceptor(context: Context): DnsFailureInterceptor {
        return DnsFailureInterceptor(context)
    }

    @Provides
    @Singleton
    fun provideFallbackDns(): DnsFailureInterceptor.FallbackDns {
        return DnsFailureInterceptor.FallbackDns()
    }



}

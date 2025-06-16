package com.flashsphere.rainwaveplayer.internal.di

import android.app.Application
import android.content.Context
import android.os.Build
import com.flashsphere.rainwaveplayer.BuildConfig
import com.flashsphere.rainwaveplayer.coroutine.coroutineExceptionHandler
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.flow.ConnectivityObserver
import com.flashsphere.rainwaveplayer.model.stationInfo.InfoErrorResponse
import com.flashsphere.rainwaveplayer.okhttp.AuthenticatedUserInterceptor
import com.flashsphere.rainwaveplayer.okhttp.CustomHttpLoggingInterceptor
import com.flashsphere.rainwaveplayer.okhttp.RequestHeadersInterceptor
import com.flashsphere.rainwaveplayer.okhttp.TrustedCertificateStore
import com.flashsphere.rainwaveplayer.repository.RainwaveService
import com.flashsphere.rainwaveplayer.ui.UiEventDelegate
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import com.flashsphere.rainwaveplayer.util.JobUtils.cancel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.tls.HandshakeCertificates
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import timber.log.Timber
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

    private var connectionPoolJob: Job? = null

    @Provides
    @Singleton
    fun provideApplicationContext(application: Application): Context {
        return application
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val builder = Retrofit.Builder()
            .baseUrl(RainwaveService.API_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideJson() = Json {
        coerceInputValues = true
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    fun provideRainwaveService(retrofit: Retrofit): RainwaveService {
        return retrofit.create(RainwaveService::class.java)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(connectionPool: ConnectionPool,
                            authenticatedUserInterceptor: AuthenticatedUserInterceptor,
                            requestHeadersInterceptor: RequestHeadersInterceptor,
                            trustedCertificateStore: TrustedCertificateStore): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(authenticatedUserInterceptor)
            .addInterceptor(requestHeadersInterceptor)
            .connectionPool(connectionPool)
            .apply {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    val clientCertificates = handshakeCertificates(trustedCertificateStore)

                    sslSocketFactory(
                        clientCertificates.sslSocketFactory(),
                        clientCertificates.trustManager
                    )
                }
                if (BuildConfig.DEBUG) {
                    addInterceptor(CustomHttpLoggingInterceptor())
                }
            }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideConnectionPool(connectivityObserver: ConnectivityObserver,
                              coroutineDispatchers: CoroutineDispatchers): ConnectionPool {
        Timber.d("Creating connection pool")
        val connectionPool = ConnectionPool()

        // if there's no connectivity,
        // existing connections in the pool are still reused
        // and causes SocketTimeoutException,
        // so we evict the connection pool when there's no connectivity
        cancel(connectionPoolJob)
        connectionPoolJob = connectivityObserver.connectivityFlow
            .filter { connected -> !connected }
            .onEach {
                Timber.d("Evicting connection pool")
                connectionPool.evictAll()
            }
            .launchWithDefaults(coroutineDispatchers.scope, "OkHttp Connection Pool")

        return connectionPool
    }

    @Provides
    @Singleton
    fun provideTrustedCertificateStore(context: Context): TrustedCertificateStore {
        return TrustedCertificateStore(context)
    }

    private fun handshakeCertificates(trustedCertificateStore: TrustedCertificateStore): HandshakeCertificates {
        val builder = HandshakeCertificates.Builder()

        trustedCertificateStore.rootCertificates.asSequence()
            .map { it.certificate }
            .forEach { cert ->
            if (cert is X509Certificate) {
                builder.addTrustedCertificate(cert)
            }
        }

        return builder
            .addPlatformTrustedCertificates()
            .build()
    }

    @Provides
    @Singleton
    @Named("info_error_response_converter")
    fun provideInfoErrorResponseConverter(retrofit: Retrofit): Converter<ResponseBody, InfoErrorResponse> {
        return retrofit.responseBodyConverter(InfoErrorResponse::class.java, emptyArray())
    }

    @Provides
    @Singleton
    fun provideAppCoroutineDispatchers(): CoroutineDispatchers {
        return CoroutineDispatchers(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + coroutineExceptionHandler),
            compute = Dispatchers.Default,
            network = Dispatchers.IO,
            main = Dispatchers.Main.immediate,
        )
    }

    @Provides
    @Singleton
    fun provideUiEventDelegate(dispatchers: CoroutineDispatchers): UiEventDelegate {
        return UiEventDelegate(dispatchers.scope)
    }
}

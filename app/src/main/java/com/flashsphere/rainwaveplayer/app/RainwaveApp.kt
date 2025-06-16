package com.flashsphere.rainwaveplayer.app

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.multidex.MultiDex
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.disk.DiskCache
import coil3.network.cachecontrol.CacheControlCacheStrategy
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.svg.SvgDecoder
import com.flashsphere.rainwaveplayer.BuildConfig
import com.flashsphere.rainwaveplayer.flow.MediaPlayerStateObserver
import com.flashsphere.rainwaveplayer.media.NotificationChannelHelper
import com.flashsphere.rainwaveplayer.repository.StationRepository
import com.flashsphere.rainwaveplayer.util.Analytics
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import com.flashsphere.rainwaveplayer.util.CrashlyticsTree
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.DEBUG_PROPERTY_NAME
import kotlinx.coroutines.DEBUG_PROPERTY_VALUE_ON
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import timber.log.Timber
import timber.log.Timber.DebugTree
import javax.inject.Inject

@HiltAndroidApp
class RainwaveApp : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    @Inject
    lateinit var stationRepository: StationRepository

    @Inject
    lateinit var coroutineDispatchers: CoroutineDispatchers

    @Inject
    lateinit var analytics: Analytics

    @Inject
    lateinit var okHttpClient: OkHttpClient

    @Inject
    lateinit var mediaPlayerStateObserver: MediaPlayerStateObserver

    init {
        if (BuildConfig.DEBUG) {
            System.setProperty(DEBUG_PROPERTY_NAME, DEBUG_PROPERTY_VALUE_ON)
            Timber.plant(DebugTree())
        }
    }

    override fun onCreate() {
        if (ProcessPhoenix.isPhoenixProcess(this)) {
            return
        }

        super.onCreate()

        Timber.plant(CrashlyticsTree(this, coroutineDispatchers, dataStore))

        NotificationChannelHelper(this).setupNotificationChannels()
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_BACKGROUND) {
            stationRepository.cleanupCache()
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(OkHttpNetworkFetcherFactory(
                    callFactory = { okHttpClient },
                    cacheStrategy = { CacheControlCacheStrategy() })
                )
                add(SvgDecoder.Factory())
            }
            .diskCache(DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache").toOkioPath())
                .maxSizeBytes(52428800).build())
            .build()
    }
}

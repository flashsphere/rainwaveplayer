package com.flashsphere.rainwaveplayer.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.CATEGORY_TRANSPORT
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.cast.CastContextHolder
import com.flashsphere.rainwaveplayer.cast.isPlaying
import com.flashsphere.rainwaveplayer.coroutine.coroutineExceptionHandler
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.flow.MediaPlayerStateObserver
import com.flashsphere.rainwaveplayer.media.MediaNotificationHelper.Companion.FOREGROUND_SERVICE_TYPE
import com.flashsphere.rainwaveplayer.media.MediaNotificationHelper.Companion.NOTIFICATION_ID
import com.flashsphere.rainwaveplayer.model.MediaPlayerStatus.State.Buffering
import com.flashsphere.rainwaveplayer.model.MediaPlayerStatus.State.Playing
import com.flashsphere.rainwaveplayer.playback.AudioFocusManager
import com.flashsphere.rainwaveplayer.playback.PlaybackManager
import com.flashsphere.rainwaveplayer.util.Analytics
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import com.flashsphere.rainwaveplayer.util.JobUtils.cancel
import com.flashsphere.rainwaveplayer.view.activity.StartPlaybackActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.N)
@AndroidEntryPoint
class MediaTileService : TileService() {
    @Inject
    lateinit var mediaPlayerStateObserver: MediaPlayerStateObserver
    @Inject
    lateinit var analytics: Analytics
    @Inject
    lateinit var castContextHolder: CastContextHolder
    @Inject
    lateinit var playbackManager: PlaybackManager
    @Inject
    lateinit var coroutineDispatchers: CoroutineDispatchers

    private lateinit var scope: CoroutineScope
    private var job: Job? = null

    override fun onCreate() {
        super.onCreate()
        scope = CoroutineScope(coroutineDispatchers.main + SupervisorJob() + coroutineExceptionHandler)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onClick() {
        Timber.d("onClick")
        super.onClick()
        analytics.logEvent(Analytics.EVENT_USE_TILE)

        val castSession = castContextHolder.getCastSession()
        if (castSession != null) {
            if (!castSession.isPlaying()) {
                Timber.i("Media tile play")
                scope.launchWithDefaults("Media tile Cast") { playbackManager.playOnCast() }
            } else {
                Timber.i("Media tile stop")
                castContextHolder.endCastSession()
            }
        } else {
            if (mediaPlayerStateObserver.currentState.isStopped()) {
                Timber.i("Media tile play")
                runCatching {
                    // Earlier versions of Android 15 requires the app to be started or in background before allowing audio focus
                    // if we can't get audio focus, we use the playback activity to start the playback
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.VANILLA_ICE_CREAM && !testAudioFocus()) {
                        startPlaybackActivity()
                    } else {
                        // Android 14 specifically don't allow starting foreground service,
                        // if the app is not already started or in background
                        MediaService.playOrThrow(this)
                    }
                }.onFailure {
                    startPlaybackActivity()
                }
            } else {
                Timber.i("Media tile stop")
                MediaService.stop(this)
            }
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    @Suppress("DEPRECATION")
    private fun startPlaybackActivity() {
        Timber.d("Using playback activity to start playback")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(StartPlaybackActivity.getPendingIntent(this))
        } else {
            startActivityAndCollapse(StartPlaybackActivity.getCallingIntent(this))
        }
    }

    private fun testAudioFocus(): Boolean {
        // foreground service is required to request audio focus
        val notificationManager = NotificationManagerCompat.from(this)
        val notification = createNotification()

        runCatching {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE)
        }

        // requesting audio focus
        val audioFocusManager = AudioFocusManager(this) {}
        return try {
            audioFocusManager.requestAudioFocus().also { Timber.d("Test audio focus = %s", it) }
        } finally {
            audioFocusManager.abandonAudioFocus()
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            notificationManager.cancel(NOTIFICATION_ID)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, getString(R.string.channel_name_playback))
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_rainwave_24dp)
            .setCategory(CATEGORY_TRANSPORT)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentTitle(getString(R.string.connecting))
            .setTicker(getString(R.string.connecting))
            .build()
    }

    override fun onTileAdded() {
        Timber.d("onTileAdded")
        super.onTileAdded()
        analytics.logEvent(Analytics.EVENT_ADD_TILE)
    }

    override fun onTileRemoved() {
        Timber.d("onTileRemoved")
        super.onTileRemoved()
        analytics.logEvent(Analytics.EVENT_REMOVE_TILE)
    }

    override fun onStartListening() {
        Timber.d("onStartListening")
        super.onStartListening()

        cancel(job)
        job = mediaPlayerStateObserver.flow
            .onEach { currentState ->
                qsTile?.let { tile ->
                    when (currentState.state) {
                        Buffering, Playing -> {
                            tile.state = Tile.STATE_ACTIVE
                        }
                        else -> {
                            tile.state = Tile.STATE_INACTIVE
                        }
                    }
                    tile.updateTile()
                }
            }
            .launchWithDefaults(scope, "Media Tile Listening")
    }

    override fun onStopListening() {
        Timber.d("onStopListening")
        cancel(job)
        super.onStopListening()
    }

    companion object {
        @RequiresApi(Build.VERSION_CODES.N)
        fun requestListeningState(context: Context) {
            runCatching {
                Timber.d("Request listening state for tile")
                requestListeningState(context, ComponentName(context, MediaTileService::class.java))
            }.onFailure {
                Timber.e(it, "Unable to request listening state for tile!")
            }
        }
    }
}

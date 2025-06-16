package com.flashsphere.rainwaveplayer.media

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.KEYCODE_MEDIA_NEXT
import android.view.KeyEvent.KEYCODE_MEDIA_PAUSE
import android.view.KeyEvent.KEYCODE_MEDIA_PLAY
import android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
import android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
import android.view.KeyEvent.KEYCODE_MEDIA_STOP
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.flow.MediaPlayerStateObserver
import com.flashsphere.rainwaveplayer.service.MediaService
import com.flashsphere.rainwaveplayer.util.PendingIntentUtils
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MediaButtonReceiver : BroadcastReceiver() {
    @Inject
    lateinit var mediaPlayerStateObserver: MediaPlayerStateObserver

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_MEDIA_BUTTON != intent.action ||
            !intent.hasExtra(Intent.EXTRA_KEY_EVENT)) {
            Timber.d("Ignore unsupported intent: %s", intent)
            return
        }

        val ke = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java) ?: return
        if (ke.action != ACTION_DOWN) {
            return
        }

        val source = intent.getStringExtra("source") ?: "system"
        Timber.i("Media button: %d, action: %d, source: %s", ke.keyCode, ke.action, source)

        intent.component = ComponentName(context, MediaService::class.java)

        when (ke.keyCode) {
            KEYCODE_MEDIA_PAUSE -> {
                if (mediaPlayerStateObserver.currentState.isStopped()) {
                    Timber.i("Not pausing as media is not playing")
                    return
                }
                startService(context, intent)
            }
            KEYCODE_MEDIA_PLAY,
            KEYCODE_MEDIA_NEXT,
            KEYCODE_MEDIA_PREVIOUS,
            KEYCODE_MEDIA_PLAY_PAUSE -> {
                startForegroundService(context, intent)
            }
            KEYCODE_MEDIA_STOP -> {
                MediaService.stop(context)
            }
        }
    }

    private fun startForegroundService(context: Context, intent: Intent) {
        runCatching {
            ContextCompat.startForegroundService(context, intent)
        }.onFailure {
            Timber.e(it, "Can't start foreground service")
        }
    }

    private fun startService(context: Context, intent: Intent) {
        runCatching {
            context.startService(intent)
        }.onFailure {
            Timber.e(it, "Can't start service")
        }
    }

    companion object {
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, MediaButtonReceiver::class.java)
        }

        fun getPendingIntent(context: Context): PendingIntent {
            val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                component = getComponentName(context)
            }
            return PendingIntent.getBroadcast(context, R.id.action_media_button, mediaButtonIntent,
                PendingIntentUtils.getPendingIntentFlags())
        }
    }
}

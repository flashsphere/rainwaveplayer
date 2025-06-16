package com.flashsphere.rainwaveplayer.util

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_NO_CREATE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.playback.PlaybackManager
import com.flashsphere.rainwaveplayer.util.PendingIntentUtils.getPendingIntentFlags
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.SLEEP_TIMER_MILLIS
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SleepTimerBroadcastReceiver : BroadcastReceiver() {
    @Inject
    lateinit var dataStore: DataStore<Preferences>
    @Inject
    lateinit var playbackManager: PlaybackManager

    override fun onReceive(context: Context, intent: Intent?) {
        Timber.d("sleep timer broadcast receiver")
        dataStore.removeBlocking(SLEEP_TIMER_MILLIS)
        getExistingBroadcast(context)?.cancel()
        playbackManager.stop()
    }

    companion object {
        fun createBroadcastIntent(context: Context): PendingIntent {
            val intent = Intent(context, SleepTimerBroadcastReceiver::class.java)
            return PendingIntent.getBroadcast(context, R.id.sleep_timer_request_code, intent,
                getPendingIntentFlags())
        }
        fun getExistingBroadcast(context: Context): PendingIntent? {
            val intent = Intent(context, SleepTimerBroadcastReceiver::class.java)
            return PendingIntent.getBroadcast(context, R.id.sleep_timer_request_code, intent,
                getPendingIntentFlags(FLAG_NO_CREATE))
        }
    }
}

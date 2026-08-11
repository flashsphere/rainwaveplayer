package com.flashsphere.rainwaveplayer.view.activity

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Toast
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.service.MediaService
import com.flashsphere.rainwaveplayer.util.PendingIntentUtils
import com.flashsphere.rainwaveplayer.view.helper.SleepTimerDelegate
import dagger.hilt.android.AndroidEntryPoint
import java.time.Duration
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class StartPlaybackActivity : BaseActivity() {

    @Inject
    lateinit var sleepTimerDelegate: SleepTimerDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startPlayback()
        createSleepTimer()
        finish()
    }

    private fun startPlayback() {
        val stationId = extractStationIdFromPath() ?: extractStationIdFromExtras()
        if (stationId == null) {
            MediaService.play(this)
        } else {
            MediaService.playFromMediaId(this, stationId)
        }
    }

    private fun extractStationIdFromPath(): String? {
        val pathSegments = intent.data?.pathSegments
        if (pathSegments == null || pathSegments.size < 2) {
            return null
        }
        return pathSegments[1].takeIf { (it.toIntOrNull() ?: 0) > 0 }
    }

    private fun extractStationIdFromExtras(): String? {
        return intent.getStringExtra(INTENT_EXTRA_PARAM_STATION)?.takeIf { (it.toIntOrNull() ?: 0) > 0 }
    }

    private fun createSleepTimer() {
        val duration = (extractSleepTimerDurationFromPath() ?: extractSleepTimerDurationFromExtras()) ?: return

        sleepTimerDelegate.removeSleepTimer()
        val endTimeMillis = sleepTimerDelegate.createSleepTimer(duration)

        val formattedSleepTimer = Date(endTimeMillis).let {
            getString(R.string.sleep_timer_stop,
                DateFormat.getDateFormat(this).format(it),
                DateFormat.getTimeFormat(this).format(it))
        }
        Toast.makeText(this, formattedSleepTimer, Toast.LENGTH_LONG).show()
    }

    private fun extractSleepTimerDurationFromPath(): Duration? {
        return intent.data?.getQueryParameter(INTENT_EXTRA_PARAM_SLEEP_TIMER_DURATION)?.let { parseDuration(it) }
    }

    private fun extractSleepTimerDurationFromExtras(): Duration? {
        return intent.getStringExtra(INTENT_EXTRA_PARAM_SLEEP_TIMER_DURATION)?.let { parseDuration(it) }
    }

    private fun parseDuration(duration: String): Duration? {
        return runCatching { Duration.parse("PT${duration}") }.getOrNull()
    }

    companion object {
        private const val INTENT_EXTRA_PARAM_STATION = "station_id"
        private const val INTENT_EXTRA_PARAM_SLEEP_TIMER_DURATION = "sleep_timer"

        fun getCallingIntent(context: Context): Intent {
            return Intent(context, StartPlaybackActivity::class.java)
        }

        fun getPendingIntent(context: Context): PendingIntent {
            val intent = getCallingIntent(context)
            return PendingIntent.getActivity(context, R.id.start_playback_activity_request_code, intent, PendingIntentUtils.getPendingIntentFlags())
        }
    }
}

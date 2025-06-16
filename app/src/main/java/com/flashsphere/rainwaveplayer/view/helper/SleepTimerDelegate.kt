package com.flashsphere.rainwaveplayer.view.helper

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flashsphere.rainwaveplayer.util.Analytics
import com.flashsphere.rainwaveplayer.util.Analytics.Companion.EVENT_REMOVE_SLEEP_TIMER
import com.flashsphere.rainwaveplayer.util.Analytics.Companion.EVENT_SET_SLEEP_TIMER
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.SLEEP_TIMER_MILLIS
import com.flashsphere.rainwaveplayer.util.SleepTimerBroadcastReceiver.Companion.createBroadcastIntent
import com.flashsphere.rainwaveplayer.util.SleepTimerBroadcastReceiver.Companion.getExistingBroadcast
import com.flashsphere.rainwaveplayer.util.getBlocking
import com.flashsphere.rainwaveplayer.util.removeBlocking
import com.flashsphere.rainwaveplayer.util.updateBlocking
import timber.log.Timber
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.minutes

class SleepTimerDelegate(
    private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val analytics: Analytics,
) {
    val showState = mutableStateOf(false)
    private val alarmManager = context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun getExistingSleepTimer(): Long? {
        val sleepTimerMillis = dataStore.getBlocking(SLEEP_TIMER_MILLIS)
        val pendingIntentBroadcast = getExistingBroadcast(context)
        Timber.d("pending broadcast = %s", pendingIntentBroadcast)
        return if (sleepTimerMillis == SLEEP_TIMER_MILLIS.defaultValue || pendingIntentBroadcast == null) {
            removeSleepTimer()
            null
        } else {
            sleepTimerMillis
        }
    }

    fun createSleepTimer(hour: Int, minute: Int): Long {
        val endTime = ZonedDateTime.now()
            .withHour(hour)
            .withMinute(minute)
            .truncatedTo(ChronoUnit.MINUTES)
            .run {
                return@run if (this.isBefore(ZonedDateTime.now())) {
                    this.plusDays(1)
                } else {
                    this
                }
            }
        val endTimeInMillis = endTime.toInstant().toEpochMilli()
        Timber.d("Creating sleep timer alarm %s", endTime)
        createSleepTimer(endTimeInMillis)
        analytics.logEvent(EVENT_SET_SLEEP_TIMER)
        return endTimeInMillis
    }

    private fun createSleepTimer(timeInMillis: Long) {
        val difference = timeInMillis - System.currentTimeMillis()
        dataStore.updateBlocking(SLEEP_TIMER_MILLIS, timeInMillis)

        val triggerTime = SystemClock.elapsedRealtime() + difference
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerTime,
                createBroadcastIntent(context))
        } else {
            alarmManager.setWindow(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerTime,
                10.minutes.inWholeMilliseconds,
                createBroadcastIntent(context))
        }
    }

    fun removeSleepTimer() {
        dataStore.removeBlocking(SLEEP_TIMER_MILLIS)
        getExistingBroadcast(context)?.let {
            Timber.d("Cancelling sleep timer alarm")
            alarmManager.cancel(it)
            it.cancel()
            analytics.logEvent(EVENT_REMOVE_SLEEP_TIMER)
        }
    }
}

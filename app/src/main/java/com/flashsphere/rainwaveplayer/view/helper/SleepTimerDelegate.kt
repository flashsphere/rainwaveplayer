package com.flashsphere.rainwaveplayer.view.helper

import androidx.compose.runtime.mutableStateOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.playback.PlaybackManager
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.SLEEP_TIMER_MILLIS
import com.flashsphere.rainwaveplayer.util.getBlocking
import com.flashsphere.rainwaveplayer.util.getFlow
import com.flashsphere.rainwaveplayer.util.removeBlocking
import com.flashsphere.rainwaveplayer.util.updateBlocking
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class SleepTimerDelegate @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val playbackManager: PlaybackManager,
    coroutineDispatchers: CoroutineDispatchers,
) {
    val showState = mutableStateOf(false)

    init {
        coroutineDispatchers.scope.launchWithDefaults("Sleep timer flow") {
            dataStore.getFlow(SLEEP_TIMER_MILLIS)
                .distinctUntilChanged()
                .collectLatest { timeInMillis ->
                    val delayDuration = timeInMillis - System.currentTimeMillis()
                    if (delayDuration > 0L) {
                        Timber.d("Sleep timer to run after %d ms", delayDuration)
                        delay(delayDuration.milliseconds)
                        withContext(coroutineDispatchers.main) {
                            playbackManager.stop()
                        }
                        removeSleepTimer()
                    }
                }
        }
    }

    fun getExistingSleepTimer(): Long? {
        val sleepTimerMillis = dataStore.getBlocking(SLEEP_TIMER_MILLIS)
        return if (sleepTimerMillis < System.currentTimeMillis()) {
            removeSleepTimer()
            null
        } else {
            sleepTimerMillis
        }
    }

    fun createSleepTimer(duration: Duration): Long {
        val endTime = ZonedDateTime.now()
            .plus(duration)
        val endTimeInMillis = endTime.toInstant().toEpochMilli()
        Timber.d("Creating sleep timer alarm %s", endTime)
        createSleepTimer(endTimeInMillis)
        return endTimeInMillis
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
        return endTimeInMillis
    }

    private fun createSleepTimer(timeInMillis: Long) {
        dataStore.updateBlocking(SLEEP_TIMER_MILLIS, timeInMillis)
    }

    fun removeSleepTimer() {
        dataStore.removeBlocking(SLEEP_TIMER_MILLIS)
    }
}

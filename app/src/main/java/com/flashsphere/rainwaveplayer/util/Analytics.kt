package com.flashsphere.rainwaveplayer.util

import android.os.Bundle
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.ANALYTICS
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Analytics @Inject constructor(
    coroutineDispatchers: CoroutineDispatchers,
    dataStore: DataStore<Preferences>,
) {
    private val scope = coroutineDispatchers.scope
    private var analyticsEnabled = dataStore.getBlocking(ANALYTICS)

    init {
        configureAnalytics(analyticsEnabled)
        dataStore.data
            .map { preferences -> preferences[ANALYTICS.key] ?: ANALYTICS.defaultValue }
            .distinctUntilChanged()
            .onEach { onSettingChanged(it) }
            .launchWithDefaults(scope, "Analytics setting changed")
    }

    fun logEvent(eventName: String, station: Station?) {
        if (station == null || station === Station.UNKNOWN) {
            return
        }
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, station.id.toString())
            putString(FirebaseAnalytics.Param.ITEM_NAME, station.name)
        }
        logEvent(eventName, bundle)
    }

    @JvmOverloads
    fun logEvent(eventName: String, bundle: Bundle? = null) {
        if (eventName.isBlank()) {
            return
        }
        if (!analyticsEnabled) {
            val message = StringBuilder(eventName)
            bundle?.keySet()?.forEach { key ->
                message.append("\n")
                message.append(key).append(" = ").append(bundle.getString(key))
            }
            Timber.i("%s", message)
            return
        }
        runCatching {
            Firebase.analytics.logEvent(eventName, bundle)
        }.onFailure {
            Timber.d(it, "Error logging event %s to Firebase Analytics", eventName)
        }
    }

    private fun onSettingChanged(analyticsEnabled: Boolean) {
        if (this.analyticsEnabled != analyticsEnabled) {
            this.analyticsEnabled = analyticsEnabled
            configureAnalytics(analyticsEnabled)
        }
    }

    companion object {
        const val EVENT_LOGIN = "login"
        const val EVENT_PLAY_REQUEST = "play_request"
        const val EVENT_ADD_TILE = "add_tile"
        const val EVENT_REMOVE_TILE = "remove_tile"
        const val EVENT_USE_TILE = "use_tile"
        const val EVENT_USE_SHORTCUT = "use_shortcut"
        const val EVENT_VOTE_SONG_NOTIFICATION = "vote_song_notification"
        const val EVENT_OPEN_DISCORD = "open_discord"
        const val EVENT_OPEN_PATREON = "open_patreon"
        const val EVENT_SET_SLEEP_TIMER = "set_sleep_timer"
        const val EVENT_REMOVE_SLEEP_TIMER = "remove_sleep_timer"

        private fun configureAnalytics(enabled: Boolean) {
            Timber.d("Analytics enabled = %s", enabled)
            runCatching {
                Firebase.analytics.apply {
                    setAnalyticsCollectionEnabled(enabled)
                    if (!enabled) {
                        resetAnalyticsData()
                    }
                }
            }.onFailure {
                Timber.e(it, "Error configuring Firebase Analytics")
            }
        }
    }
}

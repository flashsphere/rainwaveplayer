package com.flashsphere.rainwaveplayer.util

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.flashsphere.rainwaveplayer.util.BottomNavPreference.Labeled

object PreferencesKeys {
    private const val SETTINGS_VERSION = 9

    val VERSION = PreferenceKey(intPreferencesKey("com.flashsphere.keys.version"), SETTINGS_VERSION)
    val USER_ID = PreferenceKey(intPreferencesKey("com.flashsphere.keys.userid"), -1)
    val API_KEY = PreferenceKey(stringPreferencesKey("com.flashsphere.keys.apikey"), "")
    val LAST_PLAYED = PreferenceKey(intPreferencesKey("com.flashsphere.keys.lastplayed"), -1)
    val ADD_REQUEST_TO_TOP = PreferenceKey(booleanPreferencesKey("com.flashsphere.prefs.add_request_to_top"), false)
    val AUTO_REQUEST_FAVE = PreferenceKey(booleanPreferencesKey("com.flashsphere.prefs.auto_request_fave"), false)
    val AUTO_REQUEST_UNRATED = PreferenceKey(booleanPreferencesKey("com.flashsphere.prefs.auto_request_unrated"), false)
    val AUTO_REQUEST_CLEAR = PreferenceKey(booleanPreferencesKey("com.flashsphere.prefs.auto_request_clear"), false)
    val AUTO_PLAY = PreferenceKey(booleanPreferencesKey("com.flashsphere.prefs.auto_play"), false)
    val BUFFER_MIN = PreferenceKey(floatPreferencesKey("com.flashsphere.prefs.buffer_min"), 5F)
    val CRASH_REPORTING = PreferenceKey(booleanPreferencesKey("com.flashsphere.prefs.crash_reporting"), true)
    val ANALYTICS = PreferenceKey(booleanPreferencesKey("com.flashsphere.prefs.analytics"), true)
    val SSL_RELAY = PreferenceKey(booleanPreferencesKey("com.flashsphere.prefs.ssl_relay"), true)
    val BTM_NAV = PreferenceKey(stringPreferencesKey("com.flashsphere.prefs.btm_nav"), Labeled.value)
    val VOTE_SONG_NOTIFICATIONS = PreferenceKey(booleanPreferencesKey("com.flashsphere.prefs.vote_song_notifications"), false)
    val SLEEP_TIMER_MILLIS = PreferenceKey(longPreferencesKey("com.flashsphere.prefs.sleep_timer_MILLIS"), 0L)
    val USE_OGG = PreferenceKey(booleanPreferencesKey("com.flashsphere.prefs.use_ogg"), false)
    val AUTO_VOTE_RULES = PreferenceKey(stringPreferencesKey("com.flashsphere.prefs.auto_vote_rules"), "")
    val HIDE_RATING_UNTIL_RATED = PreferenceKey(booleanPreferencesKey("com.flashsphere.prefs.hide_rating_until_rated"), false)
    val USE_ANY_NETWORK = PreferenceKey(booleanPreferencesKey("com.flashsphere.prefs.use_any_network"), false)

    @Deprecated("Not used since pref version 8")
    val SYSTEM_RESUMPTION = booleanPreferencesKey("com.flashsphere.prefs.system_resumption")

    @Deprecated("Not used since pref version 6")
    val BUFFER_MAX = floatPreferencesKey("com.flashsphere.prefs.buffer_max")

    @Deprecated("Not used since pref version 6")
    val BUFFER_FOR_PLAYBACK = floatPreferencesKey("com.flashsphere.prefs.buffer_for_playback")

    @Deprecated("Not used since pref version 6")
    val BUFFER_REBUFFER = floatPreferencesKey("com.flashsphere.prefs.buffer_rebuffer")
}

class PreferenceKey<T>(
    val key: Preferences.Key<T>,
    val defaultValue: T,
)

enum class BottomNavPreference(
    val value: String,
) {
    Labeled("labeled"),
    Unlabeled("unlabeled"),
    Hidden("hidden");

    fun isHidden(): Boolean = this == Hidden

    companion object {
        fun of(value: String): BottomNavPreference {
            return entries.find { it.value == value } ?: Labeled
        }
    }
}

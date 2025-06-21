package com.flashsphere.rainwaveplayer.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.flashsphere.rainwaveplayer.util.BottomNavPreference.Labeled
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

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

    suspend fun exportTvPreferences(dataStore: DataStore<Preferences>, json: Json): String? {
        return dataStore.data
            .map {
                mutableMapOf<String, JsonElement>().apply {
                    exportPreference(it, USER_ID, this) { JsonPrimitive(it) }
                    exportPreference(it, API_KEY, this) { JsonPrimitive(it) }
                    exportPreference(it, ADD_REQUEST_TO_TOP, this) { JsonPrimitive(it) }
                    exportPreference(it, AUTO_REQUEST_FAVE, this) { JsonPrimitive(it) }
                    exportPreference(it, AUTO_REQUEST_UNRATED, this) { JsonPrimitive(it) }
                    exportPreference(it, AUTO_REQUEST_CLEAR, this) { JsonPrimitive(it) }
                    exportPreference(it, BUFFER_MIN, this) { JsonPrimitive(it) }
                    exportPreference(it, CRASH_REPORTING, this) { JsonPrimitive(it) }
                    exportPreference(it, ANALYTICS, this) { JsonPrimitive(it) }
                    exportPreference(it, SSL_RELAY, this) { JsonPrimitive(it) }
                    exportPreference(it, USE_OGG, this) { JsonPrimitive(it) }
                    exportPreference(it, AUTO_VOTE_RULES, this) { JsonPrimitive(it) }
                    exportPreference(it, HIDE_RATING_UNTIL_RATED, this) { JsonPrimitive(it) }
                }.let {
                    json.encodeToString(JsonObject(it))
                }.also {
                    Timber.d("Exporting TV preferences: %s", it)
                }
            }
            .firstOrNull()
    }

    suspend fun importTvPreferences(dataStore: DataStore<Preferences>, json: Json, jsonString: String) {
        val jsonPreferences = runCatching { json.decodeFromString<JsonObject?>(jsonString) }
            .getOrNull()
        if (jsonPreferences.isNullOrEmpty()) return

        Timber.d("Importing TV preferences: %s", jsonPreferences)

        dataStore.update {
            importPreference(it, USER_ID, jsonPreferences) { it.jsonPrimitive.int }
            importPreference(it, API_KEY, jsonPreferences) { it.jsonPrimitive.content }
            importPreference(it, ADD_REQUEST_TO_TOP, jsonPreferences) { it.jsonPrimitive.boolean }
            importPreference(it, AUTO_REQUEST_FAVE, jsonPreferences) { it.jsonPrimitive.boolean }
            importPreference(it, AUTO_REQUEST_UNRATED, jsonPreferences) { it.jsonPrimitive.boolean }
            importPreference(it, AUTO_REQUEST_CLEAR, jsonPreferences) { it.jsonPrimitive.boolean }
            importPreference(it, BUFFER_MIN, jsonPreferences) { it.jsonPrimitive.float }
            importPreference(it, CRASH_REPORTING, jsonPreferences) { it.jsonPrimitive.boolean }
            importPreference(it, ANALYTICS, jsonPreferences) { it.jsonPrimitive.boolean }
            importPreference(it, SSL_RELAY, jsonPreferences) { it.jsonPrimitive.boolean }
            importPreference(it, USE_OGG, jsonPreferences) { it.jsonPrimitive.boolean }
            importPreference(it, AUTO_VOTE_RULES, jsonPreferences) { it.jsonPrimitive.content }
            importPreference(it, HIDE_RATING_UNTIL_RATED, jsonPreferences) { it.jsonPrimitive.boolean }
        }
    }

    private fun <T> exportPreference(preferences: Preferences, preferenceKey: PreferenceKey<T>,
                                     content: MutableMap<String, JsonElement>, converter: (T) -> JsonElement) {
        val value = preferences[preferenceKey.key] ?: return

        content[preferenceKey.key.name] = converter(value)
    }

    private fun <T> importPreference(preferences: MutablePreferences, preferenceKey: PreferenceKey<T>,
                                     jsonObject: JsonObject, converter: (JsonElement) -> T) {
        val jsonElement = jsonObject[preferenceKey.key.name] ?: return
        preferences[preferenceKey.key] = converter(jsonElement)
    }
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

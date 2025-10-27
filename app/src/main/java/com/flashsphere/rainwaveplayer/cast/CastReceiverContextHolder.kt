package com.flashsphere.rainwaveplayer.cast

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ProcessLifecycleOwner
import com.flashsphere.rainwaveplayer.coroutine.suspendRunCatching
import com.flashsphere.rainwaveplayer.flow.MediaPlayerStateObserver
import com.flashsphere.rainwaveplayer.service.MediaService
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import com.flashsphere.rainwaveplayer.util.PreferenceKey
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.ADD_REQUEST_TO_TOP
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.API_KEY
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.AUTO_REQUEST_CLEAR
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.AUTO_REQUEST_FAVE
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.AUTO_REQUEST_UNRATED
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.AUTO_VOTE_RULES
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.BUFFER_MIN
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.HIDE_RATING_UNTIL_RATED
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.SSL_RELAY
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.USER_ID
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.USE_OGG
import com.flashsphere.rainwaveplayer.util.isTv
import com.flashsphere.rainwaveplayer.util.update
import com.google.android.gms.cast.tv.CastReceiverContext
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CastReceiverContextHolder @Inject constructor(
    @ApplicationContext applicationContext: Context,
    mediaPlayerStateObserver: MediaPlayerStateObserver,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) {
    val context: CastReceiverContext? = if (applicationContext.isTv()) {
        runCatching {
            CastReceiverContext.initInstance(applicationContext)
            CastReceiverContext.getInstance()
        }.getOrNull()
            ?.apply {
                ProcessLifecycleOwner.get().lifecycle.addObserver(
                    CastReceiverProcessLifecycleObserver(this, mediaPlayerStateObserver))
                registerEventCallback(object : CastReceiverContext.EventCallback() {
                    override fun onStopApplication() {
                        MediaService.stop(applicationContext)
                    }
                })
            }
    } else {
        null
    }

    suspend fun importTvPreferences(jsonString: String) {
        val jsonPreferences = withContext(coroutineDispatchers.compute) {
            suspendRunCatching { json.decodeFromString<JsonObject?>(jsonString) }.getOrNull()
        }
        if (jsonPreferences.isNullOrEmpty()) return

        Timber.d("Importing TV preferences: %s", jsonPreferences)

        dataStore.update { prefs ->
            importPreference(prefs, USER_ID, jsonPreferences) { it.jsonPrimitive.int }
            importPreference(prefs, API_KEY, jsonPreferences) { it.jsonPrimitive.content }
            importPreference(prefs, ADD_REQUEST_TO_TOP, jsonPreferences) { it.jsonPrimitive.boolean }
            importPreference(prefs, AUTO_REQUEST_FAVE, jsonPreferences) { it.jsonPrimitive.boolean }
            importPreference(prefs, AUTO_REQUEST_UNRATED, jsonPreferences) { it.jsonPrimitive.boolean }
            importPreference(prefs, AUTO_REQUEST_CLEAR, jsonPreferences) { it.jsonPrimitive.boolean }
            importPreference(prefs, BUFFER_MIN, jsonPreferences) { it.jsonPrimitive.float }
            importPreference(prefs, SSL_RELAY, jsonPreferences) { it.jsonPrimitive.boolean }
            importPreference(prefs, USE_OGG, jsonPreferences) { it.jsonPrimitive.boolean }
            importPreference(prefs, AUTO_VOTE_RULES, jsonPreferences) { it.jsonPrimitive.content }
            importPreference(prefs, HIDE_RATING_UNTIL_RATED, jsonPreferences) { it.jsonPrimitive.boolean }
        }
    }

    private inline fun <T> importPreference(preferences: MutablePreferences,
                                            preferenceKey: PreferenceKey<T>,
                                            jsonObject: JsonObject,
                                            converter: (JsonElement) -> T) {
        val jsonElement = jsonObject[preferenceKey.key.name] ?: return
        preferences[preferenceKey.key] = converter(jsonElement)
    }
}

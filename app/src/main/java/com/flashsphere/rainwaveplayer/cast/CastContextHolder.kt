package com.flashsphere.rainwaveplayer.cast

import android.content.Context
import androidx.annotation.MainThread
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
import com.google.android.gms.cast.CredentialsData
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CastContextHolder @Inject constructor(
    @ApplicationContext context: Context,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) {
    private val castContextDelegate = lazy {
        if (!context.isTv()) {
            runCatching {
                Timber.d("Initializing cast context")
                val task = CastContext.getSharedInstance(context, Runnable::run)
                task.result
            }.getOrNull()
        } else {
            null
        }
    }
    val castContext by castContextDelegate

    @MainThread
    fun getCastSession(): CastSession? {
        return if (castContextDelegate.isInitialized()) {
            castContext?.sessionManager?.currentCastSession
        } else {
            null
        }
    }
    @MainThread
    fun stopPlayback() {
        getCastSession()?.remoteMediaClient?.stop()
    }
    @MainThread
    fun endCastSession() {
        castContext?.sessionManager?.endCurrentSession(true)
    }
    suspend fun setCastLaunchCredentials() {
        val castContext = castContext ?: return

        exportTvPreferences()?.let { json ->
            val credentials = CredentialsData.Builder().setCredentials(json).build()
            castContext.setLaunchCredentialsData(credentials)
        }
    }

    private suspend fun exportTvPreferences(): String? =
        dataStore.data
            .map { prefs ->
                withContext(coroutineDispatchers.compute) {
                    mutableMapOf<String, JsonElement>().apply {
                        exportPreference(prefs, USER_ID, this) { JsonPrimitive(it) }
                        exportPreference(prefs, API_KEY, this) { JsonPrimitive(it) }
                        exportPreference(prefs, ADD_REQUEST_TO_TOP, this) { JsonPrimitive(it) }
                        exportPreference(prefs, AUTO_REQUEST_FAVE, this) { JsonPrimitive(it) }
                        exportPreference(prefs, AUTO_REQUEST_UNRATED, this) { JsonPrimitive(it) }
                        exportPreference(prefs, AUTO_REQUEST_CLEAR, this) { JsonPrimitive(it) }
                        exportPreference(prefs, BUFFER_MIN, this) { JsonPrimitive(it) }
                        exportPreference(prefs, SSL_RELAY, this) { JsonPrimitive(it) }
                        exportPreference(prefs, USE_OGG, this) { JsonPrimitive(it) }
                        exportPreference(prefs, AUTO_VOTE_RULES, this) { JsonPrimitive(it) }
                        exportPreference(prefs, HIDE_RATING_UNTIL_RATED, this) { JsonPrimitive(it) }
                    }.let {
                        json.encodeToString(JsonObject(it))
                    }.also {
                        Timber.d("Exporting TV preferences: %s", it)
                    }
                }
            }
            .firstOrNull()

    private inline fun <T> exportPreference(preferences: Preferences,
                                            preferenceKey: PreferenceKey<T>,
                                            content: MutableMap<String, JsonElement>,
                                            converter: (T) -> JsonElement) {
        val value = preferences[preferenceKey.key] ?: return
        content[preferenceKey.key.name] = converter(value)
    }
}

@MainThread
fun CastSession?.isPlaying(): Boolean {
    val remoteMediaClient = this?.remoteMediaClient ?: return false
    return when (remoteMediaClient.playerState) {
        MediaStatus.PLAYER_STATE_LOADING,
        MediaStatus.PLAYER_STATE_BUFFERING,
        MediaStatus.PLAYER_STATE_PLAYING -> true
        else -> false
    }
}

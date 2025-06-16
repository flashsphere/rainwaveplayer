package com.flashsphere.rainwaveplayer.cast

import android.content.Context
import androidx.annotation.MainThread
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import com.flashsphere.rainwaveplayer.util.PreferencesKeys
import com.flashsphere.rainwaveplayer.util.isTv
import com.google.android.gms.cast.CredentialsData
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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

        withContext(coroutineDispatchers.compute) {
            PreferencesKeys.exportTvPreferences(dataStore, json)
        }?.let { json ->
            castContext.setLaunchCredentialsData(
                CredentialsData.Builder().setCredentials(json).build())
        }
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

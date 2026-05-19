package com.flashsphere.rainwaveplayer.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.playback.PlaybackManager
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

class TvBackgroundPlay(
    private val context: Context,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val dataStore: DataStore<Preferences>,
    private val playbackManager: PlaybackManager,
) : DefaultLifecycleObserver {
    fun configure() {
        if (!context.isTv()) return

        dataStore.getFlow(PreferencesKeys.TV_BACKGROUND_PLAY)
            .distinctUntilChanged()
            .onEach { enabled ->
                if (enabled) {
                    enable()
                } else {
                    disable()
                }
            }
            .flowOn(coroutineDispatchers.main)
            .launchWithDefaults(coroutineDispatchers.scope, "TV background playback")
    }

    private fun enable() {
        Timber.d("Enable background play")
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }

    private fun disable() {
        Timber.d("Disable background play")
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        playbackManager.stop()
    }
}

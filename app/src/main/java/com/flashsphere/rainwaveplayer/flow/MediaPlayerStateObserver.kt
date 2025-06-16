package com.flashsphere.rainwaveplayer.flow

import android.content.Context
import android.os.Build
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.model.MediaPlayerStatus
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.service.MediaTileService
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaPlayerStateObserver @Inject constructor(
    @ApplicationContext private val context: Context,
    coroutineDispatchers: CoroutineDispatchers,
) {
    private val _flow = MutableSharedFlow<MediaPlayerStatus>(
        replay = 1,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    ).apply {
        tryEmit(MediaPlayerStatus(Station.UNKNOWN, MediaPlayerStatus.State.Stopped))
    }
    val flow: Flow<MediaPlayerStatus> = _flow.asSharedFlow().distinctUntilChanged()

    val currentState: MediaPlayerStatus get() = _flow.replayCache.first()

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            flow
                .onEach { MediaTileService.requestListeningState(context) }
                .flowOn(coroutineDispatchers.main)
                .launchWithDefaults(coroutineDispatchers.scope, "Media Player State for Tile")
        }
    }

    fun updateState(status: MediaPlayerStatus) {
        Timber.d("update status: station = %s, state = %s",
            status.station.id, status.state)
        _flow.tryEmit(status)
    }

    fun updateState(state: MediaPlayerStatus.State) {
        val status = MediaPlayerStatus(currentState.station, state)
        updateState(status)
    }
}

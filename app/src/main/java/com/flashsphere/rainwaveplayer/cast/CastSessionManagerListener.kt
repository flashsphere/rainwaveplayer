package com.flashsphere.rainwaveplayer.cast

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.flow.MediaPlayerStateObserver
import com.flashsphere.rainwaveplayer.flow.remoteMediaClientFlow
import com.flashsphere.rainwaveplayer.model.MediaPlayerStatus
import com.flashsphere.rainwaveplayer.model.MediaPlayerStatus.State
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.service.MediaService
import com.flashsphere.rainwaveplayer.util.JobUtils.cancel
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManager
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import com.google.android.gms.cast.framework.R as CastR

class CastSessionManagerListener(
    castContext: CastContext,
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val stations: List<Station>,
    private val mediaPlayerStateObserver: MediaPlayerStateObserver,
    private val castState: MutableStateFlow<String>,
) : DefaultSessionManagerListener, DefaultLifecycleObserver {

    private val sessionManager: SessionManager = castContext.sessionManager
    private var job: Job? = null

    override fun onCreate(owner: LifecycleOwner) {
        Timber.d("onCreate")
        sessionManager.addSessionManagerListener(this, CastSession::class.java)
    }

    override fun onStart(owner: LifecycleOwner) {
        Timber.d("onStart")
        val castSession = sessionManager.currentCastSession
        if (castSession != null && castSession.remoteMediaClient != null) {
            onSessionResumed(castSession, false)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        Timber.d("onDestroy")
        cleanupSession()
        sessionManager.removeSessionManagerListener(this, CastSession::class.java)
    }

    private fun cleanupSession() {
        Timber.d("cleanupSession")
        cancel(job)
    }

    private fun subscribeToPlayerState(remoteMediaClient: RemoteMediaClient, castDeviceName: String) {
        cancel(job)
        job = remoteMediaClientFlow(remoteMediaClient)
            .distinctUntilChangedBy { it.playerState }
            .onEach { result ->
                val playerState = result.playerState
                val mediaInfo = result.mediaInfo
                Timber.d("cast player state = %d", playerState)

                var station = getStation(mediaInfo, stations)

                val state = when (playerState) {
                    MediaStatus.PLAYER_STATE_LOADING, MediaStatus.PLAYER_STATE_BUFFERING -> {
                        State.Buffering
                    }
                    MediaStatus.PLAYER_STATE_PAUSED -> {
                        State.Stopped
                    }
                    MediaStatus.PLAYER_STATE_PLAYING -> {
                        State.Playing
                    }
                    else -> State.Stopped
                }

                mediaPlayerStateObserver.updateState(MediaPlayerStatus(station, state))

                val castToDeviceText = context.getString(CastR.string.cast_casting_to_device, castDeviceName)
                val stateText: String = when (state) {
                    State.Buffering -> context.getString(R.string.cast_buffering, castToDeviceText)
                    State.Playing -> context.getString(R.string.cast_playing, castToDeviceText)
                    else -> castToDeviceText
                }
                castState.value = stateText
            }
            .onCompletion {
                if (it is CancellationException) {
                    mediaPlayerStateObserver.updateState(State.Stopped)
                }
            }
            .launchWithDefaults(coroutineScope, "Cast Remote Media Client")
    }

    private fun getStation(mediaInfo: MediaInfo?, stations: List<Station>): Station {
        if (mediaInfo == null) return Station.UNKNOWN

        return runCatching { mediaInfo.contentId.toInt() }
            .recoverCatching { mediaInfo.customData!!.getInt("stationId") }
            .map { stationId -> stations.first { it.id == stationId } }
            .getOrElse { Station.UNKNOWN }
    }

    override fun onSessionStarting(castSession: CastSession) {
        Timber.d("onSessionStarting")
        castSession.castDevice?.let {
            castState.value = context.getString(CastR.string.cast_connecting_to_device, it.friendlyName)
        }
    }

    override fun onSessionStarted(castSession: CastSession, sessionId: String) {
        Timber.d("onSessionStarted")
        stopPlayback()

        castSession.castDevice?.let {
            val deviceName = it.friendlyName
            castState.value = context.getString(CastR.string.cast_casting_to_device, deviceName)
            castSession.remoteMediaClient?.let { subscribeToPlayerState(it, deviceName) }
        }
    }

    override fun onSessionStartFailed(castSession: CastSession, error: Int) {
        Timber.d("onSessionStartFailed")
        castState.value = ""
    }

    override fun onSessionEnding(castSession: CastSession) {
        Timber.d("onSessionEnding")
    }

    override fun onSessionEnded(castSession: CastSession, error: Int) {
        Timber.d("onSessionEnded")
        castState.value = ""
        cleanupSession()
    }

    override fun onSessionResuming(castSession: CastSession, sessionId: String) {
        Timber.d("onSessionResuming")
    }

    override fun onSessionResumed(castSession: CastSession, wasSuspended: Boolean) {
        Timber.d("onSessionResumed")
        stopPlayback()

        castSession.castDevice?.run {
            val deviceName = friendlyName
            castState.value = context.getString(CastR.string.cast_casting_to_device, deviceName)
            castSession.remoteMediaClient?.let { subscribeToPlayerState(it, deviceName) }
        }
    }

    override fun onSessionResumeFailed(castSession: CastSession, error: Int) {
        Timber.d("onSessionResumeFailed")
    }

    override fun onSessionSuspended(castSession: CastSession, reason: Int) {
        Timber.d("onSessionSuspended")
        castState.value = ""
        cleanupSession()
    }

    private fun stopPlayback() {
        if (mediaPlayerStateObserver.currentState.state != State.Stopped) {
            Timber.d("stopPlayback")
            MediaService.stop(context)
        }
    }
}

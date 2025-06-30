package com.flashsphere.rainwaveplayer.playback

import android.content.Context
import com.flashsphere.rainwaveplayer.cast.CastContextHolder
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.flow.MediaPlayerStateObserver
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.repository.StationRepository
import com.flashsphere.rainwaveplayer.service.MediaService
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastSession
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackManager @Inject constructor(
    private val context: Context,
    private val castContextHolder: CastContextHolder,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val stationRepository: StationRepository,
    private val mediaPlayerStateObserver: MediaPlayerStateObserver,
) {
    fun togglePlayback(station: Station) {
        val mediaPlayerStatus = mediaPlayerStateObserver.currentState
        if (mediaPlayerStatus.isPlaying(station)) {
            stop(station)
        } else {
            play(station)
        }
    }

    suspend fun play() {
        val castSession = withContext(coroutineDispatchers.main) {
            castContextHolder.getCastSession()
        }
        if (castSession == null) {
            MediaService.play(context)
        } else {
            val stations = stationRepository.getLastPlayedStationWithDefault()
            withContext(coroutineDispatchers.main) {
                play(castSession, stations)
            }
        }
    }

    fun play(station: Station) {
        val castSession = castContextHolder.getCastSession()
        if (castSession == null) {
            MediaService.playFromStation(context, station)
        } else {
            play(castSession, station)
        }
    }

    suspend fun playOnCast() {
        val lastPlayed = stationRepository.getLastPlayedStationWithDefault()
        withContext(coroutineDispatchers.main) {
            castContextHolder.getCastSession()?.let { play(it, lastPlayed) }
        }
    }

    fun play(castSession: CastSession, station: Station) {
        val metadata = MediaMetadata()
        metadata.putString(MediaMetadata.KEY_TITLE, station.name)

        val media = MediaInfo.Builder(station.id.toString())
            .setContentUrl(stationRepository.getRelayUrl(station))
            .setContentType(MIME_TYPE_AUDIO)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setMetadata(metadata)
            .build()

        val mediaLoadRequest = MediaLoadRequestData.Builder()
            .setMediaInfo(media)
            .setAutoplay(true)
            .build()

        val result = castSession.remoteMediaClient?.load(mediaLoadRequest)
        coroutineDispatchers.scope.launchWithDefaults("Remote media client load status") {
            if (result?.await()?.status?.isSuccess == true) {
                stationRepository.refreshStationInfo(station.id, 3)
            }
        }
    }

    fun stop() {
        stopLocal()
        stopCast()
    }

    private fun stopLocal() {
        MediaService.stop(context)
    }

    private fun stopCast() {
        castContextHolder.stopPlayback()
    }

    fun stop(station: Station) {
        val castSession = castContextHolder.getCastSession()
        if (castSession == null) {
            MediaService.stop(context)
        } else {
            castSession.remoteMediaClient?.stop()
            stationRepository.refreshStationInfo(station.id, 10)
        }
    }

    companion object {
        private const val MIME_TYPE_AUDIO = "audio/*"
    }
}

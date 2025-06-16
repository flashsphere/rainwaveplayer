package com.flashsphere.rainwaveplayer.media

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.ERROR_CODE_UNKNOWN_ERROR
import android.support.v4.media.session.PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN
import android.support.v4.media.session.PlaybackStateCompat.STATE_BUFFERING
import android.support.v4.media.session.PlaybackStateCompat.STATE_ERROR
import android.support.v4.media.session.PlaybackStateCompat.STATE_NONE
import android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED
import android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING
import android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED
import androidx.core.net.toUri
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.cast.CastReceiverContextHolder
import com.flashsphere.rainwaveplayer.flow.MediaPlayerStateObserver
import com.flashsphere.rainwaveplayer.model.MediaPlayerStatus
import com.flashsphere.rainwaveplayer.model.MediaPlayerStatus.State.Buffering
import com.flashsphere.rainwaveplayer.model.MediaPlayerStatus.State.Playing
import com.flashsphere.rainwaveplayer.model.MediaPlayerStatus.State.Stopped
import com.flashsphere.rainwaveplayer.model.song.Song
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.model.stationInfo.InfoResponse
import com.flashsphere.rainwaveplayer.receiver.FavoriteSongIntentHandler
import com.flashsphere.rainwaveplayer.receiver.FavoriteSongIntentHandler.Companion.ACTION_FAVORITE_SONG
import com.flashsphere.rainwaveplayer.repository.UserRepository
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.tv.media.MediaManager
import com.google.android.gms.common.images.WebImage
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

class MediaSessionHelper(
    private val context: Context,
    private val userRepository: UserRepository,
    private val mediaPlayerStateObserver: MediaPlayerStateObserver,
    castReceiverContextHolder: CastReceiverContextHolder,
) {
    private var mediaMetadataBuilder = MediaMetadataCompat.Builder()
    private var mediaSession: MediaSessionCompat
    private var mediaManager: MediaManager? = null

    @PlaybackStateCompat.State
    private var state = STATE_NONE

    init {
        val mbrComponent = MediaButtonReceiver.getComponentName(context)
        val mbrIntent = MediaButtonReceiver.getPendingIntent(context)

        mediaSession = MediaSessionCompat(context, TAG, mbrComponent, mbrIntent)
            .apply {
                setPlaybackState(buildPlaybackState(state))
                setCallback(MediaSessionCallback(context, mediaPlayerStateObserver))
                isActive = true
            }

        this.mediaManager = castReceiverContextHolder.context?.mediaManager?.also {
            it.setSessionCompatToken(mediaSession.sessionToken)
        }
    }

    fun getMediaSession(): MediaSessionCompat = mediaSession

    fun destroy() {
        state = STATE_NONE
        updatePlaybackState(state)

        mediaSession.run {
            isActive = false
            setCallback(null)
            release()
        }
        mediaManager?.setSessionCompatToken(null)
    }

    fun setConnectingState() {
        val contentText = context.resources.getText(R.string.connecting).toString()

        updatePlaybackState(STATE_BUFFERING)
        updateMetadata(contentText)

        mediaPlayerStateObserver.updateState(Buffering)
    }

    fun setConnectingState(station: Station) {
        val contentText = context.resources.getText(R.string.connecting).toString()

        updatePlaybackState(STATE_BUFFERING)
        updateMetadata(station, contentText)

        mediaPlayerStateObserver.updateState(MediaPlayerStatus(station, Buffering))
    }

    fun setBufferingState(station: Station) {
        val contentText = context.resources.getText(R.string.buffering).toString()

        updatePlaybackState(STATE_BUFFERING)
        updateMetadata(station, contentText)

        mediaPlayerStateObserver.updateState(MediaPlayerStatus(station, Buffering))
    }

    fun setWaitingForNetworkState(station: Station) {
        val contentText = context.resources.getText(R.string.waiting_for_network).toString()

        updatePlaybackState(STATE_ERROR, contentText)
        updateMetadata(station, contentText)

        mediaPlayerStateObserver.updateState(MediaPlayerStatus(station, Buffering))
    }

    fun setStoppedState(station: Station) {
        val contentText = context.resources.getText(R.string.stopped).toString()

        updatePlaybackState(STATE_STOPPED)
        updateMetadata(station, contentText)

        mediaPlayerStateObserver.updateState(MediaPlayerStatus(station, Stopped))
    }

    fun setStoppedState() {
        val contentText = context.resources.getText(R.string.stopped).toString()

        updatePlaybackState(STATE_STOPPED)
        updateMetadata(null, contentText)

        mediaPlayerStateObserver.updateState(Stopped)
    }

    fun setPausedState(station: Station) {
        val contentText = context.resources.getText(R.string.stopped).toString()

        updatePlaybackState(STATE_PAUSED)
        updateMetadata(station, contentText)

        mediaPlayerStateObserver.updateState(MediaPlayerStatus(station, Stopped))
    }

    fun setPlayingState(station: Station) {
        updatePlaybackState(STATE_PLAYING)
        updateMetadata(station)

        mediaPlayerStateObserver.updateState(MediaPlayerStatus(station, Playing))
    }

    fun setPlayingState(station: Station, infoResponse: InfoResponse, albumArt: Bitmap? = null) {
        updatePlaybackState(STATE_PLAYING, station, infoResponse)
        updateMetadata(station, infoResponse.getCurrentSong(), albumArt)

        mediaPlayerStateObserver.updateState(MediaPlayerStatus(station, Playing))
    }

    private fun getAvailableActions(): Long {
        return PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_STOP or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
    }

    private fun updateMetadata(state: String) {
        val mediaMetadata = mediaMetadataBuilder
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, null)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, null)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, null)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, null)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, state)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, -1)
            .build()

        mediaSession.setMetadata(mediaMetadata)
        updateCastMediaMetadata(mediaMetadata)
    }

    private fun updateMetadata(station: Station?, state: String) {
        Timber.d("update metadata for station")
        val mediaMetadata = mediaMetadataBuilder
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, station?.id?.toString())
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, null)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, null)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, station?.name)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, state)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, -1)
            .build()

        mediaSession.setMetadata(mediaMetadata)
        updateCastMediaMetadata(mediaMetadata)
    }

    private fun updateMetadata(station: Station) {
        val mediaMetadata = mediaMetadataBuilder
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, station.id.toString())
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, null)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, null)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, null)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, station.name)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, -1)
            .build()

        mediaSession.setMetadata(mediaMetadata)
        updateCastMediaMetadata(mediaMetadata)
    }

    private fun updateMetadata(station: Station, song: Song, albumArt: Bitmap? = null) {
        Timber.d("update metadata for station info")
        val mediaMetadata = mediaMetadataBuilder
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, station.id.toString())
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, song.getAlbumCoverUrl())
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.getArtistName())
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.getAlbumName())
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.length.seconds.inWholeMilliseconds)
            .build()

        mediaSession.setMetadata(mediaMetadata)
        updateCastMediaMetadata(mediaMetadata)
    }

    private fun updateCastMediaMetadata(metadata: MediaMetadataCompat) {
        mediaManager?.mediaStatusModifier?.mediaInfoModifier?.let { mediaInfoModifier ->
            metadata.getText(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)?.toString()?.let {
                mediaInfoModifier.contentId = it
            }

            mediaInfoModifier.metadataModifier?.let { metadataModifier ->
                metadataModifier.putString(
                    MediaMetadata.KEY_TITLE,
                    metadata.getText(MediaMetadataCompat.METADATA_KEY_TITLE)?.toString()
                )
                metadataModifier.putString(
                    MediaMetadata.KEY_ALBUM_TITLE,
                    metadata.getText(MediaMetadataCompat.METADATA_KEY_ALBUM)?.toString()
                )
                metadataModifier.putString(
                    MediaMetadata.KEY_ARTIST,
                    metadata.getText(MediaMetadataCompat.METADATA_KEY_ARTIST)?.toString()
                )

                metadata.getText(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)?.toString()?.let {
                    metadataModifier.images = listOf(WebImage(it.toUri()))
                }
            }
        }
    }

    private fun createFavCustomAction(station: Station, song: Song): PlaybackStateCompat.CustomAction {
        val favoriteIcon: Int
        val actionName: String

        if (song.favorite) {
            favoriteIcon = R.drawable.ic_favorite_white_20dp
            actionName = context.resources.getString(R.string.unfavorite_song)
        } else {
            favoriteIcon = R.drawable.ic_favorite_border_white_20dp
            actionName = context.resources.getString(R.string.favorite_song)
        }

        return PlaybackStateCompat.CustomAction.Builder(
            ACTION_FAVORITE_SONG, actionName, favoriteIcon)
            .setExtras(FavoriteSongIntentHandler.buildBundle(station, song))
            .build()
    }

    private fun createCloseCustomAction(): PlaybackStateCompat.CustomAction {
        val icon = R.drawable.ic_close_white_24dp
        val actionName = context.resources.getString(R.string.action_close)

        return PlaybackStateCompat.CustomAction.Builder(
            ACTION_CLOSE, actionName, icon)
            .build()
    }

    private fun updatePlaybackState(@PlaybackStateCompat.State state: Int,
                                    message: String? = null) {
        updatePlaybackState(state, null, null, message)
    }

    private fun updatePlaybackState(@PlaybackStateCompat.State state: Int,
                                    station: Station? = null,
                                    infoResponse: InfoResponse? = null,
                                    message: String? = null) {
        Timber.d("update playback state to %d", state)
        this.state = state

        mediaSession.run {
            setPlaybackState(buildPlaybackState(state, station, infoResponse, message))
        }
    }

    private fun buildPlaybackState(@PlaybackStateCompat.State state: Int,
                                   station: Station? = null,
                                   infoResponse: InfoResponse? = null,
                                   message: String? = null): PlaybackStateCompat {

        val playbackPosition = if (infoResponse != null && state == STATE_PLAYING) {
            infoResponse.getCurrentPosition().seconds.inWholeMilliseconds
        } else {
            PLAYBACK_POSITION_UNKNOWN
        }

        val playbackSpeed = if (state == STATE_PLAYING) {
            PLAYBACK_SPEED
        } else {
            0F
        }

        val playbackStateBuilder = PlaybackStateCompat.Builder()
            .setActions(getAvailableActions())
            .apply {
                setState(state, playbackPosition, playbackSpeed)

                if (state == STATE_ERROR) {
                    setErrorMessage(ERROR_CODE_UNKNOWN_ERROR, message)
                }
                if (userRepository.isLoggedIn()
                    && station != null
                    && infoResponse != null) {
                    addCustomAction(createFavCustomAction(station, infoResponse.getCurrentSong()))
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    addCustomAction(createCloseCustomAction())
                }
            }
        return playbackStateBuilder.build()
    }

    companion object {
        const val PLAYBACK_SPEED = 1F
        const val ACTION_CLOSE = "rainwave.intent.action.CLOSE"

        private const val TAG = "MediaSessionHelper"
    }
}

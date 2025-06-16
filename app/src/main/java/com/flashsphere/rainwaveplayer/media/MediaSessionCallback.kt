package com.flashsphere.rainwaveplayer.media

import android.content.Context
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import com.flashsphere.rainwaveplayer.flow.MediaPlayerStateObserver
import com.flashsphere.rainwaveplayer.media.MediaSessionHelper.Companion.ACTION_CLOSE
import com.flashsphere.rainwaveplayer.receiver.FavoriteSongIntentHandler
import com.flashsphere.rainwaveplayer.service.MediaService
import timber.log.Timber

class MediaSessionCallback(
    private val context: Context,
    private val mediaPlayerStateObserver: MediaPlayerStateObserver,
) : MediaSessionCompat.Callback() {
    override fun onPause() {
        Timber.i("Media session pause")
        if (mediaPlayerStateObserver.currentState.isStopped()) {
            Timber.i("Not pausing as media is not playing")
            return
        }
        MediaService.pause(context)
    }

    override fun onStop() {
        Timber.i("Media session stop")
        MediaService.stop(context)
    }

    override fun onSkipToNext() {
        Timber.i("Media session skip")
        MediaService.skipToNext(context)
    }

    override fun onSkipToPrevious() {
        Timber.i("Media session previous")
        MediaService.skipToPrev(context)
    }

    override fun onPlay() {
        Timber.i("Media session play")
        MediaService.play(context)
    }

    override fun onPlayFromSearch(query: String?, extras: Bundle?) {
        Timber.i("Media session play from search query = %s", query)
        MediaService.playFromSearch(context, query)
    }

    override fun onPlayFromMediaId(mediaId: String, extras: Bundle?) {
        Timber.i("Media session play from media id = %s", mediaId)
        MediaService.playFromMediaId(context, mediaId)
    }

    override fun onCustomAction(action: String, extras: Bundle) {
        Timber.i("onCustomAction action = %s", action)
        when (action) {
            FavoriteSongIntentHandler.ACTION_FAVORITE_SONG -> {
                FavoriteSongIntentHandler.favoriteSong(context, extras)
            }
            ACTION_CLOSE -> {
                MediaService.stop(context)
            }
        }
    }
}

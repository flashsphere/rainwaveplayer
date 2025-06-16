package com.flashsphere.rainwaveplayer.service

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.net.toUri
import androidx.media.MediaBrowserServiceCompat
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.repository.StationRepository
import timber.log.Timber

class MediaBrowserItemsCollector(
    private val root: String,
    private val stationRepository: StationRepository,
    private val mediaItems: MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>>,
) {
    init {
        mediaItems.detach()
    }

    fun process(stations: List<Station>) {
        val items = stations.map { station ->
            mapToMediaItem(station, stationRepository)
        }

        Timber.d("onLoadChildren for %s root - got %d items", root, items.size)
        mediaItems.sendResult(items)
    }

    fun process(error: Throwable) {
        Timber.d(error, "onLoadChildren for %s root - error", root)
        mediaItems.sendResult(null)
    }

    private fun mapToMediaItem(
        station: Station,
        stationRepository: StationRepository
    ): MediaBrowserCompat.MediaItem {
        val description = MediaDescriptionCompat.Builder()
            .setMediaId(station.id.toString())
            .setTitle(station.name)
            .setSubtitle(station.description)
            .setMediaUri(stationRepository.getRelayUrl(station).toUri())
            .build()
        return MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }
}

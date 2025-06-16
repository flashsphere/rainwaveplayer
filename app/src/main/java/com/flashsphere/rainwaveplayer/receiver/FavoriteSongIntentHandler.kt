package com.flashsphere.rainwaveplayer.receiver

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.core.content.IntentCompat
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.model.song.Song
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.service.MediaService
import com.flashsphere.rainwaveplayer.util.JobUtils.cancel
import com.flashsphere.rainwaveplayer.util.PendingIntentUtils.getPendingIntentFlags
import com.flashsphere.rainwaveplayer.view.viewmodel.FaveSongDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.parcelize.Parcelize

class FavoriteSongIntentHandler(
    private val coroutineScope: CoroutineScope,
    private val faveSongDelegate: FaveSongDelegate,
) {
    private var faveSongJob: Job? = null

    fun handleIntent(intent: Intent?): Boolean {
        if (intent == null || ACTION_FAVORITE_SONG != intent.action) {
            return false
        }

        val favoriteSong = IntentCompat.getParcelableExtra(intent, EXTRA_FAVORITE_SONG, FavoriteSong::class.java)
            ?: return true

        cancel(faveSongJob)
        faveSongJob = faveSongDelegate.faveSong(coroutineScope, favoriteSong.songId, favoriteSong.favorite)

        return true
    }

    @Parcelize
    data class FavoriteSong(val stationId: Int, val songId: Int, val favorite: Boolean) : Parcelable

    companion object {
        const val ACTION_FAVORITE_SONG = "rainwave.intent.action.FAVORITE_SONG"
        const val EXTRA_FAVORITE_SONG = "rainwave.intent.extra.FAVORITE_SONG"

        private const val EXTRA_STATION_ID = "rainwave.intent.extra.STATION"
        private const val EXTRA_SONG_ID = "rainwave.intent.extra.SONG"
        private const val EXTRA_FAVORITE = "rainwave.intent.extra.FAVORITE"

        fun buildBundle(station: Station, song: Song): Bundle {
            return Bundle().apply {
                putInt(EXTRA_STATION_ID, station.id)
                putInt(EXTRA_SONG_ID, song.id)
                putBoolean(EXTRA_FAVORITE, !song.favorite)
            }
        }

        fun buildPendingIntent(context: Context, station: Station, song: Song): PendingIntent {
            val intent = Intent(ACTION_FAVORITE_SONG).apply {
                component = ComponentName(context, MediaService::class.java)
                putExtra(EXTRA_FAVORITE_SONG, FavoriteSong(station.id, song.id, !song.favorite))
            }

            return PendingIntent.getService(context, R.id.favorite_song_request_code, intent, getPendingIntentFlags())
        }

        fun favoriteSong(context: Context, extras: Bundle) {
            val stationId = extras.getInt(EXTRA_STATION_ID, -1)
            val songId = extras.getInt(EXTRA_SONG_ID, -1)
            val favorite = extras.getBoolean(EXTRA_FAVORITE, false)

            if (stationId == -1 || songId == -1) {
                return
            }

            val intent = Intent(ACTION_FAVORITE_SONG).apply {
                component = ComponentName(context, MediaService::class.java)
                putExtra(EXTRA_FAVORITE_SONG, FavoriteSong(stationId, songId, favorite))
            }

            context.startService(intent)
        }
    }
}

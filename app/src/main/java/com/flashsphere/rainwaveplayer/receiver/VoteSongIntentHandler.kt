package com.flashsphere.rainwaveplayer.receiver

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.flashsphere.rainwaveplayer.model.song.Song
import com.flashsphere.rainwaveplayer.service.MediaService
import com.flashsphere.rainwaveplayer.util.Analytics
import com.flashsphere.rainwaveplayer.util.JobUtils.cancel
import com.flashsphere.rainwaveplayer.util.PendingIntentUtils.getPendingIntentFlags
import com.flashsphere.rainwaveplayer.view.viewmodel.VoteSongDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

class VoteSongIntentHandler(
    private val coroutineScope: CoroutineScope,
    private val voteSongDelegate: VoteSongDelegate,
    private val analytics: Analytics
) {
    private var voteSongJob: Job? = null

    fun handleIntent(intent: Intent?): Boolean {
        if (intent == null || ACTION_VOTE_SONG != intent.action) {
            return false
        }

        val stationId = intent.getIntExtra(EXTRA_STATION_ID, -1)
        val eventId = intent.getIntExtra(EXTRA_EVENT_ID, -1)
        val songEntryId = intent.getIntExtra(EXTRA_SONG_ENTRY_ID, -1)
        if (stationId == -1 || eventId == -1 || songEntryId == -1) {
            return true
        }

        analytics.logEvent(Analytics.EVENT_VOTE_SONG_NOTIFICATION)

        cancel(voteSongJob)
        voteSongJob = voteSongDelegate.voteSong(coroutineScope, stationId, eventId, songEntryId)

        return true
    }

    companion object {
        const val ACTION_VOTE_SONG = "rainwave.intent.action.VOTE_SONG"

        const val EXTRA_STATION_ID = "rainwave.intent.extra.STATION_ID"
        const val EXTRA_EVENT_ID = "rainwave.intent.extra.EVENT_ID"
        const val EXTRA_SONG_ENTRY_ID = "rainwave.intent.extra.SONG_ENTRY_ID"

        private fun buildIntent(context: Context, stationId: Int, eventId: Int, song: Song): Intent {
            val componentName = ComponentName(context, MediaService::class.java)
            return Intent(ACTION_VOTE_SONG).apply {
                component = componentName
                putExtra(EXTRA_STATION_ID, stationId)
                putExtra(EXTRA_EVENT_ID, eventId)
                putExtra(EXTRA_SONG_ENTRY_ID, song.entryId)
            }
        }

        fun buildPendingIntent(context: Context, requestCode: Int, stationId: Int, eventId: Int, song: Song): PendingIntent {
            val intent = buildIntent(context, stationId, eventId, song)

            return PendingIntent.getService(context, requestCode, intent, getPendingIntentFlags())
        }
    }
}

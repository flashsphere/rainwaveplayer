package com.flashsphere.rainwaveplayer.media

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.GROUP_ALERT_SUMMARY
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.media.app.NotificationCompat.MediaStyle
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.toBitmap
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.model.event.Event
import com.flashsphere.rainwaveplayer.model.song.Song
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.receiver.VoteSongIntentHandler
import com.flashsphere.rainwaveplayer.repository.UserRepository
import com.flashsphere.rainwaveplayer.util.JobUtils.cancel
import com.flashsphere.rainwaveplayer.util.PendingIntentUtils
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.VOTE_SONG_NOTIFICATIONS
import com.flashsphere.rainwaveplayer.util.getBlocking
import com.flashsphere.rainwaveplayer.util.isTv
import com.flashsphere.rainwaveplayer.view.activity.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import timber.log.Timber

class VoteSongNotificationHelper(
    private val context: Context,
    private val scope: CoroutineScope,
    private val userRepository: UserRepository,
    private val dataStore: DataStore<Preferences>,
) {
    private val isTv = context.isTv()
    private val notificationManager = NotificationManagerCompat.from(context)

    private var event: Event? = null

    private var job: Job? = null

    private fun createNotificationBuilder(station: Station): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, context.getString(R.string.channel_name_vote_song))
            .setShowWhen(false)
            .setSmallIcon(R.drawable.ic_vote_24dp)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(createNotificationContentIntent(station))
            .setGroup(VOTE_SONG_GROUP)
            .setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
    }

    private fun createNotificationContentIntent(station: Station): PendingIntent {
        val intent = MainActivity.getCallingIntent(context, station)
        return PendingIntent.getActivity(context, R.id.vote_song_notification_content_request_code, intent, PendingIntentUtils.getPendingIntentFlags())
    }

    fun showNotifications(station: Station, event: Event) {
        val userCredentials = userRepository.getCredentials()
        if (userCredentials == null || isTv) {
            return
        }

        removeNotifications()

        if (!dataStore.getBlocking(VOTE_SONG_NOTIFICATIONS)) {
            return
        }

        val hasRequestedSongOrVoted = event.songs.any { it.userIdRequested == userCredentials.userId || it.voted }
        if (hasRequestedSongOrVoted) {
            return
        }

        cancel(job)
        job = scope.launchWithDefaults("Vote Song Notifications") {
            event.songs.asReversed()
                .map { Pair(it, async { fetchImage(it) }) }
                .forEach { (song, bitmap) ->
                    showSongNotification(station, event, song, bitmap.await())
                }
            showSummaryNotification(station, event)
        }
        this.event = event
    }

    private fun showSummaryNotification(station: Station, event: Event) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val comingUpText = if (event.name.isNotEmpty()) {
                context.getString(R.string.vote_now_event, event.name)
            } else {
                context.getString(R.string.coming_up_event, context.getString(R.string.vote_now))
            }

            val summaryNotification = createNotificationBuilder(station)
                .setContentTitle(comingUpText)
                .setStyle(NotificationCompat.InboxStyle()
                    .setSummaryText(comingUpText))
                .setGroup(VOTE_SONG_GROUP)
                .setGroupSummary(true)
                .build()
            postNotification(event.id, summaryNotification)
        }
    }

    private suspend fun fetchImage(song: Song): Bitmap? {
        var bitmap: Bitmap? = null
        context.imageLoader.execute(ImageRequest.Builder(context)
            .data(song.getAlbumCoverUrl())
            .target(
                onStart = {},
                onSuccess = { result -> bitmap = result.toBitmap() },
                onError = {}
            )
            .build())

        return bitmap
    }

    private fun showSongNotification(station: Station, event: Event, song: Song, bitmap: Bitmap?) {
        val notificationId = song.entryId
        val voteSongIntent = VoteSongIntentHandler.buildPendingIntent(context, notificationId,
            station.id, event.id, song)

        val notificationBuilder = createNotificationBuilder(station)
            .setContentTitle(song.title)
            .setContentText(song.getAlbumName())
            .setStyle(MediaStyle()
                .setShowCancelButton(false)
                .setShowActionsInCompactView(0))
            .addAction(R.drawable.ic_vote_24dp, context.getString(R.string.action_vote), voteSongIntent)

        val notification = if (bitmap != null) {
            notificationBuilder.setLargeIcon(bitmap).build()
        } else {
            notificationBuilder.build()
        }
        postNotification(notificationId, notification)
    }

    private fun postNotification(notificationId: Int, notification: Notification) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notificationId, notification)
        }
    }

    fun removeNotifications(eventId: Int? = null) {
        if (eventId != null && eventId != event?.id) {
            return
        }

        Timber.d("Removing vote song notifications")
        cancel(job)

        event?.let {
            it.songs.forEach { song ->
                notificationManager.cancel(song.entryId)
            }
            notificationManager.cancel(it.id)
        }
        event = null
    }

    companion object {
        private const val VOTE_SONG_GROUP = "com.flashsphere.group.vote_song"
    }
}

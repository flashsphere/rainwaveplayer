package com.flashsphere.rainwaveplayer.media

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
import android.graphics.Bitmap
import android.os.Build
import android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE
import android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY
import android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT
import android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
import android.support.v4.media.session.PlaybackStateCompat.ACTION_STOP
import android.support.v4.media.session.PlaybackStateCompat.MediaKeyAction
import android.view.KeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.KEYCODE_MEDIA_NEXT
import android.view.KeyEvent.KEYCODE_MEDIA_PAUSE
import android.view.KeyEvent.KEYCODE_MEDIA_PLAY
import android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
import android.view.KeyEvent.KEYCODE_MEDIA_STOP
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Action
import androidx.core.app.NotificationCompat.CATEGORY_TRANSPORT
import androidx.core.graphics.drawable.toBitmap
import androidx.media.app.NotificationCompat.MediaStyle
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.model.song.Song
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.receiver.FavoriteSongIntentHandler
import com.flashsphere.rainwaveplayer.repository.UserRepository
import com.flashsphere.rainwaveplayer.util.PendingIntentUtils.getPendingIntentFlags
import com.flashsphere.rainwaveplayer.view.activity.MainActivity.Companion.getCallingIntent
import timber.log.Timber

class MediaNotificationHelper(
    private val context: Context,
    private val mediaSessionHelper: MediaSessionHelper,
    private val userRepository: UserRepository,
) {
    private val largeIcon by lazy {
        AppCompatResources.getDrawable(context, R.drawable.ic_rainwave_64dp)?.toBitmap()
    }

    private var notificationBuilder = NotificationCompat.Builder(context, context.getString(R.string.channel_name_playback))
        .setShowWhen(false)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setDeleteIntent(generateMediaPlayerIntent(context, ACTION_STOP))
        .setSmallIcon(R.drawable.ic_rainwave_24dp)
        .setLargeIcon(largeIcon)
        .setCategory(CATEGORY_TRANSPORT)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

    private fun createNotificationBuilder(station: Station?,
                                          song: Song?,
                                          action: Action): NotificationCompat.Builder {
        Timber.d("createNotificationBuilder")

        return notificationBuilder
            .clearActions()
            .apply {
                var actionsToShow = intArrayOf(1, 3)

                song?.let {
                    val contentTitle = song.title
                    val contentText = song.getAlbumName()

                    setContentIntent(getNotificationIntent(station))
                    setContentTitle(contentTitle)
                    setContentText(contentText)
                    setTicker(contentTitle + LINE_SEPARATOR + contentText)

                    if (userRepository.isLoggedIn()) {
                        actionsToShow = intArrayOf(0, 2, 4)
                        val favIntent = generateFavoriteSongIntent(context, station!!, it)

                        if (it.favorite) {
                            addAction(R.drawable.ic_favorite_white_20dp, context.getString(R.string.unfavorite_song), favIntent)
                        } else {
                            addAction(R.drawable.ic_favorite_border_white_20dp, context.getString(R.string.favorite_song), favIntent)
                        }
                    }
                }

                addAction(R.drawable.ic_skip_previous_24dp,
                    context.getString(R.string.action_prev),
                    generateMediaPlayerIntent(context, ACTION_SKIP_TO_PREVIOUS))

                addAction(action)

                addAction(R.drawable.ic_skip_next_24dp, context.getString(R.string.action_next),
                    generateMediaPlayerIntent(context, ACTION_SKIP_TO_NEXT))

                addAction(R.drawable.ic_close_white_24dp, context.getString(R.string.action_close),
                    generateMediaPlayerIntent(context, ACTION_STOP))

                setStyle(MediaStyle()
                    .setMediaSession(mediaSessionHelper.getMediaSession().sessionToken)
                    .setShowActionsInCompactView(*actionsToShow))
            }
    }

    private fun createPlayingNotificationBuilder(station: Station? = null,
                                                 song: Song? = null): NotificationCompat.Builder {
        val action = Action(R.drawable.ic_pause_24dp,
            context.getString(R.string.action_pause),
            generateMediaPlayerIntent(context, ACTION_PAUSE))
        return createNotificationBuilder(station, song, action)
    }

    private fun createStoppedNotificationBuilder(station: Station? = null): NotificationCompat.Builder {
        val action = Action(R.drawable.ic_play_arrow_24dp,
            context.getString(R.string.action_play),
            generateMediaPlayerIntent(context, ACTION_PLAY))
        return createNotificationBuilder(station, null, action)
    }

    fun createConnectingNotification(): Notification {
        val contentText = context.resources.getString(R.string.connecting)
        return createPlayingNotificationBuilder()
            .setContentIntent(getNotificationIntent())
            .setLargeIcon(largeIcon)
            .setContentTitle(contentText)
            .setTicker(contentText)
            .build()
    }

    fun createConnectingNotification(station: Station): Notification {
        val contentTitle = station.name
        val contentText = context.resources.getString(R.string.connecting)

        return createPlayingNotificationBuilder(station)
            .setContentIntent(getNotificationIntent(station))
            .setLargeIcon(largeIcon)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setTicker(contentTitle + LINE_SEPARATOR + contentText)
            .build()
    }

    fun createBufferingNotification(station: Station): Notification {
        val contentTitle = station.name
        val contentText = context.resources.getString(R.string.buffering)

        return createPlayingNotificationBuilder(station)
            .setContentIntent(getNotificationIntent(station))
            .setLargeIcon(largeIcon)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setTicker(contentTitle + LINE_SEPARATOR + contentText)
            .build()
    }

    fun createPlayingNotification(station: Station): Notification {
        return createPlayingNotificationBuilder(station)
            .setContentIntent(getNotificationIntent(station))
            .setLargeIcon(largeIcon)
            .setContentTitle(station.name)
            .setContentText(null)
            .setTicker(station.name)
            .build()
    }

    fun createWaitingForNetworkNotification(station: Station): Notification {
        val contentTitle = station.name
        val contentText = context.resources.getString(R.string.waiting_for_network)

        return createPlayingNotificationBuilder(station)
            .setContentIntent(getNotificationIntent(station))
            .setLargeIcon(largeIcon)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setTicker(contentTitle + LINE_SEPARATOR + contentText)
            .build()
    }

    fun showStoppedNotification(station: Station): Notification {
        val contentTitle = station.name
        val contentText = context.resources.getString(R.string.stopped)

        return createStoppedNotificationBuilder(station)
            .setContentIntent(getNotificationIntent(station))
            .setLargeIcon(largeIcon)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setTicker(contentTitle + LINE_SEPARATOR + contentText)
            .build()
    }

    fun createSongNotification(station: Station, song: Song): Notification {
        Timber.d("createSongNotification")

        return createPlayingNotificationBuilder(station, song)
            .setLargeIcon(largeIcon)
            .build()
    }

    fun createSongNotification(station: Station, song: Song, albumArt: Bitmap): Notification {
        Timber.d("createSongNotification")

        return createPlayingNotificationBuilder(station, song)
            .setLargeIcon(albumArt)
            .build()
    }

    private fun getNotificationIntent(station: Station? = null): PendingIntent {
        val intent: Intent = if (station == null) {
            getCallingIntent(context)
        } else {
            getCallingIntent(context, station)
        }
        return PendingIntent.getActivity(context, R.id.playback_notification_content_request_code, intent, getPendingIntentFlags())
    }

    companion object {
        val NOTIFICATION_ID = R.id.playback_notification_id
        val FOREGROUND_SERVICE_TYPE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }

        private val LINE_SEPARATOR = System.lineSeparator()

        private fun generateMediaPlayerIntent(context: Context, @MediaKeyAction action: Long): PendingIntent {
            val keyCode = when (action) {
                ACTION_PAUSE -> {
                    KEYCODE_MEDIA_PAUSE
                }
                ACTION_PLAY -> {
                    KEYCODE_MEDIA_PLAY
                }
                ACTION_SKIP_TO_PREVIOUS -> {
                    KEYCODE_MEDIA_PREVIOUS
                }
                ACTION_SKIP_TO_NEXT -> {
                    KEYCODE_MEDIA_NEXT
                }
                else -> {
                    KEYCODE_MEDIA_STOP
                }
            }
            val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                component = MediaButtonReceiver.getComponentName(context)
                putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(ACTION_DOWN, keyCode))
                putExtra("source", "notification")
            }

            return PendingIntent.getBroadcast(context, action.toInt(), intent, getPendingIntentFlags())
        }

        private fun generateFavoriteSongIntent(context: Context, station: Station, song: Song): PendingIntent {
            return FavoriteSongIntentHandler.buildPendingIntent(context, station, song)
        }
    }
}

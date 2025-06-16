package com.flashsphere.rainwaveplayer.media

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_LOW
import com.flashsphere.rainwaveplayer.R

class NotificationChannelHelper(
    val context: Context,
) {
    private val notificationManager = NotificationManagerCompat.from(context)
    private val playbackChannelName = context.getString(R.string.channel_name_playback)
    private val voteSongChannelName = context.getString(R.string.channel_name_vote_song)
    private val channels = listOf(playbackChannelName, voteSongChannelName, "cast_media_notification")

    fun setupNotificationChannels() {
        cleanupNotificationChannels()
        createPlaybackNotificationChannel()
        createVoteSongNotificationChannel()
    }

    private fun cleanupNotificationChannels() {
        notificationManager.deleteUnlistedNotificationChannels(channels)
    }

    private fun createPlaybackNotificationChannel() {
        val channel = NotificationChannelCompat.Builder(playbackChannelName, IMPORTANCE_LOW)
            .setName(playbackChannelName)
            .setShowBadge(false)
            .setLightsEnabled(false)
            .setVibrationEnabled(false)
            .build()

        notificationManager.createNotificationChannel(channel)
    }

    private fun createVoteSongNotificationChannel() {
        val channel = NotificationChannelCompat.Builder(voteSongChannelName, IMPORTANCE_LOW)
            .setName(voteSongChannelName)
            .setShowBadge(true)
            .setLightsEnabled(false)
            .setVibrationEnabled(false)
            .build()

        notificationManager.createNotificationChannel(channel)
    }
}

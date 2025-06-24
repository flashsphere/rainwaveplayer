package com.flashsphere.rainwaveplayer.view.uistate.model

import android.content.Context
import androidx.compose.runtime.Immutable
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.model.request.Request
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

@Immutable
class RequestState(
    val id: Int,
    val title: String,
    val art: String,
    val songId: Int,
    val albumName: String,
    val valid: Boolean,
    val good: Boolean,
    val cooldown: Boolean,
    val cooldownEndTime: Long,
    val electionBlocked: Boolean,
    val electionBlockedBy: String,
    val stationName: String
) {
    constructor(request: Request) : this(
        id = request.requestId,
        title = request.title,
        art = request.getAlbumCoverUrl(),
        songId = request.songId,
        albumName = request.getAlbumName(),
        valid = request.valid,
        good = request.good,
        cooldown = request.cool,
        cooldownEndTime = request.coolEndTime,
        electionBlocked = request.electionBlocked,
        electionBlockedBy = request.electionBlockedBy,
        stationName = request.stationName
    )

    fun getCooldownText(context: Context): String? {
        if (!valid && !good) {
            return context.getString(R.string.song_on_another_station, stationName)
        }
        if (cooldown) {
            return getCooldownTime(context)
        }
        if (electionBlocked) {
            return when (electionBlockedBy) {
                "album" -> context.getString(R.string.album_queued_for_voting)
                "group" -> context.getString(R.string.category_queued)
                "in_election" -> context.getString(R.string.song_queued_for_voting)
                else -> null
            }
        }
        return null
    }

    private fun getCooldownTime(context: Context): String {
        val currentTimeInSeconds = System.currentTimeMillis().milliseconds.inWholeSeconds
        return if (cooldownEndTime > currentTimeInSeconds + 20) {
            var cooldownEndTimeInSeconds = cooldownEndTime - currentTimeInSeconds

            val days = TimeUnit.SECONDS.toDays(cooldownEndTimeInSeconds)
            cooldownEndTimeInSeconds -= TimeUnit.DAYS.toSeconds(days)

            val hours = TimeUnit.SECONDS.toHours(cooldownEndTimeInSeconds)
            cooldownEndTimeInSeconds -= TimeUnit.HOURS.toSeconds(hours)

            val minutes = TimeUnit.SECONDS.toMinutes(cooldownEndTimeInSeconds)

            val cooldownEndTime = StringBuilder()
            if (days > 0) {
                cooldownEndTime.append(days).append(context.getString(R.string.time_day))
            }
            if (hours > 0) {
                if (cooldownEndTime.isNotEmpty()) {
                    cooldownEndTime.append(" ")
                }
                cooldownEndTime.append(hours).append(context.getString(R.string.time_hour))
            }
            if (minutes > 0) {
                if (cooldownEndTime.isNotEmpty()) {
                    cooldownEndTime.append(" ")
                }
                cooldownEndTime.append(minutes).append(context.getString(R.string.time_minute))
            }
            context.getString(R.string.on_cooldown, cooldownEndTime)
        } else {
            context.getString(R.string.coming_off_cooldown)
        }
    }
}

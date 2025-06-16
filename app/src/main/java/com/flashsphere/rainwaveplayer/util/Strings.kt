package com.flashsphere.rainwaveplayer.util

import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.encodeUtf8
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

object Strings {
    fun String?.toEmpty(): String {
        return this ?: ""
    }

    fun String.fromBase64(): String {
        return this.decodeBase64()?.string(StandardCharsets.UTF_8) ?: ""
    }

    fun String.toBase64(): String {
        return this.encodeUtf8().base64()
    }

    fun formatDuration(length: Duration): String {
        var remaining = length

        val hours = remaining.inWholeHours
        remaining -= hours.hours

        val minutes = remaining.inWholeMinutes
        remaining -= minutes.minutes

        val seconds = remaining.inWholeSeconds

        val cooldownEndTime = StringBuilder()
        if (hours > 0) {
            cooldownEndTime.append(hours).append(":")
            if (minutes < 10) {
                cooldownEndTime.append(0)
            }
        }
        cooldownEndTime.append(minutes)
            .append(":")
            .append(String.format(Locale.ENGLISH, "%02d", seconds))
        return cooldownEndTime.toString()
    }
}

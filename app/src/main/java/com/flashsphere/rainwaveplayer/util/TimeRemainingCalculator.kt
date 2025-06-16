package com.flashsphere.rainwaveplayer.util

import com.flashsphere.rainwaveplayer.model.stationInfo.InfoResponse
import com.flashsphere.rainwaveplayer.view.uistate.model.NowPlayingHeaderItem
import com.flashsphere.rainwaveplayer.view.uistate.model.NowPlayingSongItem
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private fun getTimeRemaining(endTime: Long, timeDifference: Duration) =
        (endTime - System.currentTimeMillis().milliseconds.inWholeSeconds +
            timeDifference.inWholeSeconds)
            .coerceAtLeast(0)

fun InfoResponse.getTimeRemaining(): Long {
    return getTimeRemaining(currentEvent.endTime, apiInfo.timeDifference)
}

fun NowPlayingHeaderItem.getTimeRemaining(): Long {
    return getTimeRemaining(eventEndTime, apiTimeDifference)
}

fun NowPlayingSongItem.getTimeRemaining(): Long {
    return getTimeRemaining(eventEndTime, apiTimeDifference)
}

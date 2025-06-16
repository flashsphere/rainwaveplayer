package com.flashsphere.rainwaveplayer.model

import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Serializable
class ApiInfo(
    val time: Long
) {
    val timeDifference: Duration = (time - System.currentTimeMillis().milliseconds.inWholeSeconds)
        .coerceAtLeast(0).seconds
}

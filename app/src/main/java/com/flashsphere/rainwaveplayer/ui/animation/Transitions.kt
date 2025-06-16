package com.flashsphere.rainwaveplayer.ui.animation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

fun fadeIn(durationMillis: Int = 200) = fadeIn(
    animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing)
)
fun fadeOut(durationMillis: Int = 200) = fadeOut(
    animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing)
)

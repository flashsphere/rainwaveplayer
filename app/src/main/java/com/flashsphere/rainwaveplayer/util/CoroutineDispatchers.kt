package com.flashsphere.rainwaveplayer.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

data class CoroutineDispatchers(
    val scope: CoroutineScope,
    val compute: CoroutineDispatcher,
    val io: CoroutineDispatcher,
    val main: CoroutineDispatcher,
)

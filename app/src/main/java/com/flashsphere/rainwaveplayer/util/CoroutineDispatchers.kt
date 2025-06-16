package com.flashsphere.rainwaveplayer.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

data class CoroutineDispatchers(
    val scope: CoroutineScope,
    val compute: CoroutineDispatcher,
    val network: CoroutineDispatcher,
    val main: CoroutineDispatcher,
)

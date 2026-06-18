package com.flashsphere.rainwaveplayer.coroutine

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException

val coroutineExceptionHandler = CoroutineExceptionHandler { coroutineContext, t ->
    Timber.e(t, "Exception in '${coroutineContext[CoroutineName]?.name ?: "unknown"}' coroutine")
}

suspend inline fun <reified T> suspendRunCatching(crossinline block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (c: CancellationException) {
    throw c
} catch (t: Throwable) {
    Timber.e(t, "Exception in '${currentCoroutineContext()[CoroutineName]?.name ?: "unknown"}' coroutine'")
    Result.failure(t)
}

fun CoroutineScope.launchWithDefaults(name: String, block: suspend CoroutineScope.() -> Unit) =
    launch(context = coroutineExceptionHandler + CoroutineName(name), block = block)

fun <T> Flow<T>.launchWithDefaults(scope: CoroutineScope, name: String) =
    this
        .onCompletion {
            Timber.d("'%s' coroutine cancelled = %s",
                currentCoroutineContext()[CoroutineName]?.name ?: "unknown", it is CancellationException)
        }
        .launchIn(scope + coroutineExceptionHandler + CoroutineName(name))

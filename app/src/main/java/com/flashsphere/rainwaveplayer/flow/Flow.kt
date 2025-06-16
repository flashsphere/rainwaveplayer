package com.flashsphere.rainwaveplayer.flow

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.RECEIVER_EXPORTED
import com.flashsphere.rainwaveplayer.cast.CastPlayerState
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import com.flashsphere.rainwaveplayer.util.ErrorUtils.isRetryable
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InterruptedIOException
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun <T> Flow<T>.autoRetry(connectivityObserver: ConnectivityObserver,
                          coroutineDispatchers: CoroutineDispatchers,
                          errorHandler: (suspend (cause: Throwable) -> Unit)? = null,
): Flow<T> = retryWhen { cause, attempt ->
    val willRetry = withContext(coroutineDispatchers.compute) {
        if (!isRetryable(cause, connectivityObserver)) {
            false
        } else if (attempt < 1L && cause is InterruptedIOException) {
            true
        } else if (attempt >= MAX_RETRIES) {
            Timber.d("exceeded max attempts")
            false
        } else {
            true
        }
    }

    if (willRetry) {
        Timber.e(cause)
        errorHandler?.invoke(cause)
    } else {
        return@retryWhen false // returning false will propagate the cause downstream
    }

    val delayDuration = 2.0.pow(attempt.toDouble()).seconds
    Timber.d("attempt %d, delay = %s", attempt, delayDuration)
    delay(delayDuration)

    // wait for connectivity
    connectivityObserver.connectivityFlow.first { connected -> connected }

    return@retryWhen true
}.catch {
    Timber.e(it)
    if (errorHandler != null) {
        errorHandler(it) // action on cause when not retrying
    } else {
        throw it
    }
}

fun <T> Flow<T>.repeatWhen(predicate: suspend FlowCollector<T>.(value: T?) -> Boolean): Flow<T> = flow {
    var shallRepeat: Boolean
    do {
        val value = collectImpl(this)
        shallRepeat = currentCoroutineContext().isActive && predicate(value)
    } while (shallRepeat)
}

internal suspend fun <T> Flow<T>.collectImpl(
    collector: FlowCollector<T>
): T? {
    var value: T? = null
    collect {
        currentCoroutineContext().ensureActive()
        collector.emit(it)
        value = it
    }
    return value
}

fun broadcastReceiverFlow(context: Context, intentFilter: IntentFilter) = callbackFlow {
    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (isActive) {
                Timber.d("receive intent %s", intent.action)
                trySend(intent)
            }
        }
    }

    val action = intentFilter.getAction(0)
    Timber.d("register '%s' broadcast receiver", action)
    ContextCompat.registerReceiver(context, broadcastReceiver, intentFilter, RECEIVER_EXPORTED)

    awaitClose {
        Timber.d("unregister '%s' broadcast receiver", action)
        context.unregisterReceiver(broadcastReceiver)
    }
}

fun remoteMediaClientFlow(remoteMediaClient: RemoteMediaClient) = callbackFlow {
    val callback = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() {
            if (isActive) {
                trySend(CastPlayerState(remoteMediaClient.playerState, remoteMediaClient.mediaInfo))
            }
        }
    }

    Timber.d("register RemoteMediaClient callback")
    remoteMediaClient.registerCallback(callback)

    trySend(CastPlayerState(remoteMediaClient.playerState, remoteMediaClient.mediaInfo))

    awaitClose {
        Timber.d("unregister RemoteMediaClient callback")
        remoteMediaClient.unregisterCallback(callback)
    }
}

fun tickerFlow(delay: Duration) = flow {
    while (currentCoroutineContext().isActive) {
        emit(Unit)
        delay(delay)
    }
}

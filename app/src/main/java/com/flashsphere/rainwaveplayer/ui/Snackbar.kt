package com.flashsphere.rainwaveplayer.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleStartEffect
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.util.JobUtils.cancel
import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.util.OperationError.Companion.Connectivity
import com.flashsphere.rainwaveplayer.util.OperationError.Companion.Server
import com.flashsphere.rainwaveplayer.util.OperationError.Companion.Unauthorized
import com.flashsphere.rainwaveplayer.util.OperationError.Companion.Unknown
import com.flashsphere.rainwaveplayer.view.helper.CustomTabsUtil
import com.flashsphere.rainwaveplayer.view.uistate.event.SnackbarEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@Composable
fun EventsSnackbar(snackbarHostState: SnackbarHostState,
                   events: Flow<SnackbarEvent>) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    LifecycleStartEffect(Unit) {
        val job = scope.launch {
            events.collect { event ->
                val state = event.toSnackbarState(context)
                if (state.type == SnackbarStateType.Show && state.data != null) {
                    scope.launch {
                        launchSnackbar(snackbarHostState, state.data)
                    }
                } else if (state.type == SnackbarStateType.Dismiss) {
                    scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                    }
                }
            }
        }
        onStopOrDispose { cancel(job) }
    }
}

@Composable
fun TvEventsSnackbarAsToast(
    events: Flow<SnackbarEvent>,
    toastState: MutableState<Toast?> = remember { mutableStateOf(null) },
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    LifecycleStartEffect(Unit) {
        val job = events
            .onCompletion {
                toastState.value?.cancel()
                toastState.value = null
            }
            .onEach { event ->
                val state = event.toSnackbarState(context)
                if (state.type == SnackbarStateType.Show && state.data != null) {
                    scope.launch {
                        toastState.value?.cancel()
                        toastState.value = null
                        toastState.value = Toast.makeText(context, state.data.message, Toast.LENGTH_LONG)
                            .apply { show() }
                    }
                }
            }
            .launchIn(scope)
        onStopOrDispose { cancel(job) }
    }
}

suspend fun launchSnackbar(
    snackbarHostState: SnackbarHostState,
    data: SnackbarStateData
) {
    snackbarHostState.currentSnackbarData?.dismiss()
    val result = snackbarHostState.showSnackbar(
        message = data.message,
        actionLabel = if (data.action != null) {
            data.actionLabel
        } else {
            null
        },
        duration = data.duration,
    )
    when (result) {
        SnackbarResult.ActionPerformed -> {
            data.action?.invoke()
        }

        SnackbarResult.Dismissed -> {
            data.dismissAction?.invoke()
        }
    }
}

enum class SnackbarStateType {
    Show, Dismiss
}

@Immutable
class SnackbarState(
    val type: SnackbarStateType,
    val data: SnackbarStateData? = null,
)

@Immutable
class SnackbarStateData(
    val message: String,
    val duration: SnackbarDuration,
    val actionLabel: String? = null,
    val action: (() -> Unit)? = null,
    val dismissAction: (() -> Unit)? = null,
)

fun OperationError.toSnackbarData(context: Context, retryAction: (() -> Unit)): SnackbarStateData {
    return toSnackbarData(
        context = context,
        defaultMessage = context.getString(R.string.error_connection),
        defaultDuration = SnackbarDuration.Indefinite,
        retryAction = retryAction,
    )
}

fun OperationError.toSnackbarData(context: Context,
                                  defaultMessage: String,
                                  defaultDuration: SnackbarDuration,
                                  retryAction: (() -> Unit)? = null,
                                  dismissAction: (() -> Unit)? = null): SnackbarStateData {
    val isUnauthorized = type == Unauthorized
    val message = getMessage(context, defaultMessage)
    val duration = when (type) {
        Connectivity -> SnackbarDuration.Indefinite
        Unauthorized -> SnackbarDuration.Long
        Server -> SnackbarDuration.Long
        else -> defaultDuration
    }

    var actionLabel: String? = null
    var action: (() -> Unit)? = null

    if (isUnauthorized) {
        actionLabel = context.getString(R.string.login)
        action = { CustomTabsUtil.openLoginPage(context) }
    } else if (retryAction != null) {
        if (type == Unknown || type == Connectivity) {
            actionLabel = context.getString(R.string.action_retry)
            action = retryAction
        }
    }

    return SnackbarStateData(
        message = message,
        duration = duration,
        actionLabel = actionLabel,
        action = action,
        dismissAction = dismissAction
    )
}

package com.flashsphere.rainwaveplayer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.lifecycle.compose.LifecycleStartEffect
import com.flashsphere.rainwaveplayer.ui.composition.LastFocused
import com.flashsphere.rainwaveplayer.ui.composition.LocalLastFocused
import com.flashsphere.rainwaveplayer.util.JobUtils.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

@Composable
fun Modifier.saveLastFocused(
    tag: String,
    focusRequester: FocusRequester = remember(tag) { FocusRequester() },
): Modifier {
    val lastFocused = LocalLastFocused.current
    val scope = rememberCoroutineScope()
    LifecycleStartEffect(lastFocused) {
        val job = snapshotFlow { lastFocused.value }
            .filter { it.tag == tag && it.shouldRequestFocus }
            .onEach {
                Timber.d("snapshotFlow -> requesting focus for %s", tag)
                focusRequester.requestFocus()
            }
            .launchIn(scope)
        onStopOrDispose { cancel(job) }
    }

    return this then Modifier.focusRequester(focusRequester)
        .onFocusChanged {
            if (it.hasFocus || it.isFocused || it.isCaptured) {
                lastFocused.value = LastFocused(tag = tag, shouldRequestFocus = false)
            }
        }
        .onGloballyPositioned {
            if (lastFocused.value.tag == tag && lastFocused.value.shouldRequestFocus) {
                Timber.d("onGloballyPositioned -> requesting focus for %s", tag)
                focusRequester.requestFocus()
            }
        }
}

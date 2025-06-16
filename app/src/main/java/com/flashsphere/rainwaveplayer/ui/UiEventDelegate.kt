package com.flashsphere.rainwaveplayer.ui

import com.flashsphere.rainwaveplayer.view.uistate.event.MessageEvent
import com.flashsphere.rainwaveplayer.view.uistate.event.SnackbarEvent
import com.flashsphere.rainwaveplayer.view.uistate.event.UiEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow.SUSPEND
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

class UiEventDelegate(private val scope: CoroutineScope) {
    private val _events = MutableSharedFlow<UiEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        SUSPEND
    )
    val events = _events.asSharedFlow()

    val snackbarEvents = events.filterIsInstance<SnackbarEvent>()

    fun send(event: UiEvent) {
        scope.launch { _events.emit(event) }
    }

    fun sendSnackbarEvent(message: String) {
        send(MessageEvent(message))
    }
}

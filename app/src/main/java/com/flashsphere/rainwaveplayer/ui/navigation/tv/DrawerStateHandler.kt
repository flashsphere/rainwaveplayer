package com.flashsphere.rainwaveplayer.ui.navigation.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.tv.material3.DrawerValue
import com.flashsphere.rainwaveplayer.ui.composition.LocalDrawerState
import com.flashsphere.rainwaveplayer.ui.composition.LocalLastFocused
import kotlinx.coroutines.flow.filter

@Composable
fun DrawerStateHandler() {
    val lastFocused = LocalLastFocused.current
    val drawerState = LocalDrawerState.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(drawerState) {
        snapshotFlow { drawerState.currentValue }
            .filter { it == DrawerValue.Closed }
            .collect { focusRequester.requestFocus() }
    }
    BackHandler(drawerState.currentValue == DrawerValue.Open) {
        focusRequester.requestFocus()
    }
    Spacer(Modifier
        .onKeyEvent {
            if (it.key == Key.DirectionLeft || it.key == Key.DirectionRight ||
                it.key == Key.DirectionUp || it.key == Key.DirectionDown) {
                focusManager.moveFocus(FocusDirection.Next)
            }
            return@onKeyEvent false
        }
        .focusRequester(focusRequester)
        .onFocusChanged {
            if (it.hasFocus || it.isFocused || it.isCaptured) {
                lastFocused.value = lastFocused.value.copy(shouldRequestFocus = true)
            }
        }
        .focusable())
}

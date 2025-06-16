package com.flashsphere.rainwaveplayer.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClearFocusOnKeyboardHide(focusState: State<Boolean>) {
    val focusManager = LocalFocusManager.current
    val imeVisible = WindowInsets.isImeVisible
    val wasImeVisible = remember { mutableStateOf(imeVisible) }

    LaunchedEffect(imeVisible) {
        if (!imeVisible && wasImeVisible.value && focusState.value) {
            focusManager.clearFocus()
        }
        wasImeVisible.value = imeVisible
    }
}

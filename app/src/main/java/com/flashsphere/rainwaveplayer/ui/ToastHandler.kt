package com.flashsphere.rainwaveplayer.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.lifecycle.compose.LifecycleResumeEffect

@Composable
fun ToastHandler(state: MutableState<Toast?>) {
    val toast = state.value ?: return

    LifecycleResumeEffect(toast) {
        onPauseOrDispose { toast.cancel() }
    }
    LaunchedEffect(toast) {
        toast.show()
    }
}

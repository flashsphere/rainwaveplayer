package com.flashsphere.rainwaveplayer.internal

import android.content.Context
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ProcessLifecycleInitializer
import androidx.startup.Initializer

class ComposeInitializer: Initializer<ComposeView> {
    override fun create(context: Context): ComposeView {
        return ComposeView(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(ProcessLifecycleInitializer::class.java)
    }
}

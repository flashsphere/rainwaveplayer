package com.flashsphere.rainwaveplayer.network

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.ConnectivityManager.CONNECTIVITY_ACTION
import androidx.core.content.ContextCompat
import com.flashsphere.rainwaveplayer.coroutine.coroutineExceptionHandler
import com.flashsphere.rainwaveplayer.flow.broadcastReceiverFlow
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

@Suppress("DEPRECATION")
class NoOpNetworkManager(context: Context, coroutineDispatchers: CoroutineDispatchers) : NetworkManager {
    private val scope = CoroutineScope(coroutineDispatchers.main + SupervisorJob() + coroutineExceptionHandler)
    private val applicationContext = context.applicationContext
    private val connectivityManager = ContextCompat.getSystemService(applicationContext,
        ConnectivityManager::class.java)!!

    override fun isConnected(): Boolean =
        connectivityManager.activeNetworkInfo?.isConnected == true

    override val connectivityFlow = broadcastReceiverFlow(applicationContext, IntentFilter(CONNECTIVITY_ACTION))
        .map { isConnected() }
        .distinctUntilChanged()
        .buffer(0)
        .shareIn(scope = scope, started = WhileSubscribed(replayExpirationMillis = 0), replay = 0)
}

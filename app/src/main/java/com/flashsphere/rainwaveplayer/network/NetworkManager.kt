package com.flashsphere.rainwaveplayer.network

import androidx.lifecycle.DefaultLifecycleObserver
import kotlinx.coroutines.flow.Flow

interface NetworkManager : DefaultLifecycleObserver {
    fun isConnected(): Boolean
    val connectivityFlow: Flow<Boolean>

    fun registerNetworkChangeCallback(callback: NetworkChangeCallback) {}
    fun unregisterNetworkChangeCallback(callback: NetworkChangeCallback) {}

    companion object {
        const val TAG: String = "NetworkManager"
    }
}

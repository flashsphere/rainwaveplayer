package com.flashsphere.rainwaveplayer.flow

import com.flashsphere.rainwaveplayer.network.NetworkManager
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class ConnectivityObserver @Inject constructor(
    private val networkManager: NetworkManager,
) {
    fun isConnected() = networkManager.isConnected()
    val connectivityFlow: Flow<Boolean> = networkManager.connectivityFlow
}

const val MAX_RETRIES = 10

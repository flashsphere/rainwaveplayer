package com.flashsphere.rainwaveplayer.flow

import com.flashsphere.rainwaveplayer.network.NetworkManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectivityObserver @Inject constructor(
    private val networkManager: NetworkManager,
) {
    fun isConnected() = networkManager.isConnected()
    val connectivityFlow: Flow<Boolean> = networkManager.connectivityFlow
}

const val MAX_RETRIES = 10

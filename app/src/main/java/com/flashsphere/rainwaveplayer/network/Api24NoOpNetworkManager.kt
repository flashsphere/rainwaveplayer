package com.flashsphere.rainwaveplayer.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.flashsphere.rainwaveplayer.coroutine.coroutineExceptionHandler
import com.flashsphere.rainwaveplayer.network.NetworkManager.Companion.TAG
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import timber.log.Timber

@RequiresApi(Build.VERSION_CODES.N)
class Api24NoOpNetworkManager(context: Context, coroutineDispatchers: CoroutineDispatchers) : NetworkManager {
    private val scope = CoroutineScope(coroutineDispatchers.main + SupervisorJob() + coroutineExceptionHandler)
    private val applicationContext = context.applicationContext
    private val connectivityManager = ContextCompat.getSystemService(applicationContext,
        ConnectivityManager::class.java)!!

    override fun isConnected(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return hasInternetNetworkCapability(networkCapabilities)
    }

    private fun hasInternetNetworkCapability(capabilities: NetworkCapabilities): Boolean {
        return capabilities.hasCapability(NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NET_CAPABILITY_VALIDATED)
    }

    override val connectivityFlow = callbackFlow {
        val networkCallback = object : NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                if (isActive) {
                    val hasInternet = hasInternetNetworkCapability(capabilities)
                    Timber.tag(TAG).d("has internet capabilities: %s", hasInternet)
                    trySend(hasInternet)
                }
            }

            override fun onLost(ignored: Network) {
                if (isActive) {
                    Timber.tag(TAG).d("network lost")
                    trySend(false)
                }
            }
        }

        Timber.tag(TAG).d("register default network callback")
        connectivityManager.registerDefaultNetworkCallback(networkCallback)

        trySend(isConnected())

        awaitClose {
            Timber.tag(TAG).d("unregister network callback")
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }
    .distinctUntilChanged()
    .buffer(0)
    .shareIn(scope = scope, started = WhileSubscribed(replayExpirationMillis = 0), replay = 0)
}

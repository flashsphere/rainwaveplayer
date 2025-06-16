package com.flashsphere.rainwaveplayer.flow

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.ConnectivityManager.CONNECTIVITY_ACTION
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.flashsphere.rainwaveplayer.util.CoroutineDispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectivityObserver @Inject constructor(
    context: Context,
    coroutineDispatchers: CoroutineDispatchers,
) {
    private val connectivityCompat: ConnectivityCompat
    val connectivityFlow: Flow<Boolean>

    init {
        connectivityCompat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Api23ConnectivityImpl(context)
        } else {
            BaseConnectivityImpl(context)
        }

        connectivityFlow = connectivityCompat.createConnectivityFlow()
            .distinctUntilChanged()
            .shareIn(coroutineDispatchers.scope, WhileSubscribed(replayExpirationMillis = 0))
    }

    fun isConnected() = connectivityCompat.isConnected()
}

const val MAX_RETRIES = 10

sealed interface ConnectivityCompat {
    fun isConnected(): Boolean
    fun createConnectivityFlow(): Flow<Boolean>
}

class BaseConnectivityImpl(
    context: Context,
) : ConnectivityCompat {
    private val appContext = context.applicationContext
    private val connectivityManager = ContextCompat.getSystemService(appContext, ConnectivityManager::class.java)!!

    override fun isConnected(): Boolean {
        @Suppress("DEPRECATION")
        return connectivityManager.activeNetworkInfo?.isConnected ?: false
    }

    override fun createConnectivityFlow(): Flow<Boolean> {
        @Suppress("DEPRECATION")
        val intentFilter = IntentFilter(CONNECTIVITY_ACTION)
        return broadcastReceiverFlow(appContext, intentFilter)
            .map { isConnected() }
            .onStart { emit(isConnected()) }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
class Api23ConnectivityImpl(
    context: Context,
) : ConnectivityCompat {
    private val appContext = context.applicationContext
    private val connectivityManager = ContextCompat.getSystemService(appContext, ConnectivityManager::class.java)!!

    override fun isConnected(): Boolean {
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val activeNetwork =
            connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        return activeNetwork.run {
            hasCapability(NET_CAPABILITY_INTERNET) && hasCapability(NET_CAPABILITY_VALIDATED)
        }
    }

    override fun createConnectivityFlow(): Flow<Boolean> = callbackFlow {
        val networkCallback = object : NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (isActive) {
                    Timber.i("network available")
                    trySend(true)
                }
            }

            override fun onLost(ignored: Network) {
                if (isActive) {
                    Timber.i("network lost")
                    trySend(false)
                }
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NET_CAPABILITY_INTERNET)
            .addCapability(NET_CAPABILITY_VALIDATED)
            .build()

        Timber.i("register network callback")
        connectivityManager.registerNetworkCallback(request, networkCallback)

        trySend(isConnected())

        awaitClose {
            runCatching {
                Timber.i("unregister network callback")
                connectivityManager.unregisterNetworkCallback(networkCallback)
            }.onFailure {
                Timber.e(it, "Unable to unregister network callback")
            }
        }
    }
}


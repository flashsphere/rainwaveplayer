package com.flashsphere.rainwaveplayer.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED
import android.net.NetworkCapabilities.TRANSPORT_CELLULAR
import android.net.NetworkCapabilities.TRANSPORT_ETHERNET
import android.net.NetworkCapabilities.TRANSPORT_VPN
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.flashsphere.rainwaveplayer.network.NetworkManager.Companion.TAG
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@RequiresApi(Build.VERSION_CODES.N)
class Api24AnyNetworkManager(context: Context) : NetworkManager {
    private val applicationContext = context.applicationContext
    private val connectivityManager = ContextCompat.getSystemService(applicationContext,
        ConnectivityManager::class.java)!!

    private val handler = Handler(Looper.getMainLooper())
    private val availableNetworks = ConcurrentHashMap<Network, NetworkCapabilities>()
    private val networkChangeCallbacks = ConcurrentHashMap.newKeySet<NetworkChangeCallback>()

    private var currentNetwork: Network? = null
    private var usingDefaultNetwork: Boolean = true

    override fun isConnected(): Boolean = _connectivityFlow.value

    private val _connectivityFlow = MutableStateFlow(false)
    override val connectivityFlow = _connectivityFlow.asStateFlow()

    private var defaultNetworkCallback = object : NetworkCallback() {
        private var previousNetwork = Pair<Network?, Boolean>(null, false)

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            val hasInternet = capabilities.hasCapability(NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NET_CAPABILITY_VALIDATED)

            if (setPreviousNetwork(network, hasInternet)) {
                handleDefaultNetworkInternetCapability(network, hasInternet)
            }
        }

        override fun onLost(network: Network) {
            if (setPreviousNetwork(network, false)) {
                handleDefaultNetworkInternetCapability(network, false)
            }
        }

        private fun setPreviousNetwork(network: Network?, hasInternet: Boolean): Boolean {
            val currentNetwork = Pair(network, hasInternet)
            if (previousNetwork == currentNetwork) return false

            previousNetwork = currentNetwork
            return true
        }

        fun clear() {
            previousNetwork = Pair(null, false)
        }
    }

    private val networkCallback = object : NetworkCallback() {
        override fun onAvailable(network: Network) {
            Timber.tag(TAG).d("Got network %s", network)
        }

        override fun onLost(network: Network) {
            if (availableNetworks.contains(network)) {
                Timber.tag(TAG).d("Lost network %s", network)
            }
            availableNetworks.remove(network)
            selectFromAvailableNetworks()
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            availableNetworks.put(network, capabilities)
            selectFromAvailableNetworks()
        }
    }

    override fun registerNetworkChangeCallback(callback: NetworkChangeCallback) {
        networkChangeCallbacks.add(callback)
    }

    override fun unregisterNetworkChangeCallback(callback: NetworkChangeCallback) {
        networkChangeCallbacks.remove(callback)
    }

    private fun start() {
        Timber.tag(TAG).d("Register network callback")
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NET_CAPABILITY_INTERNET)
            .addCapability(NET_CAPABILITY_VALIDATED)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        connectivityManager.registerDefaultNetworkCallback(defaultNetworkCallback)
    }

    private fun stop() {
        Timber.tag(TAG).d("Unregister network callback")
        connectivityManager.unregisterNetworkCallback(defaultNetworkCallback)
        connectivityManager.unregisterNetworkCallback(networkCallback)

        handler.removeCallbacksAndMessages(null)

        connectivityManager.bindProcessToNetwork(null)
        usingDefaultNetwork = true
        _connectivityFlow.value = false
        currentNetwork = null

        availableNetworks.clear()
        networkChangeCallbacks.clear()
        defaultNetworkCallback.clear()
    }

    private fun runNetworkChangeCallbacks(network: Network?) {
        Timber.tag(TAG).d("Network %s change callback ", network)
        networkChangeCallbacks.forEach { callback ->
            handler.post { callback.networkChanged() }
        }
    }

    private fun handleDefaultNetworkInternetCapability(network: Network?, hasInternet: Boolean) {
        if (hasInternet) {
            Timber.tag(TAG).d("Using default network %s", network)
            if (connectivityManager.boundNetworkForProcess != null) {
                connectivityManager.bindProcessToNetwork(null)
            }
            usingDefaultNetwork = true
            _connectivityFlow.value = true
            if (currentNetwork != network) {
                currentNetwork = network
                runNetworkChangeCallbacks(network)
            }
        } else {
            Timber.tag(TAG).d("Default network %s has no internet", network)
            availableNetworks.remove(network)
            usingDefaultNetwork = false
            selectFromAvailableNetworks()
        }
    }

    private fun selectFromAvailableNetworks() {
        if (usingDefaultNetwork) return

        Timber.tag(TAG).d("%d available networks", availableNetworks.size)

        val selectedNetwork = availableNetworks.minByOrNull { (_, capabilities) ->
            TRANSPORT_PRIORITY.indexOfFirst { capabilities.hasTransport(it) }
        }?.key

        if (selectedNetwork != connectivityManager.boundNetworkForProcess) {
            Timber.tag(TAG).d("Using network %s", selectedNetwork)
            connectivityManager.bindProcessToNetwork(selectedNetwork)
            logNetworkCapabilities(selectedNetwork)
        }
        _connectivityFlow.value = selectedNetwork != null
        if (currentNetwork != selectedNetwork) {
            currentNetwork = selectedNetwork
            selectedNetwork?.let { runNetworkChangeCallbacks(it) }
        }
    }

    private fun logNetworkCapabilities(network: Network?) {
        if (network == null) return

        availableNetworks[network]?.let {
            Timber.tag(TAG).d("""
                is ethernet = %s
                is wifi = %s
                is cellular = %s
                is vpn = %s
                has internet = %s
                has validated = %s
            """.trimIndent(),
                it.hasTransport(TRANSPORT_ETHERNET),
                it.hasTransport(TRANSPORT_WIFI),
                it.hasTransport(TRANSPORT_CELLULAR),
                it.hasTransport(TRANSPORT_VPN),
                it.hasCapability(NET_CAPABILITY_INTERNET),
                it.hasCapability(NET_CAPABILITY_VALIDATED)
            )
        }
    }

    private val count = AtomicInteger(0)
    override fun onStart(owner: LifecycleOwner) {
        if (count.getAndAdd(1) == 0) {
            start()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        if (count.decrementAndGet() > 0) return
        stop()
    }

    companion object {
        private val TRANSPORT_PRIORITY = intArrayOf(
            TRANSPORT_ETHERNET,
            TRANSPORT_WIFI,
            TRANSPORT_CELLULAR,
        )
    }
}

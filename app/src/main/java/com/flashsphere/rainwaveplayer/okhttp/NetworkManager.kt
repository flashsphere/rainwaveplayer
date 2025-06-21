package com.flashsphere.rainwaveplayer.okhttp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.ConnectivityManager.CONNECTIVITY_ACTION
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
import androidx.core.content.ContextCompat.RECEIVER_EXPORTED
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.flashsphere.rainwaveplayer.util.PreferencesKeys
import com.flashsphere.rainwaveplayer.util.getBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

abstract class NetworkManager : DefaultLifecycleObserver {
    abstract fun isConnected(): Boolean
    abstract val connectivityFlow: Flow<Boolean>

    abstract fun registerNetworkChangeCallback(callback: NetworkChangeCallback)
    abstract fun unregisterNetworkChangeCallback(callback: NetworkChangeCallback)
    protected abstract fun start()
    protected abstract fun stop()

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
        val TAG: String = NetworkManager::class.java.simpleName
    }
}

interface NetworkChangeCallback {
    fun networkChanged()
}

@Suppress("DEPRECATION")
class NoOpNetworkManager(context: Context) : NetworkManager() {
    private val applicationContext = context.applicationContext
    private val connectivityManager = ContextCompat.getSystemService(applicationContext,
        ConnectivityManager::class.java)!!

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.tag(TAG).d("receive intent %s", intent.action)
            _connectivityFlow.value = isConnected()
        }
    }

    override fun isConnected(): Boolean =
        connectivityManager.activeNetworkInfo?.isConnected == true

    private val _connectivityFlow = MutableStateFlow(false)
    override val connectivityFlow: Flow<Boolean> = _connectivityFlow.asStateFlow()

    override fun registerNetworkChangeCallback(callback: NetworkChangeCallback) {}

    override fun unregisterNetworkChangeCallback(callback: NetworkChangeCallback) {}

    override fun start() {
        Timber.tag(TAG).d("register '%s' broadcast receiver", CONNECTIVITY_ACTION)
        ContextCompat.registerReceiver(applicationContext, broadcastReceiver, IntentFilter(CONNECTIVITY_ACTION), RECEIVER_EXPORTED)
    }

    override fun stop() {
        applicationContext.unregisterReceiver(broadcastReceiver)
    }
}

@RequiresApi(Build.VERSION_CODES.N)
class Api24NetworkManager(
    context: Context,
    dataStore: DataStore<Preferences>,
) : NetworkManager() {
    private val applicationContext = context.applicationContext
    private val connectivityManager = ContextCompat.getSystemService(applicationContext,
        ConnectivityManager::class.java)!!
    private val useAnyNetwork = dataStore.getBlocking(PreferencesKeys.USE_ANY_NETWORK)

    private val handler = Handler(Looper.getMainLooper())
    private val availableNetworks = mutableSetOf<Network>()
    private val networkCapabilitiesMap = mutableMapOf<Network, NetworkCapabilities>()
    private val networkChangeCallbacks = mutableSetOf<NetworkChangeCallback>()

    private var currentNetwork: Network? = connectivityManager.activeNetwork
    private var usingDefaultNetwork: Boolean = true

    override fun isConnected(): Boolean = _connectivityFlow.value

    private val _connectivityFlow = MutableStateFlow(true)
    override val connectivityFlow = _connectivityFlow.asStateFlow()

    private var defaultNetworkCallback = object : NetworkCallback() {
        private var previousNetwork = Pair<Network?, Boolean>(null, false)

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            val hasInternet = capabilities.hasCapability(NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NET_CAPABILITY_VALIDATED)

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
            previousNetwork = Pair<Network?, Boolean>(null, false)
        }
    }

    private val networkCallback = object : NetworkCallback() {
        override fun onAvailable(network: Network) {
            Timber.tag(TAG).d("Got network %s", network)
            availableNetworks.add(network)
        }

        override fun onLost(network: Network) {
            if (availableNetworks.contains(network)) {
                Timber.tag(TAG).d("Lost network %s", network)
            }
            availableNetworks.remove(network)
            networkCapabilitiesMap.remove(network)

            selectFromAvailableNetworks()
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            networkCapabilitiesMap.put(network, capabilities)
            selectFromAvailableNetworks()
        }
    }

    override fun registerNetworkChangeCallback(callback: NetworkChangeCallback) {
        networkChangeCallbacks.add(callback)
    }

    override fun unregisterNetworkChangeCallback(callback: NetworkChangeCallback) {
        networkChangeCallbacks.remove(callback)
    }

    override fun start() {
        Timber.tag(TAG).d("Register network callback")
        if (useAnyNetwork) {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NET_CAPABILITY_INTERNET)
                .addCapability(NET_CAPABILITY_VALIDATED)
                .build()
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        }
        connectivityManager.registerDefaultNetworkCallback(defaultNetworkCallback)
    }

    override fun stop() {
        Timber.tag(TAG).d("Unregister network callback")
        connectivityManager.unregisterNetworkCallback(defaultNetworkCallback)
        if (useAnyNetwork) {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }

        handler.removeCallbacksAndMessages(null)

        usingDefaultNetwork = true
        _connectivityFlow.value = true
        currentNetwork = connectivityManager.activeNetwork
        connectivityManager.bindProcessToNetwork(null)

        availableNetworks.clear()
        networkCapabilitiesMap.clear()
        networkChangeCallbacks.clear()
        defaultNetworkCallback.clear()
    }

    private fun runNetworkChangeCallbacks(network: Network?) {
        Timber.tag(TAG).d("Network %s change callback ", network)
        handler.post {
            networkChangeCallbacks.forEach { callback ->
                callback.networkChanged()
            }
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
            usingDefaultNetwork = false
            selectFromAvailableNetworks()
        }
    }

    private fun selectFromAvailableNetworks() {
        if (usingDefaultNetwork) return

        Timber.tag(TAG).d("%d available networks: %s", availableNetworks.size, availableNetworks)

        val selectedNetwork = availableNetworks.minByOrNull { network ->
            val capabilities = networkCapabilitiesMap[network]
            if (capabilities == null) {
                Int.MAX_VALUE
            } else {
                TRANSPORT_PRIORITY.indexOfFirst { capabilities.hasTransport(it) }
            }
        }

        if (selectedNetwork != connectivityManager.boundNetworkForProcess) {
            Timber.tag(TAG).d("Using network %s", selectedNetwork)
            connectivityManager.bindProcessToNetwork(selectedNetwork)
            logNetworkCapabilities(selectedNetwork)
        }
        _connectivityFlow.value = selectedNetwork != null
        if (selectedNetwork != null && currentNetwork != selectedNetwork) {
            currentNetwork = selectedNetwork
            runNetworkChangeCallbacks(selectedNetwork)
        }
    }

    private fun logNetworkCapabilities(network: Network?) {
        if (network == null) return
        networkCapabilitiesMap[network]?.let {
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

    companion object {
        private val TRANSPORT_PRIORITY = intArrayOf(
            TRANSPORT_ETHERNET,
            TRANSPORT_WIFI,
            TRANSPORT_CELLULAR,
        )
    }
}

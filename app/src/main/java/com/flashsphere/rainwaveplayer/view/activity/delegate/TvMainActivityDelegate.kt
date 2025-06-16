package com.flashsphere.rainwaveplayer.view.activity.delegate

import android.content.Intent
import android.os.Bundle
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.service.MediaService.Companion.getPlayFromStationIntent
import com.flashsphere.rainwaveplayer.ui.composition.LocalTvUiSettings
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.composition.LocalUserCredentials
import com.flashsphere.rainwaveplayer.ui.composition.TvUiSettings
import com.flashsphere.rainwaveplayer.ui.composition.UiScreenConfig
import com.flashsphere.rainwaveplayer.ui.drawer.DrawerItemHandler
import com.flashsphere.rainwaveplayer.ui.screen.tv.TvMainScreen
import com.flashsphere.rainwaveplayer.util.AnalyticsOnDestinationChangedListener
import com.flashsphere.rainwaveplayer.util.PreferencesKeys
import com.flashsphere.rainwaveplayer.util.getLastPlayedStation
import com.flashsphere.rainwaveplayer.view.activity.MainActivity
import com.flashsphere.rainwaveplayer.view.viewmodel.MainViewModel
import com.google.android.gms.cast.MediaError
import com.google.android.gms.cast.MediaError.DetailedErrorCode
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.tv.CastReceiverContext
import com.google.android.gms.cast.tv.SenderInfo
import com.google.android.gms.cast.tv.media.MediaException
import com.google.android.gms.cast.tv.media.MediaLoadCommandCallback
import com.google.android.gms.cast.tv.media.MediaManager
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.withContext
import timber.log.Timber

class TvMainActivityDelegate(
    private val activity: MainActivity,
    private val mainViewModel: MainViewModel,
    private val drawerItemHandler: DrawerItemHandler,
) : MainActivityDelegate, CastReceiverContext.EventCallback() {
    private var mediaManager: MediaManager? = null

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        registerCastSenderConnectedEventCallback()

        val analyticsOnDestinationChangedListener = AnalyticsOnDestinationChangedListener(activity.analytics)

        activity.setContent("TvMainScreen") {
            val navController = rememberNavController()
            DisposableEffect(navController) {
                navController.addOnDestinationChangedListener(analyticsOnDestinationChangedListener)
                onDispose { navController.removeOnDestinationChangedListener(analyticsOnDestinationChangedListener) }
            }

            val configuration = LocalConfiguration.current
            val windowSizeClass = calculateWindowSizeClass(activity)

            CompositionLocalProvider(
                LocalUiScreenConfig provides UiScreenConfig(configuration, windowSizeClass),
                LocalUserCredentials provides activity.userRepository.getCredentials(),
                LocalTvUiSettings provides TvUiSettings(activity.dataStore),
            ) {
                TvMainScreen(
                    navController = navController,
                    viewModel = mainViewModel,
                    drawerItemHandler = drawerItemHandler,
                )
            }
        }
    }

    override fun onDestroy() {
        mediaManager?.setMediaLoadCommandCallback(null)
        unregisterCastSenderConnectedEventCallback()
    }

    override fun onNewIntent(intent: Intent): Boolean {
        return mediaManager?.onNewIntent(intent) == true
    }

    override fun onStationsLoaded(stations: List<Station>) {
        if (mediaManager == null) {
            mediaManager = activity.castReceiverContextHolder.context?.mediaManager?.also { mediaManager ->
                setMediaLoadCommandCallback(mediaManager, stations)
                if (mediaManager.onNewIntent(activity.intent)) {
                    return
                }
            }
        }
        var currentStation = mainViewModel.station.value
        if (currentStation == null) {
            currentStation = activity.dataStore.getLastPlayedStation(stations)
            mainViewModel.station(currentStation)
        }
    }

    private fun setMediaLoadCommandCallback(mediaManager: MediaManager, stations: List<Station>) {
        mediaManager.setMediaLoadCommandCallback(object : MediaLoadCommandCallback() {
            override fun onLoad(senderId: String?, loadRequestData: MediaLoadRequestData): Task<MediaLoadRequestData?> {
                Timber.d("Running cast media load command")
                val stationId = getStationId(loadRequestData)
                if (stationId == null) {
                    throw MediaException(MediaError.Builder()
                        .setDetailedErrorCode(DetailedErrorCode.LOAD_FAILED)
                        .setReason(MediaError.ERROR_REASON_INVALID_PARAMS)
                        .build())
                }
                val station = stations.find { it.id == stationId }
                if (station == null) {
                    throw MediaException(MediaError.Builder()
                        .setDetailedErrorCode(DetailedErrorCode.LOAD_FAILED)
                        .setReason(MediaError.ERROR_REASON_INVALID_PARAMS)
                        .build())
                }

                mainViewModel.station(station)

                runCatching {
                    Timber.d("Starting playback for station id = %s", stationId)
                    val intent = getPlayFromStationIntent(activity, station)
                    ContextCompat.startForegroundService(activity, intent)
                }.onFailure {
                    throw MediaException(MediaError.Builder()
                        .setDetailedErrorCode(DetailedErrorCode.LOAD_FAILED)
                        .setReason(MediaError.ERROR_REASON_APP_ERROR)
                        .build())
                }.onSuccess {
                    mediaManager.setDataFromLoad(loadRequestData)
                    mediaManager.broadcastMediaStatus()
                }

                return Tasks.forResult(loadRequestData)
            }
        })
    }

    private fun getStationId(loadRequestData: MediaLoadRequestData): Int? {
        val mediaInfo = loadRequestData.mediaInfo
        if (mediaInfo == null) return null

        return runCatching { mediaInfo.contentId.toInt() }
            .recoverCatching { loadRequestData.customData!!.getInt("stationId") }
            .getOrNull()
    }

    private fun registerCastSenderConnectedEventCallback() {
        if (activity.userRepository.isLoggedIn()) return
        val receiverContext = activity.castReceiverContextHolder.context ?: return

        receiverContext.registerEventCallback(this)
    }

    private fun unregisterCastSenderConnectedEventCallback() {
        val receiverContext = activity.castReceiverContextHolder.context ?: return
        receiverContext.unregisterEventCallback(this)
    }

    override fun onSenderConnected(senderInfo: SenderInfo) {
        val jsonString = senderInfo.castLaunchRequest.credentialsData?.credentials
        if (jsonString.isNullOrBlank()) return

        activity.lifecycleScope.launchWithDefaults("Import TV Preferences") {
            withContext(activity.coroutineDispatchers.compute) {
                PreferencesKeys.importTvPreferences(activity.dataStore, activity.json, jsonString)
            }
            withContext(activity.coroutineDispatchers.main) {
                activity.stationRepository.clearCache()
                MainActivity.startActivity(activity)
                activity.finish()
            }
        }
    }
}

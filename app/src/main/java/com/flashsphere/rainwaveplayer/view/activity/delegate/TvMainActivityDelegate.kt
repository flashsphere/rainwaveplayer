package com.flashsphere.rainwaveplayer.view.activity.delegate

import android.content.Intent
import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.Surface
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.coroutine.suspendRunCatching
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.service.MediaService.Companion.getPlayFromStationIntent
import com.flashsphere.rainwaveplayer.ui.TvLoading
import com.flashsphere.rainwaveplayer.ui.composition.LocalTvUiSettings
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.composition.LocalUserCredentials
import com.flashsphere.rainwaveplayer.ui.composition.TvUiSettings
import com.flashsphere.rainwaveplayer.ui.composition.UiScreenConfig
import com.flashsphere.rainwaveplayer.ui.drawer.DrawerItemHandler
import com.flashsphere.rainwaveplayer.ui.screen.tv.TvMainScreen
import com.flashsphere.rainwaveplayer.ui.theme.tv.TvAppTheme
import com.flashsphere.rainwaveplayer.util.AnalyticsOnDestinationChangedListener
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
import com.google.android.gms.tasks.TaskCompletionSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

class TvMainActivityDelegate(
    private val activity: MainActivity,
    private val mainViewModel: MainViewModel,
    private val drawerItemHandler: DrawerItemHandler,
) : MainActivityDelegate, CastReceiverContext.EventCallback() {
    private val castReceiverContext = activity.castReceiverContextHolder.context
    private var mediaManager: MediaManager? = castReceiverContext?.mediaManager
        ?.also { setMediaLoadCommandCallback(it) }
    private val castCredentialsLoadingFlow = MutableStateFlow(false)

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

            val castCredentialsLoading = castCredentialsLoadingFlow.collectAsStateWithLifecycle().value
            if (castCredentialsLoading) {
                TvAppTheme {
                    Surface {
                        TvLoading()
                    }
                }
            } else {
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

        subscribeToMediaServiceConnection()
    }

    override fun onDestroy() {
        mediaManager?.setMediaLoadCommandCallback(null)
        unregisterCastSenderConnectedEventCallback()
    }

    override fun onNewIntent(intent: Intent): Boolean {
        return mediaManager?.onNewIntent(intent) == true
    }

    override fun onStationsLoaded(stations: List<Station>) {
        var currentStation = mainViewModel.station.value
        if (currentStation == null) {
            currentStation = activity.dataStore.getLastPlayedStation(stations)
            mainViewModel.station(currentStation)
        }
    }

    private fun setMediaLoadCommandCallback(mediaManager: MediaManager) {
        mediaManager.setMediaLoadCommandCallback(object : MediaLoadCommandCallback() {
            override fun onLoad(senderId: String?, loadRequestData: MediaLoadRequestData): Task<MediaLoadRequestData?> {
                val taskCompletionSource = TaskCompletionSource<MediaLoadRequestData>()

                activity.lifecycleScope.launchWithDefaults("Cast media load command") {
                    suspendRunCatching { onMediaLoad(loadRequestData) }
                        .onFailure {
                            if (it is Exception) {
                                taskCompletionSource.setException(it)
                            } else {
                                taskCompletionSource.setException(RuntimeException(it))
                            }
                        }
                        .onSuccess {
                            taskCompletionSource.setResult(it)
                        }
                }

                return taskCompletionSource.task
            }

            private suspend fun onMediaLoad(loadRequestData: MediaLoadRequestData): MediaLoadRequestData {
                Timber.d("Running cast media load command")
                val stationId = getStationId(loadRequestData)
                if (stationId == null) {
                    throw MediaException(MediaError.Builder()
                        .setDetailedErrorCode(DetailedErrorCode.LOAD_FAILED)
                        .setReason(MediaError.ERROR_REASON_INVALID_PARAMS)
                        .build())
                }
                val station = activity.stationRepository.getStations().find { it.id == stationId }
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

                return loadRequestData
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
        castReceiverContext?.registerEventCallback(this)
    }

    private fun unregisterCastSenderConnectedEventCallback() {
        castReceiverContext?.unregisterEventCallback(this)
    }

    override fun onSenderConnected(senderInfo: SenderInfo) {
        if (activity.userRepository.isLoggedIn()) return

        val jsonString = senderInfo.castLaunchRequest.credentialsData?.credentials
        if (jsonString.isNullOrBlank()) return

        castCredentialsLoadingFlow.value = true

        activity.lifecycleScope.launchWithDefaults("Import TV Preferences") {
            activity.castReceiverContextHolder.importTvPreferences(jsonString)
            activity.stationRepository.clearCache()

            mainViewModel.clearStationInfo()
            mainViewModel.getStations()
            castCredentialsLoadingFlow.value = false
        }
    }

    private fun subscribeToMediaServiceConnection() {
        activity.mediaServiceConnection.boundService
            .onEach { binder ->
                if (binder != null) {
                    val controller = binder.service.mediaSession.controller
                    MediaControllerCompat.setMediaController(activity, controller)
                } else {
                    MediaControllerCompat.setMediaController(activity, null)
                }
            }
            .launchWithDefaults(activity.lifecycleScope, "Media service connection in TV Main activity")
    }
}

package com.flashsphere.rainwaveplayer.view.activity.delegate

import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.lifecycleScope
import com.flashsphere.rainwaveplayer.cast.CastSessionManagerListener
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.service.MediaService
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiSettings
import com.flashsphere.rainwaveplayer.ui.composition.LocalUserCredentials
import com.flashsphere.rainwaveplayer.ui.composition.UiScreenConfig
import com.flashsphere.rainwaveplayer.ui.composition.UiSettings
import com.flashsphere.rainwaveplayer.ui.drawer.DrawerItemHandler
import com.flashsphere.rainwaveplayer.ui.navigation.Navigator
import com.flashsphere.rainwaveplayer.ui.navigation.NowPlaying
import com.flashsphere.rainwaveplayer.ui.navigation.rememberNavigationState
import com.flashsphere.rainwaveplayer.ui.navigation.topLevelRoutes
import com.flashsphere.rainwaveplayer.ui.screen.MainScreen
import com.flashsphere.rainwaveplayer.util.AnalyticsOnDestinationChangedListener
import com.flashsphere.rainwaveplayer.util.JobUtils.cancel
import com.flashsphere.rainwaveplayer.util.getBottomNavigationUiPref
import com.flashsphere.rainwaveplayer.util.getLastPlayedStation
import com.flashsphere.rainwaveplayer.util.isAutoPlayEnabled
import com.flashsphere.rainwaveplayer.util.isLoggedIn
import com.flashsphere.rainwaveplayer.view.activity.MainActivity
import com.flashsphere.rainwaveplayer.view.viewmodel.MainViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class NonTvMainActivityDelegate(
    private val activity: MainActivity,
    private val mainViewModel: MainViewModel,
    private val drawerItemHandler: DrawerItemHandler,
) : MainActivityDelegate {
    private var isFirstStart = false
    private var castStateJob: Job? = null
    private val sessionManagerListener: CastSessionManagerListener? by lazy {
        activity.castContextHolder.castContext?.let {
            CastSessionManagerListener(
                it, activity, activity.lifecycleScope,
                activity.stationRepository, activity.mediaPlayerStateObserver,
                mainViewModel.castState
            )
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        isFirstStart = (savedInstanceState == null)
        val analyticsOnDestinationChangedListener = AnalyticsOnDestinationChangedListener(activity.analytics)

        val userCredentials = activity.userRepository.getCredentials()?.also {
            activity.lifecycleScope.launchWithDefaults("Export TV Preferences") {
                activity.castContextHolder.setCastLaunchCredentials()
            }
        }

        activity.setContent("MainScreen") {
            val navigationState = rememberNavigationState(
                startRoute = NowPlaying,
                topLevelRoutes = topLevelRoutes
            )
            val navigator = remember { Navigator(navigationState) }

            DisposableEffect(navigator) {
                navigator.addOnDestinationChangedListener(analyticsOnDestinationChangedListener)
                onDispose { navigator.removeOnDestinationChangedListener(analyticsOnDestinationChangedListener) }
            }

            val configuration = LocalConfiguration.current
            val windowSizeClass = calculateWindowSizeClass(activity)
            val bottomNavUiPref = activity.dataStore.getBottomNavigationUiPref()
            val navSuiteType = if (userCredentials.isLoggedIn() && !bottomNavUiPref.isHidden()) {
                NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
            } else {
                NavigationSuiteType.None
            }

            CompositionLocalProvider(
                LocalUiScreenConfig provides UiScreenConfig(configuration, windowSizeClass),
                LocalUserCredentials provides userCredentials,
                LocalUiSettings provides UiSettings(navSuiteType, activity.dataStore),
            ) {
                MainScreen(
                    navigator = navigator,
                    mainViewModel = mainViewModel,
                    drawerItemHandler = drawerItemHandler,
                )
            }
        }

        subscribeToMediaServiceConnection()
    }

    override fun onStationsLoaded(stations: List<Station>) {
        var currentStation = mainViewModel.station.value
        if (currentStation == null) {
            currentStation = activity.dataStore.getLastPlayedStation(stations)
            mainViewModel.station(currentStation)
        }
        if (isFirstStart && activity.mediaPlayerStateObserver.currentState.isStopped() && activity.dataStore.isAutoPlayEnabled()) {
            activity.playbackManager.play(currentStation)
        }
        sessionManagerListener?.let {
            activity.lifecycle.addObserver(it)
        }
    }

    private fun subscribeToCastState(service: MediaService) {
        cancel(castStateJob)
        castStateJob = mainViewModel.castState
            .map { it.isNotEmpty() }
            .distinctUntilChanged()
            .onEach { castConnected ->
                if (castConnected) {
                    MediaControllerCompat.setMediaController(activity, null)
                } else {
                    val controller = service.mediaSession.controller
                    MediaControllerCompat.setMediaController(activity, controller)
                }
            }
            .launchWithDefaults(activity.lifecycleScope,"Cast State in Main activity")
    }

    private fun subscribeToMediaServiceConnection() {
        activity.mediaServiceConnection.boundService
            .onEach { service ->
                if (service != null) {
                    subscribeToCastState(service)
                } else {
                    cancel(castStateJob)
                    MediaControllerCompat.setMediaController(activity, null)
                }
            }
            .launchWithDefaults(activity.lifecycleScope, "Media service connection in Main activity")
    }
}

package com.flashsphere.rainwaveplayer.view.activity.delegate

import android.os.Bundle
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.flashsphere.rainwaveplayer.cast.CastSessionManagerListener
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiSettings
import com.flashsphere.rainwaveplayer.ui.composition.LocalUserCredentials
import com.flashsphere.rainwaveplayer.ui.composition.UiScreenConfig
import com.flashsphere.rainwaveplayer.ui.composition.UiSettings
import com.flashsphere.rainwaveplayer.ui.drawer.DrawerItemHandler
import com.flashsphere.rainwaveplayer.ui.screen.MainScreen
import com.flashsphere.rainwaveplayer.util.AnalyticsOnDestinationChangedListener
import com.flashsphere.rainwaveplayer.util.getBottomNavigationUiPref
import com.flashsphere.rainwaveplayer.util.getLastPlayedStation
import com.flashsphere.rainwaveplayer.util.isAutoPlayEnabled
import com.flashsphere.rainwaveplayer.util.isLoggedIn
import com.flashsphere.rainwaveplayer.view.activity.MainActivity
import com.flashsphere.rainwaveplayer.view.viewmodel.MainViewModel

class NonTvMainActivityDelegate(
    private val activity: MainActivity,
    private val mainViewModel: MainViewModel,
    private val drawerItemHandler: DrawerItemHandler,
) : MainActivityDelegate {
    private var isFirstStart = false
    private var sessionManagerListener: CastSessionManagerListener? = null

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
            val navController = rememberNavController()
            DisposableEffect(navController) {
                navController.addOnDestinationChangedListener(analyticsOnDestinationChangedListener)
                onDispose { navController.removeOnDestinationChangedListener(analyticsOnDestinationChangedListener) }
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
                    navController = navController,
                    mainViewModel = mainViewModel,
                    drawerItemHandler = drawerItemHandler,
                )
            }
        }
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

        if (sessionManagerListener == null) {
            activity.castContextHolder.castContext?.let {
                this.sessionManagerListener = CastSessionManagerListener(
                    it, activity, activity.lifecycleScope, stations, activity.mediaPlayerStateObserver,
                    mainViewModel.castState
                ).apply { activity.lifecycle.addObserver(this) }
            }
        }
    }
}

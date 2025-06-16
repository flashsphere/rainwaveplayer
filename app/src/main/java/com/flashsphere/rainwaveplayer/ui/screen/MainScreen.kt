package com.flashsphere.rainwaveplayer.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.ErrorWithRetry
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiSettings
import com.flashsphere.rainwaveplayer.ui.drawer.DrawerItemHandler
import com.flashsphere.rainwaveplayer.ui.drawer.NavigationDrawerContent
import com.flashsphere.rainwaveplayer.ui.navigation.BottomNavigation
import com.flashsphere.rainwaveplayer.ui.navigation.NavigationGraph
import com.flashsphere.rainwaveplayer.ui.navigation.NavigationSuiteScaffold
import com.flashsphere.rainwaveplayer.ui.navigation.SideNavigation
import com.flashsphere.rainwaveplayer.ui.navigation.drawerGesturesEnabledTopLevelRoutes
import com.flashsphere.rainwaveplayer.ui.sleeptimer.SleepTimerDialog
import com.flashsphere.rainwaveplayer.ui.theme.AppTheme
import com.flashsphere.rainwaveplayer.util.BottomNavPreference
import com.flashsphere.rainwaveplayer.view.viewmodel.MainViewModel

@Composable
fun MainScreen(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    drawerItemHandler: DrawerItemHandler,
) {
    AppTheme {
        val stationsScreenState = mainViewModel.stationsScreenState.collectAsStateWithLifecycle().value

        if (stationsScreenState.loading) {
            MainScreenLoading()
        } else if (stationsScreenState.error != null) {
            Surface {
                ErrorWithRetry(text = stringResource(R.string.error_connection),
                    onRetry = { mainViewModel.getStations() })
            }
        } else {
            MainScreenLoaded(
                navController = navController,
                mainViewModel = mainViewModel,
                drawerItemHandler = drawerItemHandler,
            )
        }
    }
}

@Composable
private fun MainScreenLoading() {
    Surface {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(Modifier.size(76.dp))
        }
    }
}

@Composable
private fun MainScreenLoaded(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    drawerItemHandler: DrawerItemHandler,
) {
    mainViewModel.station.collectAsStateWithLifecycle().value ?: return

    val scrollToTop = remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val gesturesEnabled = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        navController.currentBackStackEntryFlow
            .collect { navBackStackEntry ->
                val currentRoute = navBackStackEntry.destination
                gesturesEnabled.value = drawerGesturesEnabledTopLevelRoutes.any {
                    currentRoute.hasRoute(it.startDestination::class)
                }
            }
    }

    ModalNavigationDrawer(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled.value,
        drawerContent = {
            NavigationDrawerContent(
                drawerState = drawerState,
                stationsScreenState = mainViewModel.stationsScreenState,
                stationFlow = mainViewModel.station,
                userFlow = mainViewModel.user,
                drawerItemHandler = drawerItemHandler,
            )
        },
    ) {
        val navSuiteType = LocalUiSettings.current.navigationSuiteType
        val windowInsets = when (navSuiteType) {
            NavigationSuiteType.NavigationRail -> WindowInsets.systemBars.only(WindowInsetsSides.Bottom)
            NavigationSuiteType.NavigationBar -> WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
            else -> WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        }.union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))

        NavigationSuiteScaffold(
            contentWindowInsets = windowInsets,
            layoutType = navSuiteType,
            navigationSuite = {
                when (navSuiteType) {
                    NavigationSuiteType.NavigationRail -> {
                        SideNavigation(
                            navController = navController,
                            alwaysShowLabel = LocalUiSettings.current.bottomNavPreference == BottomNavPreference.Labeled,
                            scrollToTop = scrollToTop,
                            userFlow = mainViewModel.user,
                            onSleepTimerClick = drawerItemHandler.sleepTimerClick,
                            onSettingsClick = drawerItemHandler.settingsClick,
                        )
                    }
                    NavigationSuiteType.NavigationBar -> {
                        BottomNavigation(
                            navController = navController,
                            alwaysShowLabel = LocalUiSettings.current.bottomNavPreference == BottomNavPreference.Labeled,
                            scrollToTop = scrollToTop,
                        )
                    }
                    else -> {}
                }
            }
        ) {
            NavigationGraph(
                navController = navController,
                navSuiteType = navSuiteType,
                mainViewModel = mainViewModel,
                drawerState = drawerState,
                scrollToTop = scrollToTop,
            )
        }

        SleepTimerDialog(
            show = mainViewModel.showSleepTimer,
            getExistingSleepTimer = mainViewModel::getExistingSleepTimer,
            createSleepTimer = mainViewModel::createSleepTimer,
            removeSleepTimer = mainViewModel::removeSleepTimer,
        )
    }
}

@Preview
@Composable
private fun MainScreenLoadingPreview() {
    PreviewTheme { MainScreenLoading() }
}

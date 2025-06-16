package com.flashsphere.rainwaveplayer.ui.screen.tv

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ModalNavigationDrawer
import androidx.tv.material3.NavigationDrawerItemDefaults
import androidx.tv.material3.NavigationDrawerScope
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.tv.material3.rememberDrawerState
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.ui.CoilImage
import com.flashsphere.rainwaveplayer.ui.TvErrorWithRetry
import com.flashsphere.rainwaveplayer.ui.TvEventsSnackbarAsToast
import com.flashsphere.rainwaveplayer.ui.TvLoading
import com.flashsphere.rainwaveplayer.ui.alertdialog.TvCustomAlertDialog
import com.flashsphere.rainwaveplayer.ui.composition.LocalDrawerState
import com.flashsphere.rainwaveplayer.ui.composition.LocalUserCredentials
import com.flashsphere.rainwaveplayer.ui.drawer.DrawerItemHandler
import com.flashsphere.rainwaveplayer.ui.drawer.tv.TvNavigationDrawerItem
import com.flashsphere.rainwaveplayer.ui.item.tv.TvListItem
import com.flashsphere.rainwaveplayer.ui.navigation.NowPlayingRoute
import com.flashsphere.rainwaveplayer.ui.navigation.navigateToRoute
import com.flashsphere.rainwaveplayer.ui.navigation.topLevelRoutes
import com.flashsphere.rainwaveplayer.ui.navigation.tv.TvNavigationGraph
import com.flashsphere.rainwaveplayer.ui.theme.tv.TvAppTheme
import com.flashsphere.rainwaveplayer.util.isLoggedIn
import com.flashsphere.rainwaveplayer.view.activity.tv.TvWebViewActivity
import com.flashsphere.rainwaveplayer.view.uistate.model.UserState
import com.flashsphere.rainwaveplayer.view.viewmodel.MainViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

@Composable
fun TvMainScreen(
    navController: NavHostController,
    viewModel: MainViewModel,
    drawerItemHandler: DrawerItemHandler,
) {
    TvAppTheme {
        val stationsScreenState = viewModel.stationsScreenState.collectAsStateWithLifecycle().value

        Surface(modifier = Modifier.semantics { testTagsAsResourceId = true }) {
            if (stationsScreenState.loading) {
                TvLoading()
            } else if (stationsScreenState.error != null) {
                TvErrorWithRetry(text = stringResource(R.string.error_connection),
                    onRetry = { viewModel.getStations() })
            } else if (stationsScreenState.stations != null) {
                TvMainScreenLoaded(
                    navController = navController,
                    viewModel = viewModel,
                    stations = stationsScreenState.stations,
                    drawerItemHandler = drawerItemHandler,
                )
            }
            TvEventsSnackbarAsToast(events = viewModel.snackbarEvents)
        }
    }
}

@Composable
private fun TvMainScreenLoaded(
    navController: NavHostController,
    viewModel: MainViewModel,
    stations: SnapshotStateList<Station>,
    drawerItemHandler: DrawerItemHandler,
) {
    val station = viewModel.station.collectAsStateWithLifecycle().value ?: return

    val drawerState = rememberDrawerState(DrawerValue.Closed)

    CompositionLocalProvider (
        LocalDrawerState provides drawerState
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                NavigationDrawerContent(
                    navController = navController,
                    stations = stations,
                    selectedStation = station,
                    userFlow = viewModel.user,
                    drawerItemHandler = drawerItemHandler,
                    closeDrawer = { drawerState.setValue(DrawerValue.Closed) },
                )
            },
            scrimBrush = SolidColor(MaterialTheme.colorScheme.scrim.copy(alpha = 0.8f))
        ) {
            TvNavigationGraph(
                mainViewModel = viewModel,
                navController = navController,
            )
        }
    }
}

@Composable
private fun NavigationDrawerScope.NavigationDrawerContent(
    navController: NavHostController,
    stations: SnapshotStateList<Station>,
    selectedStation: Station,
    userFlow: StateFlow<UserState?>,
    closeDrawer: () -> Unit,
    drawerItemHandler: DrawerItemHandler,
) {
    val user = userFlow.collectAsStateWithLifecycle().value

    val context = LocalContext.current
    val userCredentials = LocalUserCredentials.current
    val routes = remember(userCredentials) {
        if (userCredentials.isLoggedIn()) {
            topLevelRoutes
        } else {
            listOf(NowPlayingRoute)
        }
    }
    val showStationDialog = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp, vertical = 12.dp)
            .focusGroup()
    ) {
        Column {
            if (user != null && !user.isAnon()) {
                TvNavigationDrawerItem(
                    leadingContent = {
                        CoilImage(
                            image = user.avatar,
                            contentScale = ContentScale.Fit,
                            fallback = painterResource(id = R.drawable.ic_account_circle_white_50dp),
                            error = painterResource(id = R.drawable.ic_account_circle_white_50dp),
                            placeholder = if (LocalInspectionMode.current) painterResource(R.drawable.ic_account_circle_white_50dp) else null,
                            modifier = Modifier.size(NavigationDrawerItemDefaults.IconSize).clip(CircleShape)
                        )
                    }
                ) {
                    Text(text = user.name)
                }
            } else if (!userCredentials.isLoggedIn() && hasFocus) {
                TvNavigationDrawerItem(
                    onClick = { TvWebViewActivity.startActivityForLogin(context) },
                ) { Text(text = stringResource(R.string.login)) }
            }
            if (hasFocus) {
                TvNavigationDrawerItem(
                    onClick = { showStationDialog.value = true },
                    supportingContent = {
                        Text(text = stringResource(id = R.string.tv_change_station))
                    }
                ) { Text(text = selectedStation.name) }
            }
        }

        Column(Modifier.align(Alignment.Center)) {
            routes.forEachIndexed { i, route ->
                key(i) {
                    var containsCurrentRoute by remember { mutableStateOf(false) }
                    var isRouteOnTop by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        navController.currentBackStackEntryFlow
                            .map { it.destination }
                            .collect { destination ->
                                containsCurrentRoute = destination.hierarchy.any { it.hasRoute(route::class) }
                                isRouteOnTop = destination.hasRoute(route.startDestination::class)
                            }
                    }

                    TvNavigationDrawerItem(
                        selected = containsCurrentRoute,
                        onClick = {
                            if (isRouteOnTop) {
                                closeDrawer()
                            } else if (containsCurrentRoute) {
                                navController.popBackStack(route.startDestination, false)
                            } else {
                                navController.navigateToRoute(route)
                            }
                        },
                        leadingContent = {
                            Icon(
                                painter = painterResource(route.icon),
                                contentDescription = stringResource(route.title),
                            )
                        }
                    ) {
                        Text(text = stringResource(route.title))
                    }
                }
            }
        }

        if (hasFocus) {
            Column(
                modifier = Modifier.align(Alignment.BottomStart),
            ) {
                TvNavigationDrawerItem(
                    onClick = drawerItemHandler.settingsClick,
                ) {
                    Text(text = stringResource(R.string.settings))
                }

                if (userCredentials.isLoggedIn()) {
                    TvNavigationDrawerItem(
                        onClick = drawerItemHandler.logoutClick,
                    ) {
                        Text(text = stringResource(R.string.logout))
                    }
                }
            }
        }
    }

    if (showStationDialog.value) {
        ChangeStationDialog(
            stations = stations,
            selectedStation = selectedStation,
            onStationSelected = {
                showStationDialog.value = false
                drawerItemHandler.stationClick(it)
                closeDrawer()
            },
            onDismissRequest = { showStationDialog.value = false }
        )
    }
}

@Composable
private fun ChangeStationDialog(
    stations: SnapshotStateList<Station>,
    selectedStation: Station,
    onStationSelected: (station: Station) -> Unit,
    onDismissRequest: () -> Unit,
) {
    TvCustomAlertDialog(
        modifier = Modifier.wrapContentSize(),
        onDismissRequest = onDismissRequest,
        content = {
            Column(
                modifier = Modifier.focusGroup().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                stations.forEachIndexed { i, station ->
                    key(i) {
                        val focusRequester = remember { FocusRequester() }
                        LaunchedEffect(selectedStation) {
                            if (selectedStation.id == station.id) {
                                focusRequester.requestFocus()
                            }
                        }
                        TvListItem(
                            modifier = Modifier.widthIn(max = 200.dp)
                                .focusRequester(focusRequester),
                            onClick = { onStationSelected(station) },
                            text = station.name,
                        )
                    }
                }
            }
        },
    )
}

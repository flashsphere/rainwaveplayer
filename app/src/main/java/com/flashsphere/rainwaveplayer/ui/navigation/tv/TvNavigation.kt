package com.flashsphere.rainwaveplayer.ui.navigation.tv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Down
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Up
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import androidx.tv.material3.DrawerValue
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.ui.composition.LastFocused
import com.flashsphere.rainwaveplayer.ui.composition.LocalDrawerState
import com.flashsphere.rainwaveplayer.ui.composition.LocalLastFocused
import com.flashsphere.rainwaveplayer.ui.navigation.AlbumDetail
import com.flashsphere.rainwaveplayer.ui.navigation.ArtistDetail
import com.flashsphere.rainwaveplayer.ui.navigation.CategoryDetail
import com.flashsphere.rainwaveplayer.ui.navigation.Library
import com.flashsphere.rainwaveplayer.ui.navigation.LibraryRoute
import com.flashsphere.rainwaveplayer.ui.navigation.NowPlaying
import com.flashsphere.rainwaveplayer.ui.navigation.NowPlayingRoute
import com.flashsphere.rainwaveplayer.ui.navigation.RequestsRoute
import com.flashsphere.rainwaveplayer.ui.navigation.Search
import com.flashsphere.rainwaveplayer.ui.navigation.SearchRoute
import com.flashsphere.rainwaveplayer.ui.screen.tv.TvAlbumDetailScreen
import com.flashsphere.rainwaveplayer.ui.screen.tv.TvArtistDetailScreen
import com.flashsphere.rainwaveplayer.ui.screen.tv.TvCategoryDetailScreen
import com.flashsphere.rainwaveplayer.ui.screen.tv.TvLibraryScreen
import com.flashsphere.rainwaveplayer.ui.screen.tv.TvRequestsScreen
import com.flashsphere.rainwaveplayer.ui.screen.tv.TvSearchScreen
import com.flashsphere.rainwaveplayer.ui.screen.tv.TvStationInfoScreen
import com.flashsphere.rainwaveplayer.view.viewmodel.AlbumScreenViewModel
import com.flashsphere.rainwaveplayer.view.viewmodel.ArtistScreenViewModel
import com.flashsphere.rainwaveplayer.view.viewmodel.CategoryScreenViewModel
import com.flashsphere.rainwaveplayer.view.viewmodel.LibraryScreenViewModel
import com.flashsphere.rainwaveplayer.view.viewmodel.MainViewModel
import com.flashsphere.rainwaveplayer.view.viewmodel.RequestsScreenViewModel
import com.flashsphere.rainwaveplayer.view.viewmodel.SearchScreenViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter

@Composable
fun TvNavigationGraph(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel,
    navController: NavHostController,
) {
    NavHost(
        modifier = modifier.fillMaxSize(),
        navController = navController,
        startDestination = NowPlayingRoute,
        enterTransition = { slideIntoContainer(Up, tween(300)) },
        exitTransition = { slideOutOfContainer(Up, tween(300)) },
        popEnterTransition = { slideIntoContainer(Down, tween(300)) },
        popExitTransition = { slideOutOfContainer(Down, tween(300)) },
    ) {
        navigation<NowPlayingRoute>(startDestination = NowPlaying) {
            composable<NowPlaying> {
                CompositionLocalProvider(
                    LocalLastFocused provides rememberSaveable { mutableStateOf(LastFocused()) }
                ) {
                    DrawerStateHandler()
                    TvStationInfoScreen(
                        navController = navController,
                        viewModel = mainViewModel,
                    )
                }
            }
        }
        composable<RequestsRoute> {
            CompositionLocalProvider(
                LocalLastFocused provides rememberSaveable { mutableStateOf(LastFocused()) }
            ) {
                DrawerStateHandler()
                TvRequestsScreen(
                    navController = navController,
                    viewModel = hiltViewModel<RequestsScreenViewModel>(),
                    stationFlow = mainViewModel.station,
                )
            }
        }
        navigation<LibraryRoute>(startDestination = Library) {
            composable<Library> {
                CompositionLocalProvider(
                    LocalLastFocused provides rememberSaveable { mutableStateOf(LastFocused()) }
                ) {
                    DrawerStateHandler()
                    TvLibraryScreen(
                        navController = navController,
                        viewModel = hiltViewModel<LibraryScreenViewModel>(),
                        stationFlow = mainViewModel.station,
                    )
                }
            }
            detailComposables(navController, mainViewModel.station)
        }
        navigation<SearchRoute>(startDestination = Search) {
            composable<Search> {
                CompositionLocalProvider(
                    LocalLastFocused provides rememberSaveable { mutableStateOf(LastFocused()) }
                ) {
                    DrawerStateHandler()
                    TvSearchScreen(
                        navController = navController,
                        viewModel = hiltViewModel<SearchScreenViewModel>(),
                        stationFlow = mainViewModel.station,
                    )
                }
            }
            detailComposables(navController, mainViewModel.station)
        }
    }
}

private fun NavGraphBuilder.detailComposables(
    navController: NavHostController,
    stationFlow: StateFlow<Station?>,
) {
    composable<AlbumDetail> { backStackEntry ->
        CompositionLocalProvider(
            LocalLastFocused provides rememberSaveable { mutableStateOf(LastFocused()) }
        ) {
            DrawerStateHandler()
            TvAlbumDetailScreen(
                navController = navController,
                viewModel = hiltViewModel<AlbumScreenViewModel>(),
                stationFlow = stationFlow,
                detail = backStackEntry.toRoute<AlbumDetail>(),
            )
        }
    }
    composable<ArtistDetail> { backStackEntry ->
        CompositionLocalProvider(
            LocalLastFocused provides rememberSaveable { mutableStateOf(LastFocused()) }
        ) {
            DrawerStateHandler()
            TvArtistDetailScreen(
                navController = navController,
                viewModel = hiltViewModel<ArtistScreenViewModel>(),
                stationFlow = stationFlow,
                detail = backStackEntry.toRoute<ArtistDetail>(),
            )
        }
    }
    composable<CategoryDetail> { backStackEntry ->
        CompositionLocalProvider(
            LocalLastFocused provides rememberSaveable { mutableStateOf(LastFocused()) }
        ) {
            DrawerStateHandler()
            TvCategoryDetailScreen(
                navController = navController,
                viewModel = hiltViewModel<CategoryScreenViewModel>(),
                stationFlow = stationFlow,
                detail = backStackEntry.toRoute<CategoryDetail>(),
            )
        }
    }
}

@Composable
private fun DrawerStateHandler() {
    val lastFocused = LocalLastFocused.current
    val drawerState = LocalDrawerState.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(drawerState) {
        snapshotFlow { drawerState.currentValue }
            .filter { it == DrawerValue.Closed }
            .collect { focusRequester.requestFocus() }
    }
    BackHandler(drawerState.currentValue == DrawerValue.Open) {
        focusRequester.requestFocus()
    }
    Spacer(Modifier
        .onKeyEvent {
            if (it.key == Key.DirectionLeft || it.key == Key.DirectionRight ||
                it.key == Key.DirectionUp || it.key == Key.DirectionDown) {
                focusManager.moveFocus(FocusDirection.Next)
            }
            return@onKeyEvent false
        }
        .focusRequester(focusRequester)
        .onFocusChanged {
            if (it.hasFocus || it.isFocused || it.isCaptured) {
                lastFocused.value = lastFocused.value.copy(shouldRequestFocus = true)
            }
        }
        .focusable())
}

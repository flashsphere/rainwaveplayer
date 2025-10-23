package com.flashsphere.rainwaveplayer.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Down
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.End
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Start
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Up
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType.Companion.NavigationRail
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.ui.screen.AlbumDetailScreen
import com.flashsphere.rainwaveplayer.ui.screen.ArtistDetailScreen
import com.flashsphere.rainwaveplayer.ui.screen.CategoryDetailScreen
import com.flashsphere.rainwaveplayer.ui.screen.LibraryScreen
import com.flashsphere.rainwaveplayer.ui.screen.RequestsScreen
import com.flashsphere.rainwaveplayer.ui.screen.SearchScreen
import com.flashsphere.rainwaveplayer.ui.screen.StationInfoScreen
import com.flashsphere.rainwaveplayer.view.viewmodel.AlbumScreenViewModel
import com.flashsphere.rainwaveplayer.view.viewmodel.ArtistScreenViewModel
import com.flashsphere.rainwaveplayer.view.viewmodel.CategoryScreenViewModel
import com.flashsphere.rainwaveplayer.view.viewmodel.LibraryScreenViewModel
import com.flashsphere.rainwaveplayer.view.viewmodel.MainViewModel
import com.flashsphere.rainwaveplayer.view.viewmodel.RequestsScreenViewModel
import com.flashsphere.rainwaveplayer.view.viewmodel.SearchScreenViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
fun NavigationGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    navSuiteType: NavigationSuiteType,
    mainViewModel: MainViewModel,
    drawerState: DrawerState,
    scrollToTop: MutableState<Boolean>,
) {
    val scope = rememberCoroutineScope()
    val openDrawer: () -> Unit = {
        scope.launch { drawerState.open() }
    }
    val closeDrawer: () -> Unit = {
        scope.launch { drawerState.close() }
    }

    val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = remember(navSuiteType) {
        if (navSuiteType == NavigationRail) {
            { slideIntoContainer(Up, tween(300)) }
        } else {
            { slideIntoContainer(Start, tween(300)) }
        }
    }
    val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = remember(navSuiteType) {
        if (navSuiteType == NavigationRail) {
            { slideOutOfContainer(Up, tween(300)) }
        } else {
            { slideOutOfContainer(Start, tween(300)) }
        }
    }
    val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = remember(navSuiteType) {
        if (navSuiteType == NavigationRail) {
            { slideIntoContainer(Down, tween(300)) }
        } else {
            { slideIntoContainer(End, tween(300)) }
        }
    }
    val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = remember(navSuiteType) {
        if (navSuiteType == NavigationRail) {
            { slideOutOfContainer(Down, tween(300)) }
        } else {
            { slideOutOfContainer(End, tween(300)) }
        }
    }

    NavHost(
        modifier = modifier.fillMaxSize(),
        navController = navController,
        startDestination = NowPlayingRoute,
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        popEnterTransition = popEnterTransition,
        popExitTransition = popExitTransition,
    ) {
        navigation<NowPlayingRoute>(startDestination = NowPlaying) {
            composable<NowPlaying> {
                StationInfoScreen(
                    navController = navController,
                    viewModel = mainViewModel,
                    scrollToTop = scrollToTop,
                    onMenuClick = openDrawer,
                )
                DrawerOpenBackHandler(drawerState, closeDrawer)
            }
            detailComposables(navController, mainViewModel.station)
        }
        composable<RequestsRoute> {
            RequestsScreen(
                navController = navController,
                viewModel = hiltViewModel<RequestsScreenViewModel>(),
                stationFlow = mainViewModel.station,
                onMenuClick = openDrawer,
                scrollToTop = scrollToTop,
            )
            DrawerOpenBackHandler(drawerState, closeDrawer)
        }
        navigation<LibraryRoute>(startDestination = Library) {
            composable<Library> {
                LibraryScreen(
                    navController = navController,
                    viewModel = hiltViewModel<LibraryScreenViewModel>(),
                    stationFlow = mainViewModel.station,
                    onMenuClick = openDrawer,
                    scrollToTop = scrollToTop,
                )
                DrawerOpenBackHandler(drawerState, closeDrawer)
            }
            detailComposables(navController, mainViewModel.station)
        }
        navigation<SearchRoute>(startDestination = Search) {
            composable<Search> {
                SearchScreen(
                    navController = navController,
                    viewModel = hiltViewModel<SearchScreenViewModel>(),
                    stationFlow = mainViewModel.station,
                    scrollToTop = scrollToTop,
                )
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
        AlbumDetailScreen(
            navController = navController,
            viewModel = hiltViewModel<AlbumScreenViewModel>(),
            stationFlow = stationFlow,
            albumDetail = backStackEntry.toRoute<AlbumDetail>(),
        )
    }

    composable<ArtistDetail> { backStackEntry ->
        val artistDetail = backStackEntry.toRoute<ArtistDetail>()
        val viewModel = hiltViewModel<ArtistScreenViewModel>()
        LaunchedEffect(artistDetail) {
            stationFlow.value?.let { viewModel.getArtist(it, artistDetail) }
        }
        ArtistDetailScreen(
            navController = navController,
            viewModel = viewModel,
        )
    }

    composable<CategoryDetail> { backStackEntry ->
        val categoryDetail = backStackEntry.toRoute<CategoryDetail>()
        val viewModel = hiltViewModel<CategoryScreenViewModel>()
        LaunchedEffect(categoryDetail) {
            stationFlow.value?.let { viewModel.getCategory(it, categoryDetail) }
        }
        CategoryDetailScreen(
            navController = navController,
            viewModel = viewModel,
        )
    }
}

fun NavHostController.navigateToRoute(route: Route) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

fun NavHostController.navigateToDetail(route: DetailRoute) {
    navigate(route)
}

@Composable
private fun DrawerOpenBackHandler(drawerState: DrawerState, closeDrawer: () -> Unit) {
    BackHandler(drawerState.currentValue == DrawerValue.Open) {
        closeDrawer()
    }
}

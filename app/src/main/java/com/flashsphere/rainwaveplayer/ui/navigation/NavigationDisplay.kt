package com.flashsphere.rainwaveplayer.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
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
fun NavigationDisplay(
    modifier: Modifier = Modifier,
    navigator: Navigator,
    navSuiteType: NavigationSuiteType,
    mainViewModel: MainViewModel,
    drawerState: DrawerState,
    scrollToTop: MutableState<Boolean>,
) {
    val scope = rememberCoroutineScope()
    val openDrawer: () -> Unit = remember(drawerState) {{
        scope.launch { drawerState.open() }
    }}
    val closeDrawer: () -> Unit = remember(drawerState) {{
        scope.launch { drawerState.close() }
    }}

    val entryProvider = entryProvider {
        nowPlayingSection(
            navigator = navigator,
            mainViewModel = mainViewModel,
            openDrawer = openDrawer,
            scrollToTop = scrollToTop,
        )
        requestsSection(
            navigator = navigator,
            mainViewModel = mainViewModel,
            openDrawer = openDrawer,
            scrollToTop = scrollToTop,
        )
        librarySection(
            navigator = navigator,
            mainViewModel = mainViewModel,
            openDrawer = openDrawer,
            scrollToTop = scrollToTop,
        )
        searchSection(
            navigator = navigator,
            mainViewModel = mainViewModel,
            scrollToTop = scrollToTop,
        )
        detailSections(
            navigator = navigator,
            stationFlow = mainViewModel.station,
        )
    }

    val transition = remember(navSuiteType) {
        if (navSuiteType == NavigationSuiteType.NavigationRail) {
            slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300)
            ) togetherWith slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(300)
            )
        } else {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            ) togetherWith slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(300)
            )
        }
    }

    val popTransition = remember(navSuiteType) {
        if (navSuiteType == NavigationSuiteType.NavigationRail) {
            slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(300)
            ) togetherWith slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            )
        } else {
            slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(300)
            ) togetherWith slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            )
        }
    }

    NavDisplay(
        modifier = modifier.fillMaxSize(),
        entries = navigator.state.toEntries(entryProvider),
        onBack = {
            if (drawerState.currentValue == DrawerValue.Open) {
                closeDrawer()
            } else {
                navigator.goBack()
            }
        },
        transitionSpec = { transition },
        popTransitionSpec = { popTransition },
        predictivePopTransitionSpec = { popTransition },
    )
}

private fun EntryProviderScope<NavKey>.nowPlayingSection(
    navigator: Navigator,
    mainViewModel: MainViewModel,
    openDrawer: () -> Unit,
    scrollToTop: MutableState<Boolean>,
) {
    entry<NowPlaying> {
        StationInfoScreen(
            navigator = navigator,
            viewModel = mainViewModel,
            scrollToTop = scrollToTop,
            onMenuClick = openDrawer,
        )
    }
}

private fun EntryProviderScope<NavKey>.requestsSection(
    navigator: Navigator,
    mainViewModel: MainViewModel,
    openDrawer: () -> Unit,
    scrollToTop: MutableState<Boolean>,
) {
    entry<Requests> {
        RequestsScreen(
            navigator = navigator,
            viewModel = hiltViewModel<RequestsScreenViewModel>(),
            stationFlow = mainViewModel.station,
            onMenuClick = openDrawer,
            scrollToTop = scrollToTop,
        )
    }
}

private fun EntryProviderScope<NavKey>.librarySection(
    navigator: Navigator,
    mainViewModel: MainViewModel,
    openDrawer: () -> Unit,
    scrollToTop: MutableState<Boolean>,
) {
    entry<Library> {
        LibraryScreen(
            navigator = navigator,
            viewModel = hiltViewModel<LibraryScreenViewModel>(),
            stationFlow = mainViewModel.station,
            onMenuClick = openDrawer,
            scrollToTop = scrollToTop,
        )
    }
}

private fun EntryProviderScope<NavKey>.searchSection(
    navigator: Navigator,
    mainViewModel: MainViewModel,
    scrollToTop: MutableState<Boolean>,
) {
    entry<Search> {
        SearchScreen(
            navigator = navigator,
            viewModel = hiltViewModel<SearchScreenViewModel>(),
            stationFlow = mainViewModel.station,
            scrollToTop = scrollToTop,
        )
    }
}

private fun EntryProviderScope<NavKey>.detailSections(
    navigator: Navigator,
    stationFlow: StateFlow<Station?>,
) {
    entry<AlbumDetail> { albumDetail ->
        AlbumDetailScreen(
            navigator = navigator,
            viewModel = hiltViewModel<AlbumScreenViewModel>(),
            stationFlow = stationFlow,
            albumDetail = albumDetail,
        )
    }

    entry<ArtistDetail> { artistDetail ->
        val viewModel = hiltViewModel<ArtistScreenViewModel>()
        LaunchedEffect(artistDetail) {
            stationFlow.value?.let { viewModel.getArtist(it, artistDetail) }
        }
        ArtistDetailScreen(navigator = navigator, viewModel = viewModel)
    }

    entry<CategoryDetail> { categoryDetail ->
        val viewModel = hiltViewModel<CategoryScreenViewModel>()
        LaunchedEffect(categoryDetail) {
            stationFlow.value?.let { viewModel.getCategory(it, categoryDetail) }
        }
        CategoryDetailScreen(navigator = navigator, viewModel = viewModel)
    }
}

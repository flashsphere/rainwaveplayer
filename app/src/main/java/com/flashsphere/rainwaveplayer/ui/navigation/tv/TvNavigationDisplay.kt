package com.flashsphere.rainwaveplayer.ui.navigation.tv

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.ui.composition.LastFocused
import com.flashsphere.rainwaveplayer.ui.composition.LocalLastFocused
import com.flashsphere.rainwaveplayer.ui.navigation.AlbumDetail
import com.flashsphere.rainwaveplayer.ui.navigation.ArtistDetail
import com.flashsphere.rainwaveplayer.ui.navigation.CategoryDetail
import com.flashsphere.rainwaveplayer.ui.navigation.Library
import com.flashsphere.rainwaveplayer.ui.navigation.Navigator
import com.flashsphere.rainwaveplayer.ui.navigation.NowPlaying
import com.flashsphere.rainwaveplayer.ui.navigation.Requests
import com.flashsphere.rainwaveplayer.ui.navigation.Search
import com.flashsphere.rainwaveplayer.ui.navigation.toEntries
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

@Composable
fun TvNavigationDisplay(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel,
    navigator: Navigator,
) {
    val entryProvider = entryProvider {
        nowPlayingSection(navigator, mainViewModel)
        requestsSection(navigator, mainViewModel)
        librarySection(navigator, mainViewModel)
        searchSection(navigator, mainViewModel)
        detailSections(navigator, mainViewModel.station)
    }

    val transition = remember {
        slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(300)
        ) togetherWith slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        )
    }

    val popTransition = remember {
        slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300)
        ) togetherWith slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300)
        )
    }

    NavDisplay(
        modifier = modifier.fillMaxSize(),
        entries = navigator.state.toEntries(entryProvider),
        onBack = { navigator.goBack() },
        transitionSpec = { transition },
        popTransitionSpec = { popTransition },
        predictivePopTransitionSpec = { popTransition },
    )
}

private fun EntryProviderScope<NavKey>.nowPlayingSection(
    navigator: Navigator,
    viewModel: MainViewModel,
) {
    entry<NowPlaying> {
        CompositionLocalProvider(
            LocalLastFocused provides rememberSaveable { mutableStateOf(LastFocused()) }
        ) {
            DrawerStateHandler()
            TvStationInfoScreen(navigator = navigator, viewModel = viewModel)
        }
    }
}

private fun EntryProviderScope<NavKey>.requestsSection(
    navigator: Navigator,
    viewModel: MainViewModel,
) {
    entry<Requests> {
        CompositionLocalProvider(
            LocalLastFocused provides rememberSaveable { mutableStateOf(LastFocused()) }
        ) {
            DrawerStateHandler()
            TvRequestsScreen(
                navigator = navigator,
                viewModel = hiltViewModel<RequestsScreenViewModel>(),
                stationFlow = viewModel.station,
            )
        }
    }
}

private fun EntryProviderScope<NavKey>.librarySection(
    navigator: Navigator,
    viewModel: MainViewModel,
) {
    entry<Library> {
        CompositionLocalProvider(
            LocalLastFocused provides rememberSaveable { mutableStateOf(LastFocused()) }
        ) {
            DrawerStateHandler()
            TvLibraryScreen(
                navigator = navigator,
                viewModel = hiltViewModel<LibraryScreenViewModel>(),
                stationFlow = viewModel.station,
            )
        }
    }
}

private fun EntryProviderScope<NavKey>.searchSection(
    navigator: Navigator,
    viewModel: MainViewModel,
) {
    entry<Search> {
        CompositionLocalProvider(
            LocalLastFocused provides rememberSaveable { mutableStateOf(LastFocused()) }
        ) {
            DrawerStateHandler()
            TvSearchScreen(
                navigator = navigator,
                viewModel = hiltViewModel<SearchScreenViewModel>(),
                stationFlow = viewModel.station,
            )
        }
    }
}

private fun EntryProviderScope<NavKey>.detailSections(
    navigator: Navigator,
    stationFlow: StateFlow<Station?>,
) {
    entry<AlbumDetail> { albumDetail ->
        CompositionLocalProvider(
            LocalLastFocused provides rememberSaveable { mutableStateOf(LastFocused()) }
        ) {
            DrawerStateHandler()
            TvAlbumDetailScreen(
                navigator = navigator,
                viewModel = hiltViewModel<AlbumScreenViewModel>(),
                stationFlow = stationFlow,
                detail = albumDetail,
            )
        }
    }
    entry<ArtistDetail> { artistDetail ->
        CompositionLocalProvider(
            LocalLastFocused provides rememberSaveable { mutableStateOf(LastFocused()) }
        ) {
            DrawerStateHandler()
            TvArtistDetailScreen(
                navigator = navigator,
                viewModel = hiltViewModel<ArtistScreenViewModel>(),
                stationFlow = stationFlow,
                detail = artistDetail,
            )
        }
    }
    entry<CategoryDetail> { categoryDetail ->
        CompositionLocalProvider(
            LocalLastFocused provides rememberSaveable { mutableStateOf(LastFocused()) }
        ) {
            DrawerStateHandler()
            TvCategoryDetailScreen(
                navigator = navigator,
                viewModel = hiltViewModel<CategoryScreenViewModel>(),
                stationFlow = stationFlow,
                detail = categoryDetail,
            )
        }
    }
}

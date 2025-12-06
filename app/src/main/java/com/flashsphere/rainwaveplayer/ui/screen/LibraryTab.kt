package com.flashsphere.rainwaveplayer.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.ui.AppError
import com.flashsphere.rainwaveplayer.ui.PullToRefreshBox
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiSettings
import com.flashsphere.rainwaveplayer.ui.item.AlbumItem
import com.flashsphere.rainwaveplayer.ui.item.LibraryItem
import com.flashsphere.rainwaveplayer.ui.item.RequestLineItem
import com.flashsphere.rainwaveplayer.ui.navigation.AlbumDetail
import com.flashsphere.rainwaveplayer.ui.navigation.ArtistDetail
import com.flashsphere.rainwaveplayer.ui.navigation.CategoryDetail
import com.flashsphere.rainwaveplayer.ui.navigation.Navigator
import com.flashsphere.rainwaveplayer.ui.scrollToItem
import com.flashsphere.rainwaveplayer.util.JobUtils.cancel
import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.view.uistate.LibraryScreenState
import com.flashsphere.rainwaveplayer.view.uistate.model.AlbumState
import com.flashsphere.rainwaveplayer.view.uistate.model.ArtistState
import com.flashsphere.rainwaveplayer.view.uistate.model.CategoryState
import com.flashsphere.rainwaveplayer.view.uistate.model.LibraryItem
import com.flashsphere.rainwaveplayer.view.uistate.model.RequestLineState
import com.flashsphere.rainwaveplayer.view.viewmodel.LibraryScreenViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter

@Composable
fun AlbumLibraryTab(
    navigator: Navigator,
    station: Station,
    viewModel: LibraryScreenViewModel,
    scrollToTop: MutableState<Boolean>,
) {
    val job = remember { mutableStateOf<Job?>(null) }

    LifecycleStartEffect(Unit) {
        viewModel.subscribeStateChangeEvents()
        onStopOrDispose { viewModel.unsubscribeStateChangeEvents() }
    }
    LifecycleStartEffect(station) {
        job.value = viewModel.getAllAlbums(station, false)
        onStopOrDispose { cancel(job.value) }
    }
    LibraryTab(libraryStateFlow = viewModel.albumLibraryState,
        scrollToTop = scrollToTop,
        onRefresh = { job.value = viewModel.getAllAlbums(station, true) },
        itemContent = { i, item: AlbumState ->
            AlbumItem(
                album = item,
                showGlobalRating = !LocalUiSettings.current.hideRatingsUntilRated,
                onClick = { album ->
                    navigator.navigate(AlbumDetail(album.id, album.name))
                },
                onFaveClick = { album -> viewModel.faveAlbum(station, album) },
            )
        })
}

@Composable
fun ArtistLibraryTab(
    navigator: Navigator,
    station: Station,
    viewModel: LibraryScreenViewModel,
    scrollToTop: MutableState<Boolean>,
) {
    val job = remember { mutableStateOf<Job?>(null) }

    LifecycleStartEffect(station) {
        job.value = viewModel.getAllArtists(station, false)
        onStopOrDispose { cancel(job.value) }
    }
    LibraryTab(libraryStateFlow = viewModel.artistLibraryState,
        scrollToTop = scrollToTop,
        onRefresh = { job.value = viewModel.getAllArtists(station, true) },
        itemContent = { _, item: ArtistState ->
            LibraryItem(title = item.name, onClick = {
                navigator.navigate(ArtistDetail(item.id, item.name))
            })
        })
}

@Composable
fun CategoryLibraryTab(
    navigator: Navigator,
    station: Station,
    viewModel: LibraryScreenViewModel,
    scrollToTop: MutableState<Boolean>,
) {
    val job = remember { mutableStateOf<Job?>(null) }

    LifecycleStartEffect(station) {
        job.value = viewModel.getAllCategories(station, false)
        onStopOrDispose { cancel(job.value) }
    }
    LibraryTab(libraryStateFlow = viewModel.categoryLibraryState,
        scrollToTop = scrollToTop,
        onRefresh = { job.value = viewModel.getAllCategories(station, true) },
        itemContent = { _, item: CategoryState ->
            LibraryItem(title = item.name, onClick = {
                navigator.navigate(CategoryDetail(item.id, item.name))
            })
        })
}

@Composable
fun RequestLineLibraryTab(
    station: Station,
    viewModel: LibraryScreenViewModel,
    scrollToTop: MutableState<Boolean>,
) {
    LifecycleStartEffect(station) {
        viewModel.subscribeStationInfo(station, false)
        onStopOrDispose { viewModel.unsubscribeStationInfo() }
    }
    LibraryTab(libraryStateFlow = viewModel.requestLineLibraryState,
        scrollToTop = scrollToTop,
        onRefresh = { viewModel.subscribeStationInfo(station, true) },
        itemContent = { _, item: RequestLineState ->
            RequestLineItem(item)
        })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T : LibraryItem> LibraryTab(libraryStateFlow: StateFlow<LibraryScreenState<T>>,
                                 scrollToTop: MutableState<Boolean>,
                                 onRefresh: () -> Unit,
                                 itemContent: (@Composable (i: Int, item: T) -> Unit)) {
    val libraryState = libraryStateFlow.collectAsStateWithLifecycle().value

    PullToRefreshBox(modifier = Modifier.fillMaxSize(),
        isRefreshing = libraryState.loading,
        onRefresh = onRefresh,
        contentAlignment = Alignment.TopCenter,
    ) {
        libraryState.filteredData?.let { data ->
            if (data.isEmpty()) {
                NoResults()
            }
            LibraryList(items = data, scrollToTop = scrollToTop, itemContent = itemContent)
        }

        AppError(error = libraryState.error, onRetry = onRefresh)
    }
}

@Composable
private fun <T : LibraryItem> LibraryList(
    items: SnapshotStateList<T>,
    scrollToTop: MutableState<Boolean>,
    itemContent: @Composable (i: Int, item: T) -> Unit
) {
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val gridColumnCount = LocalUiScreenConfig.current.gridSpan

    LaunchedEffect(scrollToTop) {
        snapshotFlow { scrollToTop.value }
            .filter { it }
            .collect {
                scrollToTop.value = false
                scrollToItem(scope, gridState, 0)
            }
    }

    LazyVerticalGrid(modifier = Modifier.fillMaxSize(),
        state = gridState,
        columns = GridCells.Fixed(gridColumnCount),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        itemsIndexed(items = items, key = { _: Int, item: T -> item.id }) { i: Int, item: T ->
            Box(modifier = Modifier.animateItem()) {
                itemContent(i, item)
            }
        }
    }
}

@Composable
private fun NoResults() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = stringResource(id = R.string.no_results),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp))
    }
}

@Preview
@Composable
private fun LibraryTabPreview(
    @PreviewParameter(LibraryPreviewProvider::class) data: LibraryScreenState<ArtistState>
) {
    PreviewTheme {
        Surface {
            LibraryTab(
                libraryStateFlow = remember { MutableStateFlow(data) },
                scrollToTop = remember { mutableStateOf(false) },
                onRefresh = { }
            ) { _, item ->
                LibraryItem(title = item.name, onClick = { })
            }
        }
    }
}

private class LibraryPreviewProvider :
    PreviewParameterProvider<LibraryScreenState<ArtistState>> {
    override val values: Sequence<LibraryScreenState<ArtistState>> = sequenceOf(
        mutableStateListOf(
            ArtistState(
                id = 1,
                name = "19's Sound Factory",
                items = mutableStateListOf()
            ),
            ArtistState(
                id = 2,
                name = "Kristofer Maddigan",
                items = mutableStateListOf()
            ),
            ArtistState(
                id = 3,
                name = "Artist name Artist name Artist name Artist name Artist name Artist name",
                items = mutableStateListOf()
            ),
        ).let { LibraryScreenState.loaded(stations[0], it, it) },
        LibraryScreenState.loaded(stations[0], mutableStateListOf(), mutableStateListOf()),
        LibraryScreenState.error(
            LibraryScreenState(),
            OperationError(OperationError.Server)
        )
    )
}

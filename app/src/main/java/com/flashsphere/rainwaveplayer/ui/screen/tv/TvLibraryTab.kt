package com.flashsphere.rainwaveplayer.ui.screen.tv

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.ui.TvErrorWithRetry
import com.flashsphere.rainwaveplayer.ui.TvLoading
import com.flashsphere.rainwaveplayer.ui.composition.LocalTvUiSettings
import com.flashsphere.rainwaveplayer.ui.item.tv.TvAlbumListItem
import com.flashsphere.rainwaveplayer.ui.item.tv.TvListItem
import com.flashsphere.rainwaveplayer.ui.item.tv.TvRequestLineListItem
import com.flashsphere.rainwaveplayer.ui.navigation.AlbumDetail
import com.flashsphere.rainwaveplayer.ui.navigation.ArtistDetail
import com.flashsphere.rainwaveplayer.ui.navigation.CategoryDetail
import com.flashsphere.rainwaveplayer.ui.navigation.navigateToDetail
import com.flashsphere.rainwaveplayer.ui.saveLastFocused
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTv
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTvTheme
import com.flashsphere.rainwaveplayer.ui.screen.stations
import com.flashsphere.rainwaveplayer.util.JobUtils.cancel
import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.view.uistate.LibraryScreenState
import com.flashsphere.rainwaveplayer.view.uistate.model.ArtistState
import com.flashsphere.rainwaveplayer.view.uistate.model.LibraryItem
import com.flashsphere.rainwaveplayer.view.viewmodel.LibraryScreenViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun TvAlbumLibraryTab(
    navController: NavHostController,
    viewModel: LibraryScreenViewModel,
    station: Station,
) {
    var job by remember { mutableStateOf<Job?>(null) }
    LifecycleStartEffect(Unit) {
        viewModel.subscribeStateChangeEvents()
        onStopOrDispose { viewModel.unsubscribeStateChangeEvents() }
    }
    LifecycleStartEffect(station) {
        job = viewModel.getAllAlbums(station, false)
        onStopOrDispose { cancel(job) }
    }
    TvLibraryTab(
        libraryStateFlow = viewModel.albumLibraryState,
        onRetry = { job = viewModel.getAllAlbums(station, true) }
    ) { modifier, _, item ->
        TvAlbumListItem(
            modifier = modifier.saveLastFocused(item.key),
            album = item,
            showGlobalRating = !LocalTvUiSettings.current.hideRatingsUntilRated,
            onClick = { navController.navigateToDetail(AlbumDetail(item.id, item.name)) },
        )
    }
}

@Composable
fun TvArtistLibraryTab(
    navController: NavHostController,
    viewModel: LibraryScreenViewModel,
    station: Station,
) {
    var job by remember { mutableStateOf<Job?>(null) }
    LifecycleStartEffect(station) {
        job = viewModel.getAllArtists(station, false)
        onStopOrDispose { cancel(job) }
    }
    TvLibraryTab(
        libraryStateFlow = viewModel.artistLibraryState,
        onRetry = { job = viewModel.getAllArtists(station, true) }
    ) { modifier, _, item ->
        TvListItem(
            modifier = modifier.fillMaxWidth().saveLastFocused(item.key),
            text = item.name,
            onClick = { navController.navigateToDetail(ArtistDetail(item.id, item.name)) },
        )
    }
}

@Composable
fun TvCategoryLibraryTab(
    navController: NavHostController,
    viewModel: LibraryScreenViewModel,
    station: Station,
) {
    var job by remember { mutableStateOf<Job?>(null) }
    LifecycleStartEffect(station) {
        job = viewModel.getAllCategories(station, false)
        onStopOrDispose { cancel(job) }
    }
    TvLibraryTab(
        libraryStateFlow = viewModel.categoryLibraryState,
        onRetry = { job = viewModel.getAllCategories(station, true) }
    ) { modifier, _, item ->
        TvListItem(
            modifier = modifier.fillMaxWidth().saveLastFocused(item.key),
            text = item.name,
            onClick = { navController.navigateToDetail(CategoryDetail(item.id, item.name)) },
        )
    }
}

@Composable
fun TvRequestLineLibraryTab(
    viewModel: LibraryScreenViewModel,
    station: Station,
) {
    LifecycleStartEffect(Unit) {
        viewModel.subscribeStateChangeEvents()
        onStopOrDispose { viewModel.unsubscribeStateChangeEvents() }
    }
    LifecycleStartEffect(station) {
        viewModel.subscribeStationInfo(station, false)
        onStopOrDispose { viewModel.unsubscribeStationInfo() }
    }
    TvLibraryTab(
        libraryStateFlow = viewModel.requestLineLibraryState,
        onRetry = { viewModel.subscribeStationInfo(station, true) }
    ) { _, _, item ->
        TvRequestLineListItem(item = item)
    }
}

@Composable
fun <T: LibraryItem> TvLibraryTab(
    libraryStateFlow: StateFlow<LibraryScreenState<T>>,
    onRetry: () -> Unit,
    itemContent: @Composable (modifier: Modifier, i: Int, item: T) -> Unit,
) {
    val screenState = libraryStateFlow.collectAsStateWithLifecycle().value

    if (screenState.loading) {
        TvLoading()
    } else if (screenState.error != null) {
        val message = screenState.error.getMessage(LocalContext.current, stringResource(R.string.error_connection))
        TvErrorWithRetry(
            text = message,
            onRetry = onRetry,
        )
    } else if (screenState.filteredData != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            if (screenState.filteredData.isEmpty()) {
                Text(text = stringResource(id = R.string.no_results),
                    modifier = Modifier.padding(top = 20.dp))
            }
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize().focusGroup(),
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
            ) {
                itemsIndexed(
                    items = screenState.filteredData,
                    key = { _, item -> item.id },
                ) { i, item ->
                    itemContent(Modifier.animateItem(), i, item)
                }
            }
        }
    }
}

private class LibraryTabPreviewProvider : PreviewParameterProvider<LibraryScreenState<ArtistState>> {
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

@PreviewTv
@Composable
private fun LibraryTabPreview(@PreviewParameter(LibraryTabPreviewProvider::class) data: LibraryScreenState<ArtistState>
) {
    PreviewTvTheme {
        Surface {
            TvLibraryTab(
                libraryStateFlow = remember { MutableStateFlow(data) },
                onRetry = { },
            ) { modifier, _, item ->
                TvListItem(
                    modifier = modifier.fillMaxWidth().saveLastFocused(item.key),
                    text = item.name,
                    onClick = {},
                )
            }
        }
    }
}

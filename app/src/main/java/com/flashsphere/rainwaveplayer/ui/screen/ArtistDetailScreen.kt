package com.flashsphere.rainwaveplayer.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.AppBarTitle
import com.flashsphere.rainwaveplayer.ui.AppError
import com.flashsphere.rainwaveplayer.ui.AppScaffold
import com.flashsphere.rainwaveplayer.ui.BackIcon
import com.flashsphere.rainwaveplayer.ui.PullToRefreshBox
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiSettings
import com.flashsphere.rainwaveplayer.ui.enterAlwaysScrollBehavior
import com.flashsphere.rainwaveplayer.ui.item.AlbumHeaderItem
import com.flashsphere.rainwaveplayer.ui.item.LibrarySongItem
import com.flashsphere.rainwaveplayer.ui.itemSpan
import com.flashsphere.rainwaveplayer.ui.itemsSpan
import com.flashsphere.rainwaveplayer.ui.navigation.AlbumDetail
import com.flashsphere.rainwaveplayer.ui.navigation.Navigator
import com.flashsphere.rainwaveplayer.ui.scrollToItem
import com.flashsphere.rainwaveplayer.ui.theme.AppTypography
import com.flashsphere.rainwaveplayer.util.ClassUtils.getSimpleClassName
import com.flashsphere.rainwaveplayer.view.uistate.ArtistScreenState
import com.flashsphere.rainwaveplayer.view.uistate.event.SnackbarEvent
import com.flashsphere.rainwaveplayer.view.uistate.model.AlbumState
import com.flashsphere.rainwaveplayer.view.uistate.model.ArtistAlbumState
import com.flashsphere.rainwaveplayer.view.uistate.model.ArtistState
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState
import com.flashsphere.rainwaveplayer.view.viewmodel.ArtistScreenViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Composable
fun ArtistDetailScreen(
    navigator: Navigator,
    viewModel: ArtistScreenViewModel,
) {
    LifecycleStartEffect(Unit) {
        viewModel.subscribeStateChangeEvents()
        onStopOrDispose { viewModel.unsubscribeStateChangeEvents() }
    }

    ArtistDetailScreen(
        artistScreenStateFlow = viewModel.artistScreenState,
        events = viewModel.snackbarEvents,
        onSongClick = { song -> viewModel.requestSong(song) },
        onFaveSongClick = { song -> viewModel.faveSong(song) },
        onRefresh = { viewModel.getArtist() },
        onBackClick = { navigator.goBack() },
        onAlbumClick = { navigator.navigate(AlbumDetail(it.id, it.name)) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtistDetailScreen(
    artistScreenStateFlow: StateFlow<ArtistScreenState>,
    events: Flow<SnackbarEvent>,
    onSongClick: (song: SongState) -> Unit,
    onFaveSongClick: (song: SongState) -> Unit,
    onRefresh: () -> Unit,
    onBackClick: () -> Unit,
    onAlbumClick: (album: AlbumState) -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollToTop = remember { mutableStateOf(false) }
    val artistScreenState = artistScreenStateFlow.collectAsStateWithLifecycle().value

    AppScaffold(
        navigationIcon = {
            BackIcon(onBackClick)
        },
        appBarContent = {
            AppBarTitle(
                title = artistScreenState.artist?.name ?: "",
                onClick = { scope.launch { scrollToTop.value = true } },
            )
        },
        appBarScrollBehavior = enterAlwaysScrollBehavior(scrollToTop),
        snackbarEvents = events,
    ) {
        PullToRefreshBox(modifier = Modifier.fillMaxSize(),
            isRefreshing = artistScreenState.loading,
            onRefresh = onRefresh,
            contentAlignment = Alignment.TopCenter) {
            if (artistScreenState.loaded) {
                artistScreenState.artist?.let {
                    ArtistDetailList(
                        artist = it,
                        onAlbumClick = onAlbumClick,
                        onSongClick = onSongClick,
                        onFaveSongClick = onFaveSongClick,
                        scrollToTop = scrollToTop,
                    )
                }
            }
        }

        ArtistDetailError(
            artistScreenState = artistScreenState,
            snackbarHostState = snackbarHostState,
            onRetry = onRefresh
        )
    }
}

@Composable
fun ArtistDetailList(
    artist: ArtistState,
    onAlbumClick: (album: AlbumState) -> Unit,
    onSongClick: (song: SongState) -> Unit,
    onFaveSongClick: (song: SongState) -> Unit,
    scrollToTop: MutableState<Boolean>,
) {
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val listItemPadding = LocalUiScreenConfig.current.listItemPadding
    val itemPadding = LocalUiScreenConfig.current.itemPadding
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
        item(span = itemSpan(gridColumnCount), key = artist.key) {
            Text(text = artist.name,
                style = AppTypography.titleMedium,
                modifier = Modifier
                    .animateItem()
                    .padding(horizontal = listItemPadding, vertical = itemPadding))
        }

        itemsIndexed(items = artist.items,
            key = { _, item -> item.key },
            contentType = { _, item -> item.getSimpleClassName() },
            span = itemsSpan(gridColumnCount) { _, item ->
                if (item is ArtistAlbumState)
                    GridItemSpan(gridColumnCount)
                else
                    GridItemSpan(1)
            }
        ) { _, item ->
            when (item) {
                is ArtistAlbumState -> {
                    val albumName = if (item.showStationName) {
                        stringResource(R.string.on_station, item.stationName, item.albumName)
                    } else {
                        item.albumName
                    }
                    val album = remember(item) { AlbumState(item.albumId, albumName) }
                    Box(modifier = Modifier.animateItem()) {
                        AlbumHeaderItem(album = album,
                            onClick = if (item.showStationName) null else onAlbumClick)
                    }
                }
                is SongState -> {
                    Box(modifier = Modifier.animateItem()) {
                        LibrarySongItem(song = item,
                            showGlobalRating = !LocalUiSettings.current.hideRatingsUntilRated,
                            onClick = onSongClick,
                            onFaveClick = onFaveSongClick)
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun ArtistDetailError(artistScreenState: ArtistScreenState,
                              snackbarHostState: SnackbarHostState,
                              onRetry: () -> Unit) {
    if (artistScreenState.error == null) return
    AppError(
        showAsSnackbar = artistScreenState.loaded,
        snackbarHostState = snackbarHostState,
        error = artistScreenState.error,
        onRetry = onRetry
    )
}

private class ArtistScreenPreviewProvider : PreviewParameterProvider<ArtistState> {
    override val values: Sequence<ArtistState> = sequenceOf(
        artistStateData[0],
    )
}

@Preview
@Composable
private fun ArtistDetailListPreview(
    @PreviewParameter(ArtistScreenPreviewProvider::class) artist: ArtistState,
) {
    PreviewTheme {
        ArtistDetailScreen(
            artistScreenStateFlow = remember { MutableStateFlow(ArtistScreenState.loaded(artist)) },
            events = remember { MutableSharedFlow() },
            onSongClick = {},
            onFaveSongClick = {},
            onRefresh = {},
            onBackClick = {},
            onAlbumClick = {})
    }
}

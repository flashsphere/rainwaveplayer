package com.flashsphere.rainwaveplayer.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.ui.AppError
import com.flashsphere.rainwaveplayer.ui.AppScaffold
import com.flashsphere.rainwaveplayer.ui.BackIcon
import com.flashsphere.rainwaveplayer.ui.PullToRefreshBox
import com.flashsphere.rainwaveplayer.ui.SearchTextField
import com.flashsphere.rainwaveplayer.ui.SearchTextFieldLabel
import com.flashsphere.rainwaveplayer.ui.appbar.AppBarActions
import com.flashsphere.rainwaveplayer.ui.appbar.toAppBarAction
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiSettings
import com.flashsphere.rainwaveplayer.ui.item.AlbumItem
import com.flashsphere.rainwaveplayer.ui.item.LibraryItem
import com.flashsphere.rainwaveplayer.ui.item.LibrarySongItem
import com.flashsphere.rainwaveplayer.ui.itemsSpan
import com.flashsphere.rainwaveplayer.ui.navigation.AlbumDetail
import com.flashsphere.rainwaveplayer.ui.navigation.ArtistDetail
import com.flashsphere.rainwaveplayer.ui.navigation.LibraryRoute
import com.flashsphere.rainwaveplayer.ui.navigation.NowPlayingRoute
import com.flashsphere.rainwaveplayer.ui.navigation.RequestsRoute
import com.flashsphere.rainwaveplayer.ui.navigation.navigateToDetail
import com.flashsphere.rainwaveplayer.ui.scrollToItem
import com.flashsphere.rainwaveplayer.ui.theme.AppTypography
import com.flashsphere.rainwaveplayer.util.ClassUtils.getSimpleClassName
import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.view.uistate.SearchScreenState
import com.flashsphere.rainwaveplayer.view.uistate.event.SnackbarEvent
import com.flashsphere.rainwaveplayer.view.uistate.model.AlbumSearchHeaderItem
import com.flashsphere.rainwaveplayer.view.uistate.model.AlbumState
import com.flashsphere.rainwaveplayer.view.uistate.model.ArtistSearchHeaderItem
import com.flashsphere.rainwaveplayer.view.uistate.model.ArtistState
import com.flashsphere.rainwaveplayer.view.uistate.model.SearchHeaderItem
import com.flashsphere.rainwaveplayer.view.uistate.model.SearchItem
import com.flashsphere.rainwaveplayer.view.uistate.model.SearchResult
import com.flashsphere.rainwaveplayer.view.uistate.model.SearchSongItem
import com.flashsphere.rainwaveplayer.view.uistate.model.SongSearchHeaderItem
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState
import com.flashsphere.rainwaveplayer.view.viewmodel.SearchScreenViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter

@Composable
fun SearchScreen(
    navController: NavHostController,
    viewModel: SearchScreenViewModel,
    stationFlow: StateFlow<Station?>,
    scrollToTop: MutableState<Boolean>,
) {
    val station = stationFlow.collectAsStateWithLifecycle().value ?: return

    LaunchedEffect(station) {
        viewModel.station(station)
    }

    LifecycleStartEffect(Unit) {
        viewModel.subscribeStateChangeEvents()
        onStopOrDispose { viewModel.unsubscribeStateChangeEvents() }
    }

    SearchScreen(
        navController = navController,
        searchTextFieldState = viewModel.searchTextFieldState,
        screenStateFlow = viewModel.searchState,
        search = { viewModel.search() },
        events = viewModel.snackbarEvents,
        onAlbumClick = { album ->
            navController.navigateToDetail(AlbumDetail(album.id, album.name))
        },
        onFaveAlbumClick = { album -> viewModel.faveAlbum(album) },
        onArtistClick = { artist ->
            navController.navigateToDetail(ArtistDetail(artist.id, artist.name))
        },
        onSongClick = { song -> viewModel.requestSong(song) },
        onFaveSongClick = { song -> viewModel.faveSong(song) },
        onBackClick = { navController.popBackStack() },
        scrollToTop = scrollToTop,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreen(
    navController: NavHostController,
    searchTextFieldState: TextFieldState,
    screenStateFlow: StateFlow<SearchScreenState>,
    search: () -> Unit,
    events: Flow<SnackbarEvent>,
    onAlbumClick: (album: AlbumState) -> Unit,
    onFaveAlbumClick: (album: AlbumState) -> Unit,
    onArtistClick: (artist: ArtistState) -> Unit,
    onSongClick: (song: SongState) -> Unit,
    onFaveSongClick: (song: SongState) -> Unit,
    onBackClick: () -> Unit,
    scrollToTop: MutableState<Boolean>,
) {
    AppScaffold(
        navigationIcon = {
            BackIcon(onBackClick = onBackClick)
        },
        appBarContent = {
            SearchTextField(
                state = searchTextFieldState,
                onSubmit = { search() },
                label = {
                    SearchTextFieldLabel(
                        painter = rememberVectorPainter(Icons.Filled.Search),
                        text = stringResource(id = R.string.action_search),
                    )
                },
            )
        },
        appBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
        appBarActions = {
            if (LocalUiSettings.current.navigationSuiteType == NavigationSuiteType.None) {
                AppBarActions(overflowActions = toAppBarAction(navController,
                    listOf(NowPlayingRoute, RequestsRoute, LibraryRoute)))
            }
        },
        snackbarEvents = events,
    ) {
        val screenState = screenStateFlow.collectAsStateWithLifecycle().value
        PullToRefreshBox(modifier = Modifier.fillMaxSize(),
            isRefreshing = screenState.loading,
            onRefresh = search,
            contentAlignment = Alignment.TopCenter) {
            if (screenState.loaded) {
                val items = screenState.result?.items
                if (items.isNullOrEmpty()) {
                    val message = screenState.result?.message ?: stringResource(id = R.string.no_results)
                    Text(text = message,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 20.dp))
                } else {
                    SearchResults(
                        items = items,
                        onAlbumClick = onAlbumClick,
                        onFaveAlbumClick = onFaveAlbumClick,
                        onArtistClick = onArtistClick,
                        onSongClick = onSongClick,
                        onFaveSongClick = onFaveSongClick,
                        scrollToTop = scrollToTop
                    )
                }
            }
        }

        AppError(error = screenState.error, onRetry = search)
    }
}

@Composable
private fun SearchResults(
    items: SnapshotStateList<SearchItem>,
    onAlbumClick: (album: AlbumState) -> Unit,
    onFaveAlbumClick: (album: AlbumState) -> Unit,
    onArtistClick: (artist: ArtistState) -> Unit,
    onSongClick: (song: SongState) -> Unit,
    onFaveSongClick: (song: SongState) -> Unit,
    scrollToTop: MutableState<Boolean>
) {
    val scope = rememberCoroutineScope()
    val gridColumnCount = LocalUiScreenConfig.current.gridSpan
    val gridState = rememberLazyGridState()

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
        contentPadding = PaddingValues(bottom = 80.dp),
    ) {
        itemsIndexed(items = items,
            key = { _, item -> item.key },
            contentType = { _, item -> item.getSimpleClassName() },
            span = itemsSpan(gridColumnCount) { _, item ->
                if (item is SearchHeaderItem) GridItemSpan(gridColumnCount) else GridItemSpan(1)
            }
        ) { _, item ->
            when (item) {
                is ArtistSearchHeaderItem -> {
                    SearchHeaderItem(modifier = Modifier.animateItem(),
                        title = stringResource(id = R.string.artists))
                }
                is AlbumSearchHeaderItem -> {
                    SearchHeaderItem(modifier = Modifier.animateItem(),
                        title = stringResource(id = R.string.albums))
                }
                is SongSearchHeaderItem -> {
                    SearchHeaderItem(modifier = Modifier.animateItem(),
                        title = stringResource(id = R.string.songs))
                }
                is AlbumState -> {
                    AlbumItem(
                        modifier = Modifier.animateItem(),
                        album = item,
                        showGlobalRating = !LocalUiSettings.current.hideRatingsUntilRated,
                        onClick = onAlbumClick,
                        onFaveClick = onFaveAlbumClick,
                    )
                }
                is ArtistState -> {
                    LibraryItem(modifier = Modifier.animateItem(), title = item.name,
                        onClick = { onArtistClick(item) })
                }
                is SearchSongItem -> {
                    SearchSong(modifier = Modifier.animateItem(),
                        song = item.song,
                        showGlobalRating = !LocalUiSettings.current.hideRatingsUntilRated,
                        onClick = onSongClick,
                        onFaveClick = onFaveSongClick)
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun SearchHeaderItem(modifier: Modifier = Modifier, title: String) {
    val itemPadding = LocalUiScreenConfig.current.itemPadding
    Column(modifier = modifier.padding(top = 16.dp)) {
        Text(text = title.uppercase(), style = AppTypography.bodyMedium,
            modifier = Modifier.padding(start = itemPadding, end = itemPadding, bottom = itemPadding))
        HorizontalDivider()
    }
}

@Composable
fun SearchSong(
    modifier: Modifier = Modifier,
    song: SongState,
    showGlobalRating: Boolean,
    onClick: ((item: SongState) -> Unit)?,
    onFaveClick: (item: SongState) -> Unit,
) {
    LibrarySongItem(modifier = modifier, showGlobalRating = showGlobalRating, song = song, onClick = onClick, onFaveClick = onFaveClick) {
        Text(
            text = song.title,
            style = AppTypography.bodyLarge,
            lineHeight = LocalUiScreenConfig.current.songItemTitleLineHeight,
        )
        Text(
            text = song.albumName,
            style = AppTypography.bodySmall,
            lineHeight = LocalUiScreenConfig.current.songItemArtistLineHeight,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Preview
@Composable
private fun SearchScreenPreview(
    @PreviewParameter(SearchScreenPreviewProvider::class) searchState: SearchScreenState
) {
    val context = LocalContext.current
    val navHostController = remember { NavHostController(context) }
    val textFieldState = rememberTextFieldState("some query")
    val searchScreenState  = remember { MutableStateFlow(searchState) }
    val events  = remember { MutableSharedFlow<SnackbarEvent>() }
    PreviewTheme {
        SearchScreen(
            navController = navHostController,
            searchTextFieldState = textFieldState,
            screenStateFlow = searchScreenState,
            search = {},
            events = events,
            onAlbumClick = {},
            onFaveAlbumClick = {},
            onArtistClick = {},
            onSongClick = {},
            onFaveSongClick = {},
            onBackClick = {},
            scrollToTop = remember { mutableStateOf(false) },
        )
    }
}

@Preview
@Composable
private fun SearchHeaderItemPreview() {
    PreviewTheme {
        Surface {
            SearchHeaderItem(title = "some title")
        }
    }
}

private class SearchScreenPreviewProvider : PreviewParameterProvider<SearchScreenState> {
    override val values: Sequence<SearchScreenState> = sequenceOf(SearchScreenState(
        loaded = true,
        result = SearchResult(message = "Some message")
    ), SearchScreenState(
        error = OperationError(OperationError.Server)
    ), SearchScreenState(
        loaded = true,
        result = SearchResult(items = mutableStateListOf(
            ArtistSearchHeaderItem,
            ArtistState(id = 1, name = "19's Sound Factory"),
            ArtistState(id = 2, name = "Kristofer Maddigan"),
            AlbumSearchHeaderItem,
            albumStateData[0],
            albumStateData[1],
            SongSearchHeaderItem,
            SearchSongItem(songStateData[0]),
            SearchSongItem(songStateData[1]),
        ))
    ))
}

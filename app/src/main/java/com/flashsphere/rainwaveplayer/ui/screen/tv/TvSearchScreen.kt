package com.flashsphere.rainwaveplayer.ui.screen.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.ui.SearchTextField
import com.flashsphere.rainwaveplayer.ui.TvLoading
import com.flashsphere.rainwaveplayer.ui.composition.LastFocused
import com.flashsphere.rainwaveplayer.ui.composition.LocalLastFocused
import com.flashsphere.rainwaveplayer.ui.composition.LocalTvUiSettings
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.item.tv.TvAlbumListItem
import com.flashsphere.rainwaveplayer.ui.item.tv.TvListItem
import com.flashsphere.rainwaveplayer.ui.item.tv.TvSongListItem
import com.flashsphere.rainwaveplayer.ui.itemsSpan
import com.flashsphere.rainwaveplayer.ui.navigation.AlbumDetail
import com.flashsphere.rainwaveplayer.ui.navigation.ArtistDetail
import com.flashsphere.rainwaveplayer.ui.navigation.navigateToDetail
import com.flashsphere.rainwaveplayer.ui.saveLastFocused
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTv
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTvTheme
import com.flashsphere.rainwaveplayer.ui.screen.albumStateData
import com.flashsphere.rainwaveplayer.ui.screen.songStateData
import com.flashsphere.rainwaveplayer.ui.theme.tv.TvAppTypography
import com.flashsphere.rainwaveplayer.util.ClassUtils.getSimpleClassName
import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.view.uistate.SearchScreenState
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun TvSearchScreen(
    navController: NavHostController,
    viewModel: SearchScreenViewModel,
    stationFlow: StateFlow<Station?>,
) {
    val station = stationFlow.collectAsStateWithLifecycle().value ?: return

    LaunchedEffect(station) {
        viewModel.station(station)
    }

    LifecycleStartEffect(Unit) {
        viewModel.subscribeStateChangeEvents()
        onStopOrDispose { viewModel.unsubscribeStateChangeEvents() }
    }

    TvSearchScreen(
        searchTextFieldState = viewModel.searchTextFieldState,
        screenStateFlow = viewModel.searchState,
        search = { viewModel.search() },
        onAlbumClick = { album ->
            navController.navigateToDetail(AlbumDetail(album.id, album.name))
        },
        onArtistClick = { artist ->
            navController.navigateToDetail(ArtistDetail(artist.id, artist.name))
        },
        onSongClick = { song -> viewModel.requestSong(song) },
        onFaveSongClick = { song -> viewModel.faveSong(song) },
    )
}


@Composable
private fun TvSearchScreen(
    searchTextFieldState: TextFieldState,
    screenStateFlow: StateFlow<SearchScreenState>,
    search: () -> Unit,
    onAlbumClick: (album: AlbumState) -> Unit,
    onArtistClick: (artist: ArtistState) -> Unit,
    onSongClick: (song: SongState) -> Unit,
    onFaveSongClick: (song: SongState) -> Unit,
) {
    val lastFocused = LocalLastFocused.current
    LifecycleStartEffect(Unit) {
        if (lastFocused.value.tag == null) {
            lastFocused.value = LastFocused("search_field", true)
        } else {
            lastFocused.value = lastFocused.value.copy(shouldRequestFocus = true)
        }
        onStopOrDispose {}
    }

    val screenState = screenStateFlow.collectAsStateWithLifecycle().value

    Surface {
        Column(modifier = Modifier.imePadding().fillMaxSize().padding(start = 80.dp, end = 40.dp)) {
            TvSearchTextField(
                textFieldState = searchTextFieldState,
                search = search,
            )
            if (screenState.loading) {
                TvLoading()
            } else if (screenState.loaded) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    val items = screenState.result?.items
                    if (items.isNullOrEmpty()) {
                        val message = screenState.result?.message ?: stringResource(id = R.string.no_results)
                        Text(text = message, modifier = Modifier.padding(top = 20.dp))
                    } else {
                        SearchResults(
                            items = items,
                            onAlbumClick = onAlbumClick,
                            onArtistClick = onArtistClick,
                            onSongClick = onSongClick,
                            onFaveSongClick = onFaveSongClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvSearchTextField(
    textFieldState: TextFieldState,
    search: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val hasFocus = remember { mutableStateOf(false) }

    BackHandler(textFieldState.text.isNotEmpty() && !hasFocus.value) {
        focusRequester.requestFocus()
    }

    SearchTextField(
        modifier = Modifier.saveLastFocused("search_field", focusRequester)
            .onFocusChanged {
                hasFocus.value = it.hasFocus || it.isFocused || it.isCaptured
            },
        state = textFieldState,
        clearFocusOnKeyboardHide = false,
        onSubmit = { search() },
        clearIcon = null,
    )
}

private const val gridColumnCount = 2

@Composable
private fun SearchResults(
    items: SnapshotStateList<SearchItem>,
    onAlbumClick: (album: AlbumState) -> Unit,
    onArtistClick: (artist: ArtistState) -> Unit,
    onSongClick: (song: SongState) -> Unit,
    onFaveSongClick: (song: SongState) -> Unit,
) {
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize().focusGroup(),
        columns = GridCells.Fixed(gridColumnCount),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
    ) {
        itemsIndexed(
            items = items,
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
                    TvAlbumListItem(
                        modifier = Modifier.animateItem().saveLastFocused(item.key),
                        album = item,
                        showGlobalRating = !LocalTvUiSettings.current.hideRatingsUntilRated,
                        onClick = { onAlbumClick(item) },
                    )
                }
                is ArtistState -> {
                    TvListItem(
                        modifier = Modifier.animateItem().saveLastFocused(item.key),
                        text = item.name, onClick = { onArtistClick(item) })
                }
                is SearchSongItem -> {
                    TvSongListItem(
                        songItemModifier = Modifier.animateItem().saveLastFocused(item.key),
                        song = item.song,
                        onClick = { onSongClick(item.song) },
                        onFaveClick = { onFaveSongClick(item.song) },
                    ) {
                        Text(
                            text = item.song.title,
                            style = TvAppTypography.bodyLarge,
                            lineHeight = TvAppTypography.bodyLarge.fontSize,
                        )
                        Text(
                            text = item.song.albumName,
                            style = TvAppTypography.bodySmall,
                            lineHeight = TvAppTypography.bodySmall.fontSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
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
        Text(text = title.uppercase(), style = TvAppTypography.bodyMedium,
            modifier = Modifier.padding(start = itemPadding, end = itemPadding, bottom = itemPadding))
        HorizontalDivider()
    }
}

@PreviewTv
@Composable
private fun SearchHeaderItemPreview() {
    PreviewTvTheme {
        Surface {
            SearchHeaderItem(title = "some title")
        }
    }
}

private class TvSearchScreenPreviewProvider : PreviewParameterProvider<SearchScreenState> {
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

@PreviewTv
@Composable
private fun TvSearchScreenPreview(
    @PreviewParameter(TvSearchScreenPreviewProvider::class) searchState: SearchScreenState
) {
    val textFieldState = rememberTextFieldState("some query")
    val searchScreenState  = remember { MutableStateFlow(searchState) }
    PreviewTvTheme {
        Surface {
            TvSearchScreen(
                searchTextFieldState = textFieldState,
                screenStateFlow = searchScreenState,
                search = {},
                onAlbumClick = {},
                onArtistClick = {},
                onSongClick = {},
                onFaveSongClick = {},
            )
        }
    }
}

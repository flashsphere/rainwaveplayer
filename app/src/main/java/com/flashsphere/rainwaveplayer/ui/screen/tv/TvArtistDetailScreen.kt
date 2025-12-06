package com.flashsphere.rainwaveplayer.ui.screen.tv

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.ui.TvErrorWithRetry
import com.flashsphere.rainwaveplayer.ui.TvLoading
import com.flashsphere.rainwaveplayer.ui.composition.LastFocused
import com.flashsphere.rainwaveplayer.ui.composition.LocalLastFocused
import com.flashsphere.rainwaveplayer.ui.composition.LocalTvUiSettings
import com.flashsphere.rainwaveplayer.ui.item.tv.TvAlbumHeaderItem
import com.flashsphere.rainwaveplayer.ui.item.tv.TvSongListItem
import com.flashsphere.rainwaveplayer.ui.navigation.AlbumDetail
import com.flashsphere.rainwaveplayer.ui.navigation.ArtistDetail
import com.flashsphere.rainwaveplayer.ui.navigation.Navigator
import com.flashsphere.rainwaveplayer.ui.saveLastFocused
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTv
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTvTheme
import com.flashsphere.rainwaveplayer.ui.screen.artistStateData
import com.flashsphere.rainwaveplayer.ui.theme.tv.TvAppTypography
import com.flashsphere.rainwaveplayer.util.ClassUtils.getSimpleClassName
import com.flashsphere.rainwaveplayer.view.uistate.model.AlbumState
import com.flashsphere.rainwaveplayer.view.uistate.model.ArtistAlbumState
import com.flashsphere.rainwaveplayer.view.uistate.model.ArtistState
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState
import com.flashsphere.rainwaveplayer.view.viewmodel.ArtistScreenViewModel
import kotlinx.coroutines.flow.StateFlow

@Composable
fun TvArtistDetailScreen(
    navigator: Navigator,
    viewModel: ArtistScreenViewModel,
    stationFlow: StateFlow<Station?>,
    detail: ArtistDetail,
) {
    val station = stationFlow.collectAsStateWithLifecycle().value ?: return
    LaunchedEffect(detail) {
        viewModel.getArtist(station, detail)
    }
    LifecycleStartEffect(Unit) {
        viewModel.subscribeStateChangeEvents()
        onStopOrDispose { viewModel.unsubscribeStateChangeEvents() }
    }

    val screenState = viewModel.artistScreenState.collectAsStateWithLifecycle().value
    Surface {
        if (screenState.loading) {
            TvLoading()
        } else if (screenState.error != null) {
            TvErrorWithRetry(
                text = screenState.error.getMessage(LocalContext.current, stringResource(R.string.error_connection)),
                onRetry = { viewModel.getArtist() },
            )
        } else if (screenState.loaded && screenState.artist != null) {
            TvArtistDetailScreen(
                artist = screenState.artist,
                onAlbumClick = { navigator.navigate(AlbumDetail(it.id, it.name)) },
                onSongClick = { song -> viewModel.requestSong(song) },
                onFaveSongClick = { song -> viewModel.faveSong(song) },
            )
        }
    }
}

private const val gridColumnCount = 2

@Composable
private fun TvArtistDetailScreen(
    artist: ArtistState,
    onAlbumClick: (album: AlbumState) -> Unit,
    onSongClick: (song: SongState) -> Unit,
    onFaveSongClick: (song: SongState) -> Unit,
) {
    val lastFocused = LocalLastFocused.current
    LifecycleStartEffect(artist) {
        val items = artist.items
        if (!items.isEmpty() && lastFocused.value.tag == null) {
            lastFocused.value = LastFocused(items[0].key, true)
        } else if (!lastFocused.value.shouldRequestFocus) {
            lastFocused.value = lastFocused.value.copy(shouldRequestFocus = true)
        }
        onStopOrDispose {}
    }

    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize().focusGroup(),
        columns = GridCells.Fixed(gridColumnCount),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(start = 80.dp, end = 40.dp, top = 20.dp, bottom = 20.dp),
    ) {
        item(
            span = { GridItemSpan(gridColumnCount) },
            key = artist.key,
        ) {
            Text(
                modifier = Modifier.padding(bottom = 8.dp),
                text = artist.name,
                style = TvAppTypography.titleLarge.copy(
                    lineHeight = TvAppTypography.titleLarge.lineHeight
                ),
            )
        }

        itemsIndexed(
            items = artist.items,
            key = { _, item -> item.key },
            contentType = { _, item -> item.getSimpleClassName() },
            span = { _, item ->
                if (item is ArtistAlbumState)
                    GridItemSpan(gridColumnCount)
                else
                    GridItemSpan(1)
            },
        ) { _, item ->
            when (item) {
                is ArtistAlbumState -> {
                    val albumName = if (item.showStationName) {
                        stringResource(R.string.on_station, item.stationName, item.albumName)
                    } else {
                        item.albumName
                    }
                    TvAlbumHeaderItem(
                        modifier = Modifier.animateItem().saveLastFocused(item.key),
                        album = remember(item) { AlbumState(item.albumId, albumName) },
                        onClick = if (item.showStationName) null else onAlbumClick
                    )
                }
                is SongState -> {
                    TvSongListItem(
                        modifier = Modifier.animateItem().saveLastFocused(item.key),
                        song = item,
                        showGlobalRating = !LocalTvUiSettings.current.hideRatingsUntilRated,
                        onClick = { onSongClick(item) },
                        onFaveClick = { onFaveSongClick(item) },
                    )
                }
                else -> {}
            }
        }
    }
}

private class TvArtistDetailScreenPreviewProvider : PreviewParameterProvider<ArtistState> {
    override val values: Sequence<ArtistState> = sequenceOf(
        artistStateData[0]
    )
}

@PreviewTv
@Composable
private fun TvArtistDetailScreenPreview(@PreviewParameter(TvArtistDetailScreenPreviewProvider::class) artist: ArtistState) {
    PreviewTvTheme {
        Surface {
            TvArtistDetailScreen(
                artist = artist,
                onAlbumClick = {},
                onSongClick = {},
                onFaveSongClick = {},
            )
        }
    }
}

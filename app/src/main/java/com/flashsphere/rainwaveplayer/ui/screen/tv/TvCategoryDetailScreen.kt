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
import com.flashsphere.rainwaveplayer.ui.navigation.CategoryDetail
import com.flashsphere.rainwaveplayer.ui.navigation.Navigator
import com.flashsphere.rainwaveplayer.ui.saveLastFocused
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTv
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTvTheme
import com.flashsphere.rainwaveplayer.ui.screen.categoryStateData
import com.flashsphere.rainwaveplayer.ui.theme.tv.TvAppTypography
import com.flashsphere.rainwaveplayer.util.ClassUtils.getSimpleClassName
import com.flashsphere.rainwaveplayer.view.uistate.model.AlbumState
import com.flashsphere.rainwaveplayer.view.uistate.model.CategoryState
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState
import com.flashsphere.rainwaveplayer.view.viewmodel.CategoryScreenViewModel
import kotlinx.coroutines.flow.StateFlow

@Composable
fun TvCategoryDetailScreen(
    navigator: Navigator,
    viewModel: CategoryScreenViewModel,
    stationFlow: StateFlow<Station?>,
    detail: CategoryDetail,
) {
    val station = stationFlow.collectAsStateWithLifecycle().value ?: return
    LaunchedEffect(detail) {
        viewModel.getCategory(station, detail)
    }
    LifecycleStartEffect(Unit) {
        viewModel.subscribeStateChangeEvents()
        onStopOrDispose { viewModel.unsubscribeStateChangeEvents() }
    }

    val screenState = viewModel.categoryScreenState.collectAsStateWithLifecycle().value
    Surface {
        if (screenState.loading) {
            TvLoading()
        } else if (screenState.error != null) {
            TvErrorWithRetry(
                text = screenState.error.getMessage(LocalContext.current, stringResource(R.string.error_connection)),
                onRetry = { viewModel.getCategory() },
            )
        } else if (screenState.loaded && screenState.category != null) {
            TvCategoryDetailScreen(
                category = screenState.category,
                onAlbumClick = { navigator.navigate(AlbumDetail(it.id, it.name)) },
                onSongClick = { song -> viewModel.requestSong(song) },
                onFaveSongClick = { song -> viewModel.faveSong(song) },
            )
        }
    }
}

private const val gridColumnCount = 2

@Composable
private fun TvCategoryDetailScreen(
    category: CategoryState,
    onAlbumClick: (album: AlbumState) -> Unit,
    onSongClick: (song: SongState) -> Unit,
    onFaveSongClick: (song: SongState) -> Unit,
) {
    val lastFocused = LocalLastFocused.current
    LifecycleStartEffect(category) {
        val items = category.items
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
            key = category.key,
        ) {
            Text(
                modifier = Modifier.padding(bottom = 8.dp),
                text = category.name,
                style = TvAppTypography.titleLarge.copy(
                    lineHeight = TvAppTypography.titleLarge.lineHeight
                ),
            )
        }

        itemsIndexed(
            items = category.items,
            key = { _, item -> item.key },
            contentType = { _, item -> item.getSimpleClassName() },
            span = { _, item ->
                if (item is AlbumState)
                    GridItemSpan(gridColumnCount)
                else
                    GridItemSpan(1)
            },
        ) { i, item ->
            when (item) {
                is AlbumState -> {
                    TvAlbumHeaderItem(
                        modifier = Modifier.animateItem().saveLastFocused(item.key),
                        album = item,
                        onClick = { onAlbumClick(item) },
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

private class TvCategoryDetailScreenPreviewProvider : PreviewParameterProvider<CategoryState> {
    override val values: Sequence<CategoryState> = sequenceOf(
        categoryStateData[0],
    )
}

@PreviewTv
@Composable
private fun TvCategoryDetailScreenPreview(@PreviewParameter(TvCategoryDetailScreenPreviewProvider::class) category: CategoryState) {
    PreviewTvTheme {
        Surface {
            TvCategoryDetailScreen(
                category = category,
                onAlbumClick = {},
                onSongClick = {},
                onFaveSongClick = {},
            )
        }
    }
}

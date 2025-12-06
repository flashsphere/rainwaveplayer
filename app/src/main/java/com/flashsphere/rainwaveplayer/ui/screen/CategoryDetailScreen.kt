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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.flashsphere.rainwaveplayer.view.uistate.CategoryScreenState
import com.flashsphere.rainwaveplayer.view.uistate.event.SnackbarEvent
import com.flashsphere.rainwaveplayer.view.uistate.model.AlbumState
import com.flashsphere.rainwaveplayer.view.uistate.model.CategoryState
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState
import com.flashsphere.rainwaveplayer.view.viewmodel.CategoryScreenViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Composable
fun CategoryDetailScreen(
    navigator: Navigator,
    viewModel: CategoryScreenViewModel,
) {
    LifecycleStartEffect(Unit) {
        viewModel.subscribeStateChangeEvents()
        onStopOrDispose { viewModel.unsubscribeStateChangeEvents() }
    }

    CategoryDetailScreen(
        categoryScreenStateFlow = viewModel.categoryScreenState,
        events = viewModel.snackbarEvents,
        onSongClick = { song -> viewModel.requestSong(song) },
        onFaveSongClick = { song -> viewModel.faveSong(song) },
        onRefresh = { viewModel.getCategory() },
        onBackClick = { navigator.goBack() },
        onAlbumClick = { navigator.navigate(AlbumDetail(it.id, it.name)) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDetailScreen(
    categoryScreenStateFlow: StateFlow<CategoryScreenState>,
    events: Flow<SnackbarEvent>,
    onSongClick: (song: SongState) -> Unit,
    onFaveSongClick: (song: SongState) -> Unit,
    onRefresh: () -> Unit,
    onBackClick: () -> Unit,
    onAlbumClick: (album: AlbumState) -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollToTop = remember { mutableStateOf(false) }
    val categoryScreenState = categoryScreenStateFlow.collectAsStateWithLifecycle().value

    AppScaffold(
        navigationIcon = {
            BackIcon(onBackClick)
        },
        appBarContent = {
            AppBarTitle(
                title = categoryScreenState.category?.name ?: "",
                onClick = { scope.launch { scrollToTop.value = true } },
            )
        },
        appBarScrollBehavior = enterAlwaysScrollBehavior(scrollToTop),
        snackbarEvents = events,
    ) {
        PullToRefreshBox(modifier = Modifier.fillMaxSize(),
            isRefreshing = categoryScreenState.loading,
            onRefresh = onRefresh,
            contentAlignment = Alignment.TopCenter) {
            if (categoryScreenState.loaded) {
                categoryScreenState.category?.let {
                    CategoryDetailList(
                        category = it,
                        onAlbumClick = onAlbumClick,
                        onSongClick = onSongClick,
                        onFaveSongClick = onFaveSongClick,
                        scrollToTop = scrollToTop,
                    )
                }
            }
        }

        CategoryDetailError(
            categoryScreenState = categoryScreenState,
            snackbarHostState = snackbarHostState,
            onRetry = onRefresh
        )
    }
}

@Composable
fun CategoryDetailList(
    category: CategoryState,
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
        item(span = itemSpan(gridColumnCount), key = "category-${category.id}") {
            Text(text = category.name,
                style = AppTypography.titleMedium,
                modifier = Modifier.padding(start = listItemPadding, top = itemPadding,
                    end = listItemPadding, bottom = itemPadding))
        }

        itemsIndexed(items = category.items,
            key = { _, item -> item.key },
            contentType = { _, item -> item.getSimpleClassName() },
            span = itemsSpan(gridColumnCount) { _, item ->
                if (item is AlbumState) GridItemSpan(gridColumnCount) else GridItemSpan(1)
            }
        ) { _, item ->
            when (item) {
                is AlbumState -> {
                    Box(modifier = Modifier.animateItem()) {
                        AlbumHeaderItem(album = item, onClick = onAlbumClick)
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
private fun CategoryDetailError(categoryScreenState: CategoryScreenState,
                                snackbarHostState: SnackbarHostState,
                                onRetry: () -> Unit) {
    if (categoryScreenState.error == null) return
    AppError(
        showAsSnackbar = categoryScreenState.loaded,
        snackbarHostState = snackbarHostState,
        error = categoryScreenState.error,
        onRetry = onRetry
    )
}

private class CategoryScreenPreviewProvider : PreviewParameterProvider<CategoryState> {
    override val values: Sequence<CategoryState> = sequenceOf(
        categoryStateData[0],
    )
}

@Preview
@Composable
private fun CategoryDetailListPreview(
    @PreviewParameter(CategoryScreenPreviewProvider::class) category: CategoryState)
{
    PreviewTheme {
        CategoryDetailScreen(
            categoryScreenStateFlow = remember { MutableStateFlow(CategoryScreenState.loaded(category)) },
            events = remember { MutableSharedFlow() },
            onSongClick = {},
            onFaveSongClick = {},
            onRefresh = {},
            onBackClick = {},
            onAlbumClick = {})
    }
}

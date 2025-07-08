package com.flashsphere.rainwaveplayer.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.model.toOperationError
import com.flashsphere.rainwaveplayer.ui.AppBarTitle
import com.flashsphere.rainwaveplayer.ui.AppError
import com.flashsphere.rainwaveplayer.ui.AppScaffold
import com.flashsphere.rainwaveplayer.ui.BackIcon
import com.flashsphere.rainwaveplayer.ui.PullToRefreshBox
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiSettings
import com.flashsphere.rainwaveplayer.ui.enterAlwaysScrollBehavior
import com.flashsphere.rainwaveplayer.ui.item.ErrorItem
import com.flashsphere.rainwaveplayer.ui.item.LoadingItem
import com.flashsphere.rainwaveplayer.ui.scrollToItem
import com.flashsphere.rainwaveplayer.ui.theme.AppTypography
import com.flashsphere.rainwaveplayer.view.uistate.StationsScreenState
import com.flashsphere.rainwaveplayer.view.uistate.event.SnackbarEvent
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagedSongsWithStationSelector(
    flow: Flow<PagingData<SongState>>,
    stationsScreenStateFlow: StateFlow<StationsScreenState>,
    title: String,
    stationFlow: StateFlow<Station?>,
    onStationSelected: (station: Station) -> Unit,
    events: Flow<SnackbarEvent>,
    onFaveClick: (song: SongState) -> Unit,
    onStationsRetry: () -> Unit,
    onBackPress: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollToTop = remember { mutableStateOf(false) }
    val stationsScreenState = stationsScreenStateFlow.collectAsStateWithLifecycle().value

    val windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        .union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))

    AppScaffold(
        modifier = Modifier.windowInsetsPadding(windowInsets),
        navigationIcon = {
            BackIcon(onBackPress)
        },
        appBarContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppBarTitle(
                    modifier = Modifier.weight(1F),
                    title = title,
                    onClick = { scope.launch { scrollToTop.value = true } },
                )

                StationsSelector(stationsScreenState = stationsScreenState,
                    stationFlow = stationFlow, onStationSelected = onStationSelected)
            }
        },
        appBarScrollBehavior = enterAlwaysScrollBehavior(scrollToTop = scrollToTop),
        snackbarEvents = events,
    ) {
        val data = flow.collectAsLazyPagingItems()

        val refreshLoadState = data.loadState.refresh
        val refreshing = stationsScreenState.loading || refreshLoadState is LoadState.Loading
        val noResults = data.loadState.let {
            it.source.refresh is LoadState.NotLoading && it.append.endOfPaginationReached &&
                data.itemCount == 0
        }
        val retry = {
            if (stationsScreenState.error != null) {
                onStationsRetry()
            } else {
                data.refresh()
            }
        }

        PullToRefreshBox(modifier = Modifier.fillMaxSize(),
            isRefreshing = refreshing,
            onRefresh = retry,
            contentAlignment = Alignment.TopCenter) {

            PagedSongList(data, onFaveClick, scrollToTop)

            if (noResults) {
                Text(text = stringResource(id = R.string.no_results),
                    Modifier.padding(top = 20.dp))
            }
        }

        PagedSongsError(data, stationsScreenState, snackbarHostState, retry)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationsSelector(
    stationsScreenState: StationsScreenState,
    stationFlow: StateFlow<Station?>,
    onStationSelected: (station: Station) -> Unit
) {
    val stations = stationsScreenState.stations ?: return
    val station = stationFlow.collectAsStateWithLifecycle().value ?: return

    Box {
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            BasicTextField(
                modifier = Modifier.clickable { expanded = true }
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                value = station.name, onValueChange = {},
                singleLine = true,
                readOnly = true,
                enabled = false,
                textStyle = LocalTextStyle.current.merge(AppTypography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)),
            ) { innerTextField ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 36.dp)) {
                    Spacer(Modifier.size(4.dp))
                    Box(Modifier.width(68.dp)) {
                        innerTextField()
                    }
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null
                    )
                }
            }
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                stations.forEach {
                    DropdownMenuItem(
                        text = { Text(text = it.name, style = AppTypography.bodyMedium) },
                        onClick = {
                            onStationSelected(it)
                            expanded = false
                        })
                }
            }
        }
    }
}

@Composable
private fun PagedSongList(
    data: LazyPagingItems<SongState>,
    onFaveClick: (song: SongState) -> Unit,
    scrollToTop: MutableState<Boolean>
) {
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    LaunchedEffect(scrollToTop) {
        snapshotFlow { scrollToTop.value }
            .filter { it }
            .collect {
                scrollToTop.value = false
                scrollToItem(scope, gridState, 0)
            }
    }

    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        state = gridState,
        columns = GridCells.Fixed(LocalUiScreenConfig.current.gridSpan),
        contentPadding = PaddingValues(bottom = 80.dp),
    ) {
        items(
            count = data.itemCount,
            contentType = data.itemContentType { "Song" }
        ) { i ->
            data[i]?.let { song ->
                SearchSong(song = song,
                    showGlobalRating = !LocalUiSettings.current.hideRatingsUntilRated,
                    onClick = null,
                    onFaveClick = onFaveClick)
            }
        }

        when (data.loadState.append) {
            is LoadState.Loading -> {
                item(key = -1, contentType = "Loading") {
                    LoadingItem()
                }
            }

            is LoadState.Error -> {
                item(key = -2, contentType = "Error") {
                    ErrorItem(message = stringResource(id = R.string.error_connection),
                        onRetryClick = { data.retry() })
                }
            }

            else -> {}
        }
    }
}

@Composable
private fun PagedSongsError(
    data: LazyPagingItems<SongState>,
    snackbarHostState: SnackbarHostState,
    onRetry: () -> Unit
) {
    val refreshLoadState = data.loadState.refresh
    if (refreshLoadState !is LoadState.Error) return
    AppError(
        showAsSnackbar = data.itemCount > 0,
        snackbarHostState = snackbarHostState,
        error = refreshLoadState.error.toOperationError(),
        onRetry = onRetry
    )
}

@Composable
private fun PagedSongsError(
    data: LazyPagingItems<SongState>,
    stationsScreenState: StationsScreenState,
    snackbarHostState: SnackbarHostState,
    onRetry: () -> Unit
) {
    val refreshLoadState = data.loadState.refresh
    val error = stationsScreenState.error ?: if (refreshLoadState is LoadState.Error) {
        refreshLoadState.error.toOperationError()
    } else {
        return
    }
    AppError(
        showAsSnackbar = data.itemCount > 0,
        snackbarHostState = snackbarHostState,
        error = error,
        onRetry = onRetry
    )
}

class PagingDataPreviewProvider :
    PreviewParameterProvider<PagingData<SongState>> {
    override val values: Sequence<PagingData<SongState>> = sequenceOf(
        PagingData.from(songStateData),
        PagingData.empty(
            sourceLoadStates = LoadStates(
                refresh = LoadState.NotLoading(false),
                prepend = LoadState.NotLoading(false),
                append = LoadState.NotLoading(true),
            )
        ),
        PagingData.from(
            data = songStateData, sourceLoadStates = LoadStates(
                refresh = LoadState.NotLoading(false),
                prepend = LoadState.NotLoading(false),
                append = LoadState.Loading,
            )
        ),
        PagingData.from(
            data = songStateData, sourceLoadStates = LoadStates(
                refresh = LoadState.NotLoading(false),
                prepend = LoadState.NotLoading(false),
                append = LoadState.Error(RuntimeException()),
            )
        ),
    )
}

@Preview
@Composable
private fun PagedSongsWithStationSelectorPreview(
    @PreviewParameter(PagingDataPreviewProvider::class) pagingData: PagingData<SongState>,
) {
    val stationFlow = remember { MutableStateFlow(stations[0]) }
    PreviewTheme {
        PagedSongsWithStationSelector(
            flow = remember { MutableStateFlow(pagingData) },
            stationsScreenStateFlow = remember { MutableStateFlow(StationsScreenState.loaded(stations)) },
            title = "Recent Votes",
            stationFlow = stationFlow,
            onStationSelected = { stationFlow.value = it },
            events = remember { MutableSharedFlow() },
            onFaveClick = {},
            onStationsRetry = {},
            onBackPress = {}
        )
    }
}

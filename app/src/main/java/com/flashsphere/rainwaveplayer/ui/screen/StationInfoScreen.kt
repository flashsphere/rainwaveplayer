package com.flashsphere.rainwaveplayer.ui.screen

import android.widget.Toast
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.flow.tickerFlow
import com.flashsphere.rainwaveplayer.model.MediaPlayerStatus
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.ui.AppBarSubtitleText
import com.flashsphere.rainwaveplayer.ui.AppBarTitle
import com.flashsphere.rainwaveplayer.ui.AppBarTitleText
import com.flashsphere.rainwaveplayer.ui.AppError
import com.flashsphere.rainwaveplayer.ui.AppScaffold
import com.flashsphere.rainwaveplayer.ui.Fave
import com.flashsphere.rainwaveplayer.ui.FaveAlbum
import com.flashsphere.rainwaveplayer.ui.FaveSong
import com.flashsphere.rainwaveplayer.ui.MenuIcon
import com.flashsphere.rainwaveplayer.ui.OtherRequestor
import com.flashsphere.rainwaveplayer.ui.PullToRefreshBox
import com.flashsphere.rainwaveplayer.ui.SelfRequestor
import com.flashsphere.rainwaveplayer.ui.ToastHandler
import com.flashsphere.rainwaveplayer.ui.Tooltip
import com.flashsphere.rainwaveplayer.ui.alertdialog.CustomAlertDialog
import com.flashsphere.rainwaveplayer.ui.animateScrollToItem
import com.flashsphere.rainwaveplayer.ui.appbar.AppBarActions
import com.flashsphere.rainwaveplayer.ui.appbar.toAppBarAction
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiSettings
import com.flashsphere.rainwaveplayer.ui.composition.LocalUserCredentials
import com.flashsphere.rainwaveplayer.ui.enterAlwaysScrollBehavior
import com.flashsphere.rainwaveplayer.ui.item.AlbumArt
import com.flashsphere.rainwaveplayer.ui.item.HorizontalSeparator
import com.flashsphere.rainwaveplayer.ui.itemsSpan
import com.flashsphere.rainwaveplayer.ui.navigation.AlbumDetail
import com.flashsphere.rainwaveplayer.ui.navigation.ArtistDetail
import com.flashsphere.rainwaveplayer.ui.navigation.Library
import com.flashsphere.rainwaveplayer.ui.navigation.Navigator
import com.flashsphere.rainwaveplayer.ui.navigation.Requests
import com.flashsphere.rainwaveplayer.ui.navigation.Search
import com.flashsphere.rainwaveplayer.ui.rating.RatingDialog
import com.flashsphere.rainwaveplayer.ui.rating.RatingText
import com.flashsphere.rainwaveplayer.ui.theme.AppTypography
import com.flashsphere.rainwaveplayer.ui.theme.SansSerifCondensed
import com.flashsphere.rainwaveplayer.util.Formatter
import com.flashsphere.rainwaveplayer.util.JobUtils.cancel
import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.util.Strings.formatDuration
import com.flashsphere.rainwaveplayer.util.UserCredentials
import com.flashsphere.rainwaveplayer.util.getTimeRemaining
import com.flashsphere.rainwaveplayer.util.isLoggedIn
import com.flashsphere.rainwaveplayer.view.uistate.RatingState
import com.flashsphere.rainwaveplayer.view.uistate.SongActionState
import com.flashsphere.rainwaveplayer.view.uistate.StationInfoScreenState
import com.flashsphere.rainwaveplayer.view.uistate.event.SnackbarEvent
import com.flashsphere.rainwaveplayer.view.uistate.model.AlbumState
import com.flashsphere.rainwaveplayer.view.uistate.model.ComingUpHeaderItem
import com.flashsphere.rainwaveplayer.view.uistate.model.ComingUpSongItem
import com.flashsphere.rainwaveplayer.view.uistate.model.NowPlayingHeaderItem
import com.flashsphere.rainwaveplayer.view.uistate.model.NowPlayingSongItem
import com.flashsphere.rainwaveplayer.view.uistate.model.PreviouslyPlayedHeaderItem
import com.flashsphere.rainwaveplayer.view.uistate.model.PreviouslyPlayedSongItem
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState
import com.flashsphere.rainwaveplayer.view.uistate.model.StationInfo
import com.flashsphere.rainwaveplayer.view.uistate.model.StationInfoHeaderItem
import com.flashsphere.rainwaveplayer.view.uistate.model.StationInfoSongData
import com.flashsphere.rainwaveplayer.view.uistate.model.UserState
import com.flashsphere.rainwaveplayer.view.viewmodel.MainViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds

@Composable
fun StationInfoScreen(
    navigator: Navigator,
    viewModel: MainViewModel,
    scrollToTop: MutableState<Boolean>,
    onMenuClick: () -> Unit,
) {
    val station = viewModel.station.collectAsStateWithLifecycle().value ?: return

    LifecycleStartEffect(Unit) {
        viewModel.subscribeStateChangeEvents()
        onStopOrDispose { viewModel.unsubscribeStateChangeEvents() }
    }

    LifecycleStartEffect(station) {
        viewModel.subscribeStationInfo(refresh = false)
        onStopOrDispose { viewModel.unsubscribeStationInfo() }
    }

    StationInfoScreen(
        navigator = navigator,
        station = station,
        stationInfoScreenStateFlow = viewModel.stationInfoScreenState,
        userStateFlow = viewModel.user,
        playbackState = viewModel.playbackState,
        events = viewModel.snackbarEvents,
        scrollToTop = scrollToTop,
        showPreviouslyPlayed = viewModel.showPreviouslyPlayed,
        onPreviousPlayedHeaderClick = { viewModel.showPreviouslyPlayed(it) },
        onVoteClick = { item -> viewModel.voteSong(item.eventId, item.data.song.entryId) },
        onRateClick = { rating -> viewModel.rateSong(rating) },
        onRemoveRatingClick = { rating -> viewModel.rateSong(rating) },
        onFaveSongClick = { song, position -> viewModel.faveSong(song) },
        onFaveAlbumClick = { album, position -> viewModel.faveAlbum(album) },
        onRetry = { viewModel.subscribeStationInfo(refresh = true) },
        castState = viewModel.castState,
        onMenuClick = onMenuClick,
        onPlaybackClick = viewModel::togglePlayback,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationInfoScreen(
    navigator: Navigator,
    station: Station,
    stationInfoScreenStateFlow: StateFlow<StationInfoScreenState>,
    userStateFlow: StateFlow<UserState?>,
    playbackState: StateFlow<MediaPlayerStatus>,
    events: Flow<SnackbarEvent>,
    scrollToTop: MutableState<Boolean>,
    showPreviouslyPlayed: StateFlow<Boolean>,
    onPreviousPlayedHeaderClick: (expand: Boolean) -> Unit,
    onVoteClick: (item: ComingUpSongItem) -> Unit,
    onRateClick: (ratingState: RatingState) -> Unit,
    onRemoveRatingClick: (ratingState: RatingState) -> Unit,
    onFaveSongClick: (song: SongState, position: Int) -> Unit,
    onFaveAlbumClick: (album: AlbumState, position: Int) -> Unit,
    onRetry: () -> Unit,
    castState: MutableStateFlow<String>,
    onMenuClick: () -> Unit,
    onPlaybackClick: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val ratingState = rememberSaveable { mutableStateOf<RatingState?>(null) }
    val songActionsState = rememberSaveable(stateSaver = SongActionState.Saver) { mutableStateOf(null) }
    val toastState = remember { mutableStateOf<Toast?>(null) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AppScaffold(
        modifier = Modifier
            .onKeyEvent { ke ->
                if (ke.type != KeyEventType.KeyDown) {
                    return@onKeyEvent false
                }
                when (ke.key) {
                    Key.MediaPlay,
                    Key.MediaPlayPause -> {
                        Timber.d("%s key pressed", ke.key.nativeKeyCode)
                        onPlaybackClick()
                        true
                    }
                    else -> {
                        false
                    }
                }
            }
            .focusRequester(focusRequester)
            .focusable(true),
        appBarContent = {
            StationInfoAppBarTitle(
                station = station,
                userStateFlow = userStateFlow,
                onClick = { scope.launch { scrollToTop.value = true } },
            )
        },
        appBarScrollBehavior = enterAlwaysScrollBehavior(scrollToTop),
        navigationIcon = {
            MenuIcon(onMenuClick)
        },
        floatingActionButton = {
            PlaybackFab(station, playbackState, onPlaybackClick)
        },
        appBarActions = {
            CastButton()
            if (LocalUserCredentials.current.isLoggedIn() && LocalUiSettings.current.bottomNavPreference.isHidden()) {
                if (LocalUiScreenConfig.current.widthSizeClass > WindowWidthSizeClass.Compact) {
                    AppBarActions(
                        toAppBarAction(navigator, listOf(Requests, Library, Search)),
                    )
                } else {
                    AppBarActions(
                        toAppBarAction(navigator, listOf(Requests)),
                        toAppBarAction(navigator, listOf(Library, Search)),
                    )
                }
            }
        },
        snackbarEvents = events,
    ) {
        val screenState = stationInfoScreenStateFlow.collectAsStateWithLifecycle().value

        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            enabled = false,
            isRefreshing = screenState.loading,
            onRefresh = {},
            contentAlignment = Alignment.TopCenter) {
            if (screenState.stationInfo != null) {
                StationInfoList(
                    stationInfo = screenState.stationInfo,
                    scrollToTop = scrollToTop,
                    showPreviouslyPlayed = showPreviouslyPlayed,
                    onPreviousPlayedHeaderClick = onPreviousPlayedHeaderClick,
                    onVoteClick = onVoteClick,
                    onRateClick = { song, i ->
                        ratingState.value = RatingState(song)
                    },
                    onFaveSongClick = onFaveSongClick,
                    onFaveAlbumClick = onFaveAlbumClick,
                    onSongActionsClick = {
                        songActionsState.value = SongActionState(it)
                        toastState.value = null
                    },
                )
            }

            CastInfo(modifier = Modifier.align(Alignment.BottomStart), castState = castState)
        }

        SongActionsDialog(
            state = songActionsState,
            onRateClick = {
                songActionsState.value = null
                ratingState.value = it.toRatingState()
            },
            onAlbumClick = {
                songActionsState.value = null
                navigator.navigate(it)
            },
            onArtistClick = {
                songActionsState.value = null
                navigator.navigate(it)
            },
        )

        RatingDialog(ratingState = ratingState,
            onRemoveRating = onRemoveRatingClick,
            onRate = onRateClick)

        ToastHandler(state = toastState)

        AppError(error = screenState.error, onRetry = onRetry)
    }
}

@Composable
private fun StationInfoAppBarTitle(
    station: Station,
    userStateFlow: StateFlow<UserState?>,
    onClick: (() -> Unit),
) {
    val user = userStateFlow.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    AppBarTitle(
        title = {
            AppBarTitleText(text = station.name, maxLines = 1)
        },
        subtitle = {
            user?.let {
                if (user.requestsPaused) {
                    AppBarSubtitleText(text = stringResource(R.string.requests_suspended),
                        modifier = Modifier.background(color = colorResource(R.color.suspended)))
                } else if (it.requestPosition > 0) {
                    AppBarSubtitleText(text = stringResource(R.string.request_position,
                        Formatter.formatNumberOrdinal(context, it.requestPosition))
                    )
                }
            }
        },
        onClick = onClick,
    )
}

@Composable
private fun StationInfoList(
    stationInfo: StationInfo,
    scrollToTop: MutableState<Boolean>,
    showPreviouslyPlayed: StateFlow<Boolean>,
    onPreviousPlayedHeaderClick: (expand: Boolean) -> Unit,
    onVoteClick: (item: ComingUpSongItem) -> Unit,
    onRateClick: (song: SongState, position: Int) -> Unit,
    onFaveSongClick: (song: SongState, position: Int) -> Unit,
    onFaveAlbumClick: (album: AlbumState, position: Int) -> Unit,
    onSongActionsClick: (SongState) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val gridColumnCount = LocalUiScreenConfig.current.gridSpan
    val isLoggedIn = LocalUserCredentials.current.isLoggedIn()

    val eventId = rememberSaveable { mutableIntStateOf(stationInfo.currentEventId) }

    LaunchedEffect(scrollToTop) {
        snapshotFlow { scrollToTop.value }
            .filter { it }
            .collect {
                scrollToTop.value = false
                animateScrollToItem(scope, gridState, 0)
            }
    }
    LaunchedEffect(key1 = stationInfo.currentEventId) {
        if (eventId.intValue != stationInfo.currentEventId) {
            eventId.intValue = stationInfo.currentEventId
            val index = if (showPreviouslyPlayed.value) {
                stationInfo.items.indexOfFirst { it.key == "event-${stationInfo.currentEventId}" }
            } else {
                0
            }
            animateScrollToItem(scope, gridState, max(index, 0))
        }
    }

    LazyVerticalGrid(state = gridState,
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Fixed(gridColumnCount),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        itemsIndexed(items = stationInfo.items,
            key = { _, item -> item.key },
            contentType = { _, item -> item.contentType },
            span = itemsSpan(gridColumnCount) { _, item ->
               if (item is StationInfoHeaderItem || item is NowPlayingSongItem)
                   GridItemSpan(gridColumnCount)
               else
                   GridItemSpan(1)
            }
        ) { i, item ->
            when (item) {
                is PreviouslyPlayedHeaderItem -> {
                    Box(modifier = Modifier.animateItem()) {
                        PreviouslyPlayedHeader(showPreviouslyPlayed = showPreviouslyPlayed,
                            onClick = onPreviousPlayedHeaderClick)
                    }
                }
                is PreviouslyPlayedSongItem -> {
                    Box(modifier = Modifier.animateItem()) {
                        val onClick = when {
                            isLoggedIn && item.data.song.ratingAllowed -> {
                                { onRateClick(item.data.song, i) }
                            }
                            else -> null
                        }
                        val onMoreClick = if (isLoggedIn) {
                            { onSongActionsClick(item.data.song) }
                        } else {
                            null
                        }
                        StationInfoSong(
                            item = item.data,
                            index = i,
                            onClick = onClick,
                            onMoreClick = onMoreClick,
                            onFaveSongClick = onFaveSongClick,
                            onFaveAlbumClick = onFaveAlbumClick,
                            showGlobalRating = !LocalUiSettings.current.hideRatingsUntilRated,
                        )
                    }
                }
                is NowPlayingHeaderItem -> {
                    Box(modifier = Modifier.animateItem()) {
                        NowPlayingHeader(item = item)
                    }
                }
                is NowPlayingSongItem -> {
                    Box(modifier = Modifier.animateItem()) {
                        val onClick = when {
                            isLoggedIn && item.data.song.ratingAllowed -> {
                                { onRateClick(item.data.song, i) }
                            }
                            else -> null
                        }
                        val onMoreClick = if (isLoggedIn) {
                            { onSongActionsClick(item.data.song) }
                        } else {
                            null
                        }
                        StationInfoSong(
                            item = item.data,
                            index = i,
                            onClick = onClick,
                            onMoreClick = onMoreClick,
                            onFaveSongClick = onFaveSongClick,
                            onFaveAlbumClick = onFaveAlbumClick,
                            showGlobalRating = !LocalUiSettings.current.hideRatingsUntilRated,
                        )
                    }
                }
                is ComingUpHeaderItem -> {
                    Box(modifier = Modifier.animateItem()) {
                        ComingUpHeader(item = item)
                    }
                }
                is ComingUpSongItem -> {
                    Box(modifier = Modifier.animateItem()) {
                        val song = item.data.song
                        val onClick = when {
                            isLoggedIn && song.votingAllowed && !song.voted.value -> {
                                { onVoteClick(item) }
                            }
                            else -> null
                        }
                        val onMoreClick = if (isLoggedIn) {
                            { onSongActionsClick(item.data.song) }
                        } else {
                            null
                        }
                        StationInfoSong(
                            item = item.data,
                            index = i,
                            onClick = onClick,
                            onMoreClick = onMoreClick,
                            onFaveSongClick = onFaveSongClick,
                            onFaveAlbumClick = onFaveAlbumClick,
                            showGlobalRating = !LocalUiSettings.current.hideRatingsUntilRated,
                        )
                    }
                }
                else -> {}
            }
        }
    }
    ReportDrawnWhen { gridState.layoutInfo.totalItemsCount > 0 }
}

@Composable
private fun PreviouslyPlayedHeader(showPreviouslyPlayed: StateFlow<Boolean>,
                           onClick: (expand: Boolean) -> Unit) {
    val expand = showPreviouslyPlayed.collectAsStateWithLifecycle().value
    val imageVector = if (expand) {
        Icons.Filled.ArrowDropUp
    } else {
        Icons.Filled.ArrowDropDown
    }
    Row(modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .clickable { onClick(!expand) }
        .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = stringResource(id = R.string.previously_played).uppercase(),
            style = AppTypography.bodyMedium,
            fontSize = 13.sp,
            lineHeight = 13.sp,
            modifier = Modifier.padding(end = 4.dp))
        Icon(imageVector = imageVector, contentDescription = null,
            modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun NowPlayingHeader(item: NowPlayingHeaderItem) {
    val eventName = if (item.eventName.isNotEmpty()) {
        stringResource(R.string.now_playing_event, item.eventName)
    } else {
        stringResource(id = R.string.now_playing)
    }

    Column(modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 4.dp)
    ) {
        HorizontalSeparator()

        Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
            Text(text = eventName.uppercase(),
                style = AppTypography.bodyMedium,
                fontSize = 13.sp,
                lineHeight = 13.sp,
                modifier = Modifier
                    .weight(1F)
                    .padding(top = 8.dp, end = 8.dp))
            EventCountdown(modifier = Modifier.align(Alignment.Bottom), item = item)
        }
    }
}

@Composable
private fun EventCountdown(modifier: Modifier, item: NowPlayingHeaderItem) {
    val scope = rememberCoroutineScope()
    var countdown by remember { mutableStateOf("") }
    LifecycleStartEffect(item) {
        val job = tickerFlow(1.seconds)
            .map { item.getTimeRemaining() }
            .takeWhile { it >= 0 }
            .onEach { countdown = formatDuration(it.seconds) }
            .launchIn(scope)
        onStopOrDispose { cancel(job) }
    }
    Text(text = countdown, style = AppTypography.bodySmall,
        fontSize = 11.sp, lineHeight = 11.sp, modifier = modifier
    )
}

@Composable
private fun ComingUpHeader(item: ComingUpHeaderItem) {
    val comingUp = item.getEventName(LocalContext.current, LocalUserCredentials.current.isLoggedIn())
    Column(modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 4.dp)) {
        HorizontalSeparator()
        Text(text = comingUp.uppercase(),
            style = AppTypography.bodyMedium,
            fontSize = 13.sp,
            lineHeight = 13.sp,
            modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun StationInfoSong(
    modifier: Modifier = Modifier,
    item: StationInfoSongData,
    index: Int,
    showGlobalRating: Boolean,
    onClick: (() -> Unit)?,
    onMoreClick: (() -> Unit)?,
    onFaveSongClick: (song: SongState, position: Int) -> Unit,
    onFaveAlbumClick: (album: AlbumState, position: Int) -> Unit,
) {
    val song = item.song
    val album = item.album
    val isLoggedIn = LocalUserCredentials.current.isLoggedIn()
    val background = if (song.voted.value) {
        Modifier.background(colorResource(id = R.color.voted))
    } else {
        Modifier
    }

    Row(modifier = modifier
        .then(background)
        .fillMaxWidth()
        .height(IntrinsicSize.Max)
        .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
        .padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 4.dp)) {

        AlbumImage(item = item)

        Box(Modifier.weight(1F).fillMaxHeight()) {
            Spacer(Modifier
                .width(28.dp)
                .fillMaxHeight()
                //.background(Color.Blue.copy(alpha = 0.5F))
                .focusProperties { canFocus = false }
                .clickable(interactionSource = null, indication = null, onClick = {})
                .semantics { this.hideFromAccessibility() }
            )

            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                Row {
                    if (isLoggedIn) {
                        FaveIcon(fave = FaveSong(song.favorite.value), onFaveClick = { onFaveSongClick(song, index) })
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(text = song.title, style = AppTypography.bodyLarge,
                        lineHeight = LocalUiScreenConfig.current.songItemTitleLineHeight,
                        modifier = Modifier.weight(1F).padding(end = 5.dp))
                    RatingText(song = song, showGlobalRating = showGlobalRating)
                }
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    if (isLoggedIn) {
                        FaveIcon(fave = FaveAlbum(album.favorite.value), onFaveClick = { onFaveAlbumClick(album, index) })
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(text = song.albumName, style = AppTypography.bodyMedium,
                        lineHeight = LocalUiScreenConfig.current.songItemAlbumLineHeight,
                        modifier = Modifier.weight(1F))
                }
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    if (isLoggedIn) {
                        Spacer(modifier = Modifier.width(28.dp))
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(text = song.artistName, style = AppTypography.bodySmall,
                        lineHeight = LocalUiScreenConfig.current.songItemArtistLineHeight,
                        modifier = Modifier.weight(1F))
                }
            }

            if (onMoreClick != null) {
                Box(modifier = Modifier.align(Alignment.BottomStart)) {
                    Tooltip(stringResource(id = R.string.action_more)) {
                        Box(modifier = Modifier.clickable(
                            interactionSource = null,
                            indication = ripple(bounded = false),
                            onClick = onMoreClick)
                        ) {
                            Icon(painter = painterResource(id = R.drawable.ic_more_vert_20dp),
                                tint = colorResource(id = R.color.unfavorite),
                                contentDescription = stringResource(id = R.string.action_more),
                                modifier = Modifier.padding(start = 4.dp, end = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumImage(modifier: Modifier = Modifier, item: StationInfoSongData) {
    val album = item.album
    val albumImageSize = LocalUiScreenConfig.current.nowPlayingImageSize

    Box(modifier = modifier.size(albumImageSize)) {
        AlbumArt(
            art = album.art,
            modifier = Modifier.size(albumImageSize),
        )
        if (item.requestorName.isNotEmpty()) {
            val userCredentials = LocalUserCredentials.current
            val (requestorBgColor, requestorTextColor, requestorText) = if (userCredentials?.userId == item.requestorId) {
                SelfRequestor
            } else {
                OtherRequestor
            }

            val lineHeightDp: Dp = with(LocalDensity.current) {
                13.sp.toDp()
            }

            Text(text = stringResource(id = requestorText).uppercase(),
                color = colorResource(id = requestorTextColor),
                style = AppTypography.bodySmall,
                fontFamily = SansSerifCondensed,
                fontSize = 11.sp,
                lineHeight = 11.sp,
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .width(albumImageSize)
                    .rotate(-90F)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val x = (placeable.width - placeable.height) / 2
                        val y = placeable.width / 2 - placeable.height / 2
                        layout(placeable.width, placeable.height) {
                            placeable.place(-x, -y)
                        }
                    }
                    .background(colorResource(id = requestorBgColor))
                    .padding(end = lineHeightDp))
            Text(text = item.requestorName.uppercase(),
                color = colorResource(id = requestorTextColor),
                style = AppTypography.bodySmall,
                fontFamily = SansSerifCondensed,
                fontSize = 11.sp,
                lineHeight = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .width(albumImageSize)
                    .background(colorResource(id = requestorBgColor))
                    .padding(start = lineHeightDp))
        }
    }
}

@Composable
private fun FaveIcon(modifier: Modifier = Modifier, fave: Fave, onFaveClick: () -> Unit) {
    val (faveDrawable, faveColor, faveDesc) = fave

    Box(modifier = modifier) {
        Tooltip(stringResource(id = faveDesc)) {
            Box(modifier = Modifier.clickable(
                interactionSource = null,
                indication = ripple(bounded = false),
                onClick = onFaveClick)
            ) {
                Icon(painter = painterResource(id = faveDrawable),
                    tint = colorResource(id = faveColor),
                    contentDescription = stringResource(id = faveDesc),
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp))
            }
        }
    }
}

@Composable
private fun PlaybackFab(
    station: Station,
    playbackState: StateFlow<MediaPlayerStatus>,
    onPlaybackClick: () -> Unit,
) {
    val playerStatus = playbackState.collectAsStateWithLifecycle().value
    val isPlaying = playerStatus.isPlaying(station)

    FloatingActionButton(
        modifier = Modifier.testTag("playback_btn"),
        onClick = onPlaybackClick,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Icon(
            painter = if (isPlaying) {
                painterResource(R.drawable.ic_stop_24dp)
            } else {
                painterResource(R.drawable.ic_play_arrow_24dp)
            },
            contentDescription = if (isPlaying) {
                stringResource(id = R.string.action_stop)
            } else {
                stringResource(id = R.string.action_play)
            },
        )
    }
}

private class StationInfoScreenStatePreviewProvider : PreviewParameterProvider<StationInfoScreenState> {
    override val values: Sequence<StationInfoScreenState> = sequenceOf(StationInfoScreenState(
        stationInfo = stationInfoData,
    ), StationInfoScreenState(
        error = OperationError(OperationError.Server)
    ))
}

@Preview
@Composable
private fun StationInfoScreenPreview(
    @PreviewParameter(StationInfoScreenStatePreviewProvider::class) stationInfoScreenState: StationInfoScreenState
) {
    val showPreviouslyPlayed = remember { MutableStateFlow(false) }

    PreviewTheme(userCredentials = UserCredentials(2, "")) {
        StationInfoScreen(
            navigator = rememberPreviewNavigator(),
            station = stations[0],
            stationInfoScreenStateFlow = MutableStateFlow(stationInfoScreenState),
            userStateFlow = remember { MutableStateFlow(userStateData[0]) },
            playbackState = remember { MutableStateFlow(
                MediaPlayerStatus(
                    stations[0],
                    MediaPlayerStatus.State.Stopped
                )
            ) },
            events = remember { MutableSharedFlow() },
            scrollToTop = remember { mutableStateOf(false) },
            showPreviouslyPlayed = showPreviouslyPlayed,
            onPreviousPlayedHeaderClick = { showPreviouslyPlayed.value = it },
            onVoteClick = {},
            onRateClick = {},
            onRemoveRatingClick = {},
            onFaveSongClick = { _, _ -> },
            onFaveAlbumClick = { _, _ -> },
            onRetry = {},
            castState = remember { MutableStateFlow("") },
            onMenuClick = {},
            onPlaybackClick = {},
        )
    }
}

@Composable
private fun SongActionsDialog(
    state: MutableState<SongActionState?>,
    onRateClick: (SongActionState) -> Unit,
    onAlbumClick: (AlbumDetail) -> Unit,
    onArtistClick: (ArtistDetail) -> Unit,
) {
    val song = state.value ?: return

    CustomAlertDialog(
        onDismissRequest = { state.value = null },
        content = {
            Column(Modifier.padding(top = 24.dp, bottom = 16.dp)) {
                SongActions(modifier = Modifier.weight(weight = 1F, fill = false),
                    song = song, onRateClick = onRateClick, onAlbumClick = onAlbumClick,
                    onArtistClick = onArtistClick)

                TextButton(
                    onClick = { state.value = null },
                    modifier = Modifier.align(Alignment.End).padding(end = 24.dp),
                ) {
                    Text(text = stringResource(id = R.string.action_close))
                }
            }
        },
    )
}

@Composable
private fun SongActions(
    modifier: Modifier = Modifier,
    song: SongActionState,
    onRateClick: (SongActionState) -> Unit,
    onAlbumClick: (AlbumDetail) -> Unit,
    onArtistClick: (ArtistDetail) -> Unit,
) {
    Column(modifier.verticalScroll(song.scrollState)) {
        Text(text = song.songTitle, modifier = Modifier.padding(horizontal = 24.dp),
            style = AppTypography.titleLarge)

        if (song.ratingAllowed) {
            Spacer(Modifier.height(4.dp))
            SongActionItem({ onRateClick(song) }) {
                Text(stringResource(R.string.action_rate), modifier = Modifier.weight(1F),
                    style = AppTypography.bodyLarge)
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                    contentDescription = null)
            }
        }
        SongActionHeaderItem(text = stringResource(R.string.albums))
        song.albums.forEach {
            key("album-${it.id}") {
                SongActionItem({ onAlbumClick(AlbumDetail(it.id, it.name)) }) {
                    Text(it.name, modifier = Modifier.weight(1F),
                        style = AppTypography.bodyLarge)
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                        contentDescription = null)
                }
            }
        }
        SongActionHeaderItem(text = stringResource(R.string.artists))
        song.artists.forEach {
            key("artist-${it.id}") {
                SongActionItem({ onArtistClick(ArtistDetail(it.id, it.name)) }) {
                    Text(it.name, modifier = Modifier.weight(1F),
                        style = AppTypography.bodyLarge)
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                        contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun SongActionHeaderItem(text: String) {
    Column(Modifier.padding(horizontal = 24.dp).padding(top = 16.dp)) {
        Text(text = text.uppercase(), style = AppTypography.bodySmall)
        Spacer(Modifier.height(4.dp))
        HorizontalDivider()
    }
}

@Composable
private fun SongActionItem(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Row(modifier = Modifier.heightIn(36.dp).clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Preview
@Composable
private fun SongActionsDialogPreview() {
    PreviewTheme {
        Surface {
            SongActionsDialog(
                state = remember { mutableStateOf(SongActionState(songStateData[4])) },
                onRateClick = {},
                onAlbumClick = {},
                onArtistClick = {},
            )
        }
    }
}

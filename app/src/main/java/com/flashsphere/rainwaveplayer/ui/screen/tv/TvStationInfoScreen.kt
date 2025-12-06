package com.flashsphere.rainwaveplayer.ui.screen.tv

import androidx.activity.compose.ReportDrawn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Surface
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.model.MediaPlayerStatus
import com.flashsphere.rainwaveplayer.model.MediaPlayerStatus.State.Playing
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.ui.TvErrorWithRetry
import com.flashsphere.rainwaveplayer.ui.TvLoading
import com.flashsphere.rainwaveplayer.ui.animation.fadeIn
import com.flashsphere.rainwaveplayer.ui.animation.fadeOut
import com.flashsphere.rainwaveplayer.ui.composition.LastFocused
import com.flashsphere.rainwaveplayer.ui.composition.LocalLastFocused
import com.flashsphere.rainwaveplayer.ui.item.tv.TvComingUp
import com.flashsphere.rainwaveplayer.ui.item.tv.TvNowPlaying
import com.flashsphere.rainwaveplayer.ui.item.tv.TvPreviouslyPlayed
import com.flashsphere.rainwaveplayer.ui.navigation.Navigator
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTv
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTvTheme
import com.flashsphere.rainwaveplayer.ui.screen.stationInfoData
import com.flashsphere.rainwaveplayer.ui.screen.stations
import com.flashsphere.rainwaveplayer.util.OperationError
import com.flashsphere.rainwaveplayer.view.uistate.RatingState
import com.flashsphere.rainwaveplayer.view.uistate.StationInfoScreenState
import com.flashsphere.rainwaveplayer.view.uistate.model.ComingUpSongItem
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState
import com.flashsphere.rainwaveplayer.view.uistate.model.StationInfoSongItem
import com.flashsphere.rainwaveplayer.view.viewmodel.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun TvStationInfoScreen(
    navigator: Navigator,
    viewModel: MainViewModel,
) {
    val station = viewModel.station.collectAsStateWithLifecycle().value ?: return
    val stationInfoScreenState = viewModel.stationInfoScreenState.collectAsStateWithLifecycle().value
    val selectedItem = remember { mutableStateOf<StationInfoSongItem?>(null) }
    val lastFocused = LocalLastFocused.current

    LifecycleStartEffect(Unit) {
        if (lastFocused.value.tag == null) {
            lastFocused.value = LastFocused("playback_btn", true)
        } else if (selectedItem.value == null) {
            lastFocused.value = lastFocused.value.copy(shouldRequestFocus = true)
        }

        viewModel.subscribeStateChangeEvents()
        onStopOrDispose { viewModel.unsubscribeStateChangeEvents() }
    }
    LifecycleStartEffect(station) {
        viewModel.subscribeStationInfo(false)
        onStopOrDispose { viewModel.unsubscribeStationInfo() }
    }

    TvStationInfoScreen(
        station = station,
        screenState = stationInfoScreenState,
        playbackState = viewModel.playbackState,
        onPlaybackClick = viewModel::togglePlayback,
        onRetry = { viewModel.subscribeStationInfo(true) },
        onFaveSongClick = { song -> viewModel.faveSong(song) },
        onRateClick = { rating -> viewModel.rateSong(rating) },
        onVoteClick = { item -> viewModel.voteSong(item.eventId, item.data.song.entryId) },
        onMoreClick = { selectedItem.value = it },
        showToast = { viewModel.showSnackbarMessage(it) },
    )
    AnimatedVisibility(
        visible = selectedItem.value != null,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        selectedItem.value?.let {
            TvStationInfoSongActionsScreen(
                viewModel = viewModel,
                station = station,
                item = it,
                onDismissRequest = { selectedItem.value = null },
            )
            DisposableEffect(Unit) {
                val tag = lastFocused.value.tag
                onDispose {
                    lastFocused.value = LastFocused(tag = tag, shouldRequestFocus = true)
                }
            }
        }
    }
}

@Composable
private fun TvStationInfoScreen(
    station: Station,
    screenState: StationInfoScreenState,
    playbackState: StateFlow<MediaPlayerStatus>,
    onRetry: () -> Unit,
    onPlaybackClick: () -> Unit,
    onFaveSongClick: (song: SongState) -> Unit,
    onRateClick: (ratingState: RatingState) -> Unit,
    onVoteClick: (item: ComingUpSongItem) -> Unit,
    onMoreClick: (item: StationInfoSongItem) -> Unit,
    showToast: (message: String) -> Unit,
) {
    Surface {
        if (screenState.loading) {
            TvLoading()
        } else if (screenState.error != null) {
            TvErrorWithRetry(
                text = stringResource(R.string.error_connection),
                onRetry = onRetry,
            )
        } else if (screenState.stationInfo != null) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                val nowPlayingItem = screenState.stationInfo.nowPlaying.item
                TvNowPlaying(
                    modifier = Modifier.padding(start = 80.dp, end = 40.dp, top = 20.dp),
                    station = station,
                    nowPlayingItem = nowPlayingItem,
                    playbackState = playbackState,
                    onPlaybackClick = onPlaybackClick,
                    onFaveSongClick = onFaveSongClick,
                    onRateClick = onRateClick,
                    onMoreClick = onMoreClick,
                )

                screenState.stationInfo.comingUp.forEachIndexed { i, comingUp ->
                    key(i) {
                        TvComingUp(
                            index = i,
                            comingUp = comingUp,
                            onVoteClick = onVoteClick,
                            onMoreClick = onMoreClick,
                            showToast = showToast,
                        )
                    }
                }

                TvPreviouslyPlayed(
                    items = screenState.stationInfo.previouslyPlayed.items,
                    onMoreClick = onMoreClick,
                    showToast = showToast,
                )

                ReportDrawn()
            }
        }
    }
}

private class TvStationInfoScreenPreviewProvider : PreviewParameterProvider<StationInfoScreenState> {
    override val values: Sequence<StationInfoScreenState> = sequenceOf(StationInfoScreenState(
        stationInfo = stationInfoData,
    ), StationInfoScreenState(
        error = OperationError(OperationError.Server)
    ))
}

@PreviewTv
@Composable
private fun TvStationInfoScreenPreview(
    @PreviewParameter(TvStationInfoScreenPreviewProvider::class) screenState: StationInfoScreenState
) {
    val playbackState = remember { MutableStateFlow(MediaPlayerStatus(stations[0], Playing)) }
    PreviewTvTheme {
        Surface {
            TvStationInfoScreen(
                station = stations[0],
                screenState = screenState,
                playbackState = playbackState,
                onRetry = {},
                onPlaybackClick = {},
                onFaveSongClick = {},
                onRateClick = {},
                onVoteClick = {},
                onMoreClick = {},
                showToast = {},
            )
        }
    }
}

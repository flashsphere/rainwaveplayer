package com.flashsphere.rainwaveplayer.ui.item.tv

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.flow.tickerFlow
import com.flashsphere.rainwaveplayer.model.MediaPlayerStatus
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.ui.Fave
import com.flashsphere.rainwaveplayer.ui.FaveSong
import com.flashsphere.rainwaveplayer.ui.composition.LocalUserCredentials
import com.flashsphere.rainwaveplayer.ui.rating.tv.TvRatingDialog
import com.flashsphere.rainwaveplayer.ui.saveLastFocused
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTv
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTvTheme
import com.flashsphere.rainwaveplayer.ui.screen.songStateData
import com.flashsphere.rainwaveplayer.ui.screen.stations
import com.flashsphere.rainwaveplayer.ui.theme.tv.TvAppTypography
import com.flashsphere.rainwaveplayer.util.Formatter
import com.flashsphere.rainwaveplayer.util.JobUtils.cancel
import com.flashsphere.rainwaveplayer.util.Strings.formatDuration
import com.flashsphere.rainwaveplayer.util.isLoggedIn
import com.flashsphere.rainwaveplayer.view.uistate.RatingState
import com.flashsphere.rainwaveplayer.view.uistate.model.AlbumState
import com.flashsphere.rainwaveplayer.view.uistate.model.NowPlayingSongItem
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState
import com.flashsphere.rainwaveplayer.view.uistate.model.StationInfoSongData
import com.flashsphere.rainwaveplayer.view.uistate.model.StationInfoSongItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun TvNowPlaying(
    modifier: Modifier = Modifier,
    station: Station,
    nowPlayingItem: NowPlayingSongItem,
    playbackState: StateFlow<MediaPlayerStatus>,
    onPlaybackClick: () -> Unit,
    onFaveSongClick: (song: SongState) -> Unit,
    onRateClick: (ratingState: RatingState) -> Unit,
    onMoreClick: (item: StationInfoSongItem) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth().wrapContentHeight()
    ) {
        Row(Modifier.fillMaxWidth().height(180.dp)) {
            TvStationInfoAlbumArt(item = nowPlayingItem, imageSize = 180.dp)
            Column(Modifier.fillMaxSize()) {
                NowPlayingSongInfo(
                    modifier = Modifier.padding(start = 16.dp).height(140.dp),
                    song = nowPlayingItem.data.song,
                )
                NowPlayingControls(
                    station = station,
                    item = nowPlayingItem,
                    playbackState = playbackState,
                    onPlaybackClick = onPlaybackClick,
                    onFaveSongClick = onFaveSongClick,
                    onRateClick = onRateClick,
                    onMoreClick = onMoreClick,
                )
            }
        }
        NowPlayingProgressIndicator(
            modifier = Modifier.padding(top = 20.dp),
            nowPlayingItem = nowPlayingItem
        )
    }
}

@Composable
private fun NowPlayingSongInfo(modifier: Modifier, song: SongState) {
    Column(modifier) {
        val text = remember(song) {
            buildAnnotatedString {
                withStyle(style = ParagraphStyle(lineHeight = TvAppTypography.titleLarge.fontSize)) {
                    withStyle(style = TvAppTypography.titleLarge.toSpanStyle()) {
                        append(song.title)
                    }
                }
                withStyle(style = ParagraphStyle(lineHeight = TvAppTypography.bodyLarge.fontSize)) {
                    withStyle(style = TvAppTypography.bodyLarge.toSpanStyle()) {
                        append("\n")
                        append(song.albumName)
                    }
                }
                withStyle(style = ParagraphStyle(lineHeight = TvAppTypography.bodyMedium.fontSize)) {
                    withStyle(style = TvAppTypography.bodyMedium.toSpanStyle()) {
                        append("\n")
                        append(song.artistName)
                    }
                }
            }
        }

        Text(text = text, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun NowPlayingControls(
    modifier: Modifier = Modifier,
    station: Station,
    item: NowPlayingSongItem,
    playbackState: StateFlow<MediaPlayerStatus>,
    onPlaybackClick: () -> Unit,
    onFaveSongClick: (song: SongState) -> Unit,
    onRateClick: (ratingState: RatingState) -> Unit,
    onMoreClick: (item: StationInfoSongItem) -> Unit,
) {
    val isLoggedIn = LocalUserCredentials.current.isLoggedIn()
    val ratingState = rememberSaveable { mutableStateOf<RatingState?>(null) }
    val song = item.data.song

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp).focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            key("playback_btn") {
                PlaybackButton(
                    modifier = Modifier.saveLastFocused("playback_btn"),
                    station = station,
                    playbackState = playbackState,
                    onClick = onPlaybackClick,
                )
            }
            if (isLoggedIn) {
                key("fave_btn") {
                    FaveButton(
                        modifier = Modifier.saveLastFocused("fave_btn"),
                        fave = FaveSong(song.favorite.value),
                        onClick = { onFaveSongClick(song) }
                    )
                }
                key("rating_btn") {
                    RatingButton(
                        modifier = Modifier.saveLastFocused("rating_btn"),
                        song = song,
                        ratingState = ratingState,
                    )
                }

                key("more_btn") {
                    IconButton(
                        modifier = Modifier.saveLastFocused("more_btn"),
                        onClick = { onMoreClick(item) }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreHoriz,
                            contentDescription = stringResource(R.string.action_more),
                        )
                    }
                }
            }
        }
        TvRatingDialog(ratingState = ratingState, onRate = onRateClick)
    }
}

@Composable
private fun NowPlayingProgressIndicator(modifier: Modifier, nowPlayingItem: NowPlayingSongItem) {
    val item by rememberUpdatedState(nowPlayingItem)

    val scope = rememberCoroutineScope()
    var currentPosition by remember { mutableStateOf("") }
    val length = remember(item) { formatDuration(item.data.song.length.seconds) }
    var progress by remember { mutableFloatStateOf(0.1F) }

    LifecycleStartEffect(item) {
        val job = tickerFlow(1.seconds)
            .map { item.getCurrentPosition() }
            .takeWhile { it <= item.data.song.length }
            .onEach {
                progress = (it.toFloat() / item.data.song.length)
                currentPosition = formatDuration(it.seconds)
            }
            .launchIn(scope)
        onStopOrDispose { cancel(job) }
    }
    Column(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            progress = { progress },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Row(modifier = Modifier
            .align(Alignment.End)
            .padding(top = 8.dp)) {
            Text(text = currentPosition)
            Text(text = "/", modifier = Modifier.padding(horizontal = 4.dp))
            Text(text = length)
        }
    }
}

@Composable
private fun PlaybackButton(
    modifier: Modifier = Modifier,
    station: Station,
    playbackState: StateFlow<MediaPlayerStatus>,
    onClick: () -> Unit
) {
    val playerStatus = playbackState.collectAsStateWithLifecycle().value
    val isPlaying = playerStatus.isPlaying(station)

    IconButton(
        modifier = modifier.testTag("playback_btn"),
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            focusedContainerColor = MaterialTheme.colorScheme.onSecondaryContainer,
            focusedContentColor = MaterialTheme.colorScheme.secondaryContainer,
        )
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

@Composable
private fun FaveButton(modifier: Modifier = Modifier, fave: Fave, onClick: () -> Unit) {
    val (faveDrawable, _, faveDesc) = fave

    val colors = if (fave.favorite) {
        ButtonDefaults.colors(
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            focusedContentColor = MaterialTheme.colorScheme.secondaryContainer,
        )
    } else {
        ButtonDefaults.colors()
    }
    IconButton(modifier = modifier, onClick = onClick, colors = colors) {
        Icon(
            painter = painterResource(id = faveDrawable),
            contentDescription = stringResource(id = faveDesc),
        )
    }
}

@Composable
private fun RatingButton(
    modifier: Modifier = Modifier,
    song: SongState,
    ratingState: MutableState<RatingState?>,
) {
    val rated = song.ratingUser.floatValue > 0F
    val rating = if (rated) {
        song.ratingUser.floatValue
    } else {
        song.rating
    }
    val color = if (rated) {
        ButtonDefaults.colors(
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            focusedContentColor = MaterialTheme.colorScheme.secondaryContainer,
        )
    } else {
        ButtonDefaults.colors()
    }
    Button(
        modifier = modifier,
        onClick = { ratingState.value = RatingState(song) },
        colors = color,
    ) {
        Text(text = Formatter.formatRating(rating))
        Icon(
            painter = painterResource(R.drawable.ic_star_rate_white_18dp),
            contentDescription = null,
        )
    }
}

@PreviewTv
@Composable
private fun TvNowPlayingPreview() {
    PreviewTvTheme {
        Surface {
            TvNowPlaying(
                station = stations[0],
                nowPlayingItem = NowPlayingSongItem(
                    eventId = 3,
                    StationInfoSongData(
                        song = songStateData[2],
                        album = AlbumState(
                            id = 1,
                            name = songStateData[2].albumName
                        ),
                        requestorId = 3,
                        requestorName = "Test User"
                    ),
                    eventEndTime = System.currentTimeMillis().milliseconds.inWholeSeconds + 100,
                    apiTimeDifference = 0.seconds,
                ),
                playbackState = remember { MutableStateFlow(MediaPlayerStatus(stations[0], MediaPlayerStatus.State.Stopped)) },
                onPlaybackClick = {},
                onFaveSongClick = {},
                onRateClick = {},
                onMoreClick = {},
            )
        }
    }
}

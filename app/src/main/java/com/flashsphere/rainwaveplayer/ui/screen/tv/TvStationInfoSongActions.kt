package com.flashsphere.rainwaveplayer.ui.screen.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.ui.FaveAlbum
import com.flashsphere.rainwaveplayer.ui.FaveSong
import com.flashsphere.rainwaveplayer.ui.TvLoading
import com.flashsphere.rainwaveplayer.ui.composition.LocalTvUiSettings
import com.flashsphere.rainwaveplayer.ui.item.AlbumArt
import com.flashsphere.rainwaveplayer.ui.item.tv.TvListItem
import com.flashsphere.rainwaveplayer.ui.rating.tv.TvRatingDialog
import com.flashsphere.rainwaveplayer.ui.rating.tv.TvRatingText
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTv
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTvTheme
import com.flashsphere.rainwaveplayer.ui.screen.stationInfoData
import com.flashsphere.rainwaveplayer.ui.theme.tv.TvAppTypography
import com.flashsphere.rainwaveplayer.view.uistate.RatingState
import com.flashsphere.rainwaveplayer.view.uistate.model.AlbumState
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState
import com.flashsphere.rainwaveplayer.view.uistate.model.StationInfoSongItem
import com.flashsphere.rainwaveplayer.view.viewmodel.MainViewModel

@Composable
fun TvStationInfoSongActionsScreen(
    viewModel: MainViewModel,
    station: Station,
    item: StationInfoSongItem,
    onDismissRequest: () -> Unit,
) {
    BackHandler {
        onDismissRequest()
    }

    val screenState = viewModel.stationInfoScreenState.collectAsStateWithLifecycle().value
    if (screenState.stationInfo == null) {
        TvLoading()
        return
    }

    val updatedItem = remember { mutableStateOf(item) }
    LaunchedEffect(screenState.stationInfo) {
        snapshotFlow { screenState.stationInfo.items.toList() }.collect { items ->
            items.filterIsInstance<StationInfoSongItem>()
                .firstOrNull { item.data.songId == it.data.songId }
                ?.also { updatedItem.value = it }
        }
    }

    updatedItem.value.let {
        TvStationInfoSongActionsScreen(
            item = it,
            onVoteClick = { viewModel.voteSong(it.eventId, it.data.song.entryId) },
            onRateClick = { state -> viewModel.rateSong(station.id, it.data.song, state.rating) },
            onFaveSongClick = { viewModel.faveSong(it.data.song) },
            onFaveAlbumClick = { viewModel.faveAlbum(it.data.album) },
        )
    }
}

@Composable
private fun TvStationInfoSongActionsScreen(
    item: StationInfoSongItem,
    onVoteClick: () -> Unit,
    onRateClick: (ratingState: RatingState) -> Unit,
    onFaveSongClick: () -> Unit,
    onFaveAlbumClick: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 80.dp, end = 40.dp, top = 20.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SongInfo(modifier = Modifier.weight(0.7F), item = item)
            SongActions(
                modifier = Modifier.weight(0.3F),
                item = item,
                onVoteClick = onVoteClick,
                onRateClick = onRateClick,
                onFaveSongClick = onFaveSongClick,
                onFaveAlbumClick = onFaveAlbumClick,
            )
        }
    }
}

@Composable
private fun SongInfo(modifier: Modifier = Modifier, item: StationInfoSongItem) {
    val song = item.data.song
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(art = item.data.album.art, modifier = Modifier.size(180.dp))
        Column(modifier = Modifier.padding(start = 16.dp).width(320.dp).height(180.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1F),
                verticalArrangement = Arrangement.Center,
            ) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (song.favorite.value) {
                    val fave = FaveSong(song.favorite.value)
                    Icon(
                        modifier = Modifier.size(14.dp),
                        painter = painterResource(fave.drawable),
                        contentDescription = stringResource(fave.contentDescription),
                        tint = colorResource(fave.color)
                    )
                    Spacer(Modifier.size(12.dp))
                }
                TvRatingText(song = song, showGlobalRating = !LocalTvUiSettings.current.hideRatingsUntilRated)
            }
        }
    }
}

@Composable
private fun SongActions(
    modifier: Modifier,
    item: StationInfoSongItem,
    onVoteClick: () -> Unit,
    onRateClick: (ratingState: RatingState) -> Unit,
    onFaveSongClick: () -> Unit,
    onFaveAlbumClick: () -> Unit,
) {
    val song = item.data.song
    val focusRequester = remember { FocusRequester() }
    val hasFocus = remember { mutableStateOf(false) }
    val focusProperties: FocusProperties.() -> Unit = remember { {
        left = FocusRequester.Cancel
        right = FocusRequester.Cancel
        previous = FocusRequester.Cancel
        next = FocusRequester.Cancel
    } }
    LazyColumn(
        modifier = modifier.focusRequester(focusRequester)
            .onFocusChanged { hasFocus.value = it.hasFocus }
            .onGloballyPositioned {
                if (!hasFocus.value) {
                    focusRequester.requestFocus()
                }
            },
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        if (song.ratingAllowed) {
            item(key = "rate") {
                RateAction(modifier = Modifier.focusProperties(focusProperties),
                    song = song, onRate = onRateClick)
            }
        }
        if (song.votingAllowed && !song.voted.value) {
            item(key = "vote") {
                VoteSongAction(modifier = Modifier.focusProperties(focusProperties),
                    onClick = onVoteClick)
            }
        }
        item(key = "fave_song") {
            FaveSongAction(modifier = Modifier.focusProperties(focusProperties),
                song = item.data.song, onClick = onFaveSongClick)
        }
        item(key = "fave_album") {
            FaveAlbumAction(modifier = Modifier.focusProperties(focusProperties),
                album = item.data.album, onClick = onFaveAlbumClick)
        }
    }
}

@Composable
private fun RateAction(
    modifier: Modifier = Modifier,
    song: SongState,
    onRate: (ratingState: RatingState) -> Unit,
) {
    Box {
        val ratingState = remember { mutableStateOf<RatingState?>(null) }
        TvListItem(
            modifier = modifier,
            onClick = { ratingState.value = RatingState(song.id, song.title, song.ratingUser.floatValue) },
            text = stringResource(R.string.action_rate),
        )
        TvRatingDialog(
            ratingState = ratingState,
            onRate = onRate,
        )
    }
}

@Composable
private fun VoteSongAction(modifier: Modifier = Modifier, onClick: () -> Unit) {
    TvListItem(
        modifier = modifier,
        onClick = onClick,
        text = stringResource(R.string.action_vote),
    )
}

@Composable
private fun FaveSongAction(modifier: Modifier = Modifier, song: SongState, onClick: () -> Unit) {
    val fave = FaveSong(song.favorite.value)
    TvListItem(
        modifier = modifier,
        onClick = onClick,
        text = stringResource(fave.contentDescription),
    )
}

@Composable
private fun FaveAlbumAction(modifier: Modifier = Modifier, album: AlbumState, onClick: () -> Unit) {
    val fave = FaveAlbum(album.favorite.value)
    TvListItem(
        modifier = modifier,
        onClick = onClick,
        text = stringResource(fave.contentDescription),
    )
}

private class TvStationInfoSongActionsScreenPreviewProvider : PreviewParameterProvider<StationInfoSongItem> {
    override val values: Sequence<StationInfoSongItem> = sequenceOf(
        stationInfoData.comingUp[0].items[0],
        stationInfoData.previouslyPlayed.items[0],
    )
}

@PreviewTv
@Composable
private fun TvStationInfoSongActionsScreenPreview(
    @PreviewParameter(TvStationInfoSongActionsScreenPreviewProvider::class) item: StationInfoSongItem
) {
    PreviewTvTheme {
        TvStationInfoSongActionsScreen(
            item = item,
            onVoteClick = {},
            onRateClick = {},
            onFaveSongClick = {},
            onFaveAlbumClick = {},
        )
    }
}

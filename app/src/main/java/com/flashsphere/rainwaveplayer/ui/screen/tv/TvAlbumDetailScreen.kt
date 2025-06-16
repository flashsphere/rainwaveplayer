package com.flashsphere.rainwaveplayer.ui.screen.tv

import android.widget.RatingBar
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.ui.TvErrorWithRetry
import com.flashsphere.rainwaveplayer.ui.TvLoading
import com.flashsphere.rainwaveplayer.ui.composition.LastFocused
import com.flashsphere.rainwaveplayer.ui.composition.LocalLastFocused
import com.flashsphere.rainwaveplayer.ui.composition.LocalTvUiSettings
import com.flashsphere.rainwaveplayer.ui.item.AlbumArt
import com.flashsphere.rainwaveplayer.ui.item.tv.TvSongListItem
import com.flashsphere.rainwaveplayer.ui.navigation.AlbumDetail
import com.flashsphere.rainwaveplayer.ui.saveLastFocused
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTv
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTvTheme
import com.flashsphere.rainwaveplayer.ui.screen.RatingHistogram
import com.flashsphere.rainwaveplayer.ui.screen.albumStateData
import com.flashsphere.rainwaveplayer.ui.theme.tv.TvAppTypography
import com.flashsphere.rainwaveplayer.util.Formatter.formatRating
import com.flashsphere.rainwaveplayer.view.uistate.model.AlbumState
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState
import com.flashsphere.rainwaveplayer.view.viewmodel.AlbumScreenViewModel
import kotlinx.coroutines.flow.StateFlow
import androidx.appcompat.R as AppCompatR

@Composable
fun TvAlbumDetailScreen(
    navController: NavHostController,
    viewModel: AlbumScreenViewModel,
    stationFlow: StateFlow<Station?>,
    detail: AlbumDetail,
) {
    val station = stationFlow.collectAsStateWithLifecycle().value ?: return
    LaunchedEffect(detail) {
        viewModel.getAlbum(station, detail)
    }
    LifecycleStartEffect(Unit) {
        viewModel.subscribeStateChangeEvents()
        onStopOrDispose { viewModel.unsubscribeStateChangeEvents() }
    }

    val screenState = viewModel.albumScreenState.collectAsStateWithLifecycle().value
    Surface {
        if (screenState.loading) {
            TvLoading()
        } else if (screenState.error != null) {
            TvErrorWithRetry(
                text = screenState.error.getMessage(LocalContext.current, stringResource(R.string.error_connection)),
                onRetry = { viewModel.getAlbum() },
            )
        } else if (screenState.loaded && screenState.album != null) {
            TvAlbumDetailScreen(
                album = screenState.album,
                onSongClick = { song -> viewModel.requestSong(song) },
                onFaveSongClick = { song -> viewModel.faveSong(song) },
            )
        }
    }
}

@Composable
private fun TvAlbumDetailScreen(
    album: AlbumState,
    onSongClick: (song: SongState) -> Unit,
    onFaveSongClick: (song: SongState) -> Unit,
) {
    val lastFocused = LocalLastFocused.current
    LifecycleStartEffect(album) {
        val songs = album.songs
        if (!songs.isEmpty() && lastFocused.value.tag == null) {
            lastFocused.value = LastFocused(songs[0].key, true)
        } else if (!lastFocused.value.shouldRequestFocus) {
            lastFocused.value = lastFocused.value.copy(shouldRequestFocus = true)
        }
        onStopOrDispose {}
    }
    Row(modifier = Modifier.padding(start = 80.dp, end = 40.dp).fillMaxSize()) {
        Column(Modifier.padding(vertical = 20.dp).weight(0.5F)) {
            AlbumInfo(album = album)
        }
        SongList(
            modifier = Modifier.padding(start = 40.dp).weight(0.5F),
            songs = album.songs,
            onSongClick = onSongClick,
            onFaveSongClick = onFaveSongClick,
        )
    }
}

@Composable
private fun SongList(
    modifier: Modifier,
    songs: SnapshotStateList<SongState>,
    onSongClick: (song: SongState) -> Unit,
    onFaveSongClick: (song: SongState) -> Unit,
) {
    LazyColumn(
        modifier = modifier.focusGroup(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(top = 40.dp, bottom = 20.dp),
    ) {
        itemsIndexed(
            items = songs,
            key = { _, item -> item.key }
        ) { _, item ->
            TvSongListItem(
                modifier = Modifier.animateItem(),
                songItemModifier = Modifier.saveLastFocused(item.key),
                song = item,
                showGlobalRating = !LocalTvUiSettings.current.hideRatingsUntilRated,
                onClick = { onSongClick(item) },
                onFaveClick = { onFaveSongClick(item) },
            )
        }
    }
}

@Composable
private fun AlbumInfo(album: AlbumState) {
    Row {
        AlbumArt(art = album.art, modifier = Modifier.size(180.dp))
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(text = album.name,
                style = TvAppTypography.titleLarge.copy(
                    lineHeight = TvAppTypography.titleLarge.lineHeight
                ))

            if (album.ratingUser > 0F) {
                Text(
                    text = stringResource(R.string.rating_user, formatRating(album.ratingUser)),
                    style = TvAppTypography.bodySmall.copy(
                        lineHeight = TvAppTypography.bodySmall.lineHeight
                    ),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            val cooldownText = if (album.cool) {
                stringResource(R.string.album_on_cooldown)
            } else if (album.songsOnCooldown) {
                stringResource(R.string.some_songs_on_cooldown)
            } else {
                null
            }
            if (cooldownText != null) {
                Text(
                    modifier = Modifier.padding(top = 16.dp)
                        .background(colorResource(id = R.color.cooldown_background)),
                    text = cooldownText,
                    style = TvAppTypography.bodySmall.copy(
                        lineHeight = TvAppTypography.bodySmall.lineHeight
                    ),
                )
            }
            Row(
                modifier = Modifier.padding(top = 20.dp).fillMaxWidth().height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = formatRating(album.rating),
                    style = TvAppTypography.titleLarge,
                    modifier = Modifier
                        .wrapContentSize()
                        .alignByBaseline()
                )
                Column(modifier = Modifier.padding(start = 8.dp).alignByBaseline()) {
                    AndroidView(
                        factory = { ctx ->
                            RatingBar(ctx, null, AppCompatR.attr.ratingBarStyleSmall,
                                AppCompatR.style.Widget_AppCompat_RatingBar_Small).apply {
                                setIsIndicator(true)
                                numStars = 5
                                stepSize = 0.1F
                            }
                        },
                        update = { view ->
                            view.rating = album.rating
                        },
                        modifier = Modifier.wrapContentSize()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = album.ratingCount.toString(),
                            style = TvAppTypography.bodySmall,
                            fontSize = 11.sp,
                            modifier = Modifier.wrapContentSize()
                        )
                        Spacer(Modifier.size(2.dp))
                        Icon(imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .size(12.dp)
                                .padding(0.dp)
                        )
                    }
                }
                RatingHistogram(
                    modifier = Modifier.padding(start = 8.dp),
                    album = album,
                )
            }
        }
    }
}

private class TvAlbumDetailScreenPreviewProvider : PreviewParameterProvider<AlbumState> {
    override val values = sequenceOf(albumStateData[0], albumStateData[1])
}

@PreviewTv
@Composable
private fun TvAlbumDetailScreenPreview(@PreviewParameter(TvAlbumDetailScreenPreviewProvider::class) album: AlbumState) {
    PreviewTvTheme {
        Surface {
            TvAlbumDetailScreen(
                album = album,
                onFaveSongClick = {},
                onSongClick = {},
            )
        }
    }
}

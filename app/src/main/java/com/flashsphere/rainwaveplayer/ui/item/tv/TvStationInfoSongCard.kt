package com.flashsphere.rainwaveplayer.ui.item.tv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.FaveSong
import com.flashsphere.rainwaveplayer.ui.composition.LocalTvUiSettings
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.rating.tv.TvRatingText
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTv
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTvTheme
import com.flashsphere.rainwaveplayer.ui.screen.stationInfoData
import com.flashsphere.rainwaveplayer.ui.theme.tv.TvAppTypography
import com.flashsphere.rainwaveplayer.util.UserCredentials
import com.flashsphere.rainwaveplayer.view.uistate.model.StationInfoSongItem

@Composable
fun TvStationInfoSongCard(
    modifier: Modifier = Modifier,
    item: StationInfoSongItem,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val cardWidth = LocalUiScreenConfig.current.tvCardWidth
    val imageSize = LocalUiScreenConfig.current.tvCardWidth / 2
    val song = item.data.song

    val backgroundColor = if (song.voted.value) {
        colorResource(id = R.color.voted)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Card(
        modifier = modifier.width(cardWidth).height(imageSize),
        onClick = onClick,
        onLongClick = onLongClick,
        colors = CardDefaults.colors(
            containerColor = backgroundColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = backgroundColor,
            focusedContentColor = MaterialTheme.colorScheme.onSurface,
            pressedContainerColor = backgroundColor,
            pressedContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(Modifier.fillMaxSize()) {
            TvStationInfoAlbumArt(item = item, imageSize = imageSize)
            Column(Modifier.padding(8.dp)) {
                Box(modifier = Modifier.padding(bottom = 4.dp).fillMaxWidth().weight(1F)) {
                    val text = remember(song) {
                        buildAnnotatedString {
                            withStyle(style = ParagraphStyle(lineHeight = TvAppTypography.bodyMedium.fontSize)) {
                                withStyle(style = TvAppTypography.bodyMedium.toSpanStyle()) {
                                    append(song.title)
                                }
                            }
                            withStyle(style = ParagraphStyle(lineHeight = TvAppTypography.bodySmall.fontSize)) {
                                withStyle(style = TvAppTypography.bodySmall.toSpanStyle()) {
                                    append("\n")
                                    append(song.albumName)
                                }
                            }
                            withStyle(style = ParagraphStyle(lineHeight = TvAppTypography.bodySmall.fontSize)) {
                                withStyle(style = TvAppTypography.bodySmall.toSpanStyle()) {
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
                    }
                    Spacer(Modifier.weight(1F))
                    TvRatingText(song = song, showGlobalRating = !LocalTvUiSettings.current.hideRatingsUntilRated)
                }
            }
        }
    }
}

@PreviewTv
@Composable
private fun TvStationInfoSongCardPreview(
    @PreviewParameter(StationInfoSongItemPreviewProvider::class) item: StationInfoSongItem,
) {
    PreviewTvTheme(UserCredentials(2, "")) {
        Surface {
            TvStationInfoSongCard(
                modifier = Modifier,
                item = item,
                onClick = {},
                onLongClick = {},
            )
        }
    }
}

private class StationInfoSongItemPreviewProvider : PreviewParameterProvider<StationInfoSongItem> {
    override val values: Sequence<StationInfoSongItem> = sequenceOf(
        stationInfoData.comingUp[0].items[0],
        stationInfoData.previouslyPlayed.items[0],
        stationInfoData.previouslyPlayed.items[1],
        stationInfoData.nowPlaying.item,
    )
}

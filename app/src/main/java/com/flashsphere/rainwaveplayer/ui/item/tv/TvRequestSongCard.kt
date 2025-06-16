package com.flashsphere.rainwaveplayer.ui.item.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.item.AlbumArt
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTv
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTvTheme
import com.flashsphere.rainwaveplayer.ui.theme.tv.TvAppTypography
import com.flashsphere.rainwaveplayer.util.UserCredentials
import com.flashsphere.rainwaveplayer.view.uistate.model.RequestState

@Composable
fun TvRequestSongCard(
    modifier: Modifier,
    item: RequestState,
    cardWidth: Dp = LocalUiScreenConfig.current.tvCardWidth,
    imageSize: Dp = cardWidth / 2,
    onClick: () -> Unit,
    onLongClick: () -> Unit = onClick,
) {
    Card(
        modifier = modifier.width(cardWidth).height(imageSize),
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        TvRequestSong(modifier = Modifier.fillMaxSize(), item = item, imageSize = imageSize)
    }
}

@Composable
fun TvRequestSong(
    modifier: Modifier = Modifier,
    item: RequestState,
    imageSize: Dp,
) {
    val context = LocalContext.current
    val cooldownColor = colorResource(R.color.cooldown_text)
    val cooldownText = item.getCooldownText(context)
    val cooldownBg = if (cooldownText != null) {
        Modifier.background(colorResource(R.color.cooldown_background))
    } else {
        Modifier
    }

    Row(modifier.height(imageSize)) {
        AlbumArt(art = item.art, modifier = Modifier.size(imageSize))
        Box(modifier = Modifier.then(cooldownBg).padding(8.dp).fillMaxSize()) {
            val text = remember(item) {
                buildAnnotatedString {
                    withStyle(style = ParagraphStyle(lineHeight = TvAppTypography.bodyMedium.fontSize)) {
                        withStyle(style = TvAppTypography.bodyMedium.toSpanStyle()) {
                            append(item.title)
                        }
                    }
                    withStyle(style = ParagraphStyle(lineHeight = TvAppTypography.bodySmall.fontSize)) {
                        withStyle(style = TvAppTypography.bodySmall.toSpanStyle()) {
                            append("\n")
                            append(item.albumName)
                        }
                    }
                    cooldownText?.let {
                        withStyle(style = ParagraphStyle(lineHeight = TvAppTypography.bodySmall.fontSize)) {
                            withStyle(style = TvAppTypography.bodySmall.copy(color = cooldownColor).toSpanStyle()) {
                                append("\n")
                                append(it)
                            }
                        }
                    }
                }
            }
            Text(text = text, overflow = TextOverflow.Ellipsis)
        }
    }
}

@PreviewTv
@Composable
private fun TvRequestSongCardPreview(
    @PreviewParameter(RequestItemPreviewProvider::class) item: RequestState,
) {
    PreviewTvTheme(UserCredentials(2, "")) {
        Surface {
            TvRequestSongCard(
                modifier = Modifier,
                item = item,
                onClick = {},
                onLongClick = {},
            )
        }
    }
}

private class RequestItemPreviewProvider : PreviewParameterProvider<RequestState> {
    override val values: Sequence<RequestState> = sequenceOf(RequestState(
            id = 3,
            title = "Thine Wrath...",
            art = "abc.jpg",
            songId = 3,
            albumName = "The Binding of Isaac",
            valid = false,
            good = true,
            cooldown = true,
            cooldownEndTime = 1722776461,
            electionBlocked = false,
            electionBlockedBy = "group",
            stationName = "Game",
        ),
    )
}

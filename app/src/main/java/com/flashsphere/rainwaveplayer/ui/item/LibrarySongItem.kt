package com.flashsphere.rainwaveplayer.ui.item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.FaveSong
import com.flashsphere.rainwaveplayer.ui.Tooltip
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.rating.RatingText
import com.flashsphere.rainwaveplayer.ui.screen.Preview
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTheme
import com.flashsphere.rainwaveplayer.ui.screen.songStateData
import com.flashsphere.rainwaveplayer.ui.theme.AppTypography
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState

@Composable
fun LibrarySongItem(
    song: SongState,
    showGlobalRating: Boolean,
    onClick: (song: SongState) -> Unit,
    onFaveClick: (song: SongState) -> Unit,
) {
    LibrarySongItem(song = song, showGlobalRating = showGlobalRating, onClick = onClick, onFaveClick = onFaveClick) {
        Text(
            text = song.title,
            style = AppTypography.bodyMedium,
        )
    }
}

@Composable
fun LibrarySongItem(
    modifier: Modifier = Modifier,
    song: SongState,
    showGlobalRating: Boolean,
    onClick: ((song: SongState) -> Unit)?,
    onFaveClick: (song: SongState) -> Unit,
    titleContent: @Composable (ColumnScope.() -> Unit),
) {
    val (faveDrawable, faveColor, faveDesc) = FaveSong(song.favorite.value)

    val cooldownBgColor = if (song.cool) {
        Modifier.background(colorResource(id = R.color.cooldown_background))
    } else {
        Modifier
    }

    Row(modifier = modifier.then(cooldownBgColor)
        .fillMaxWidth()
        .heightIn(min = LocalUiScreenConfig.current.listItemLineHeight)
        .height(IntrinsicSize.Min)
        .clickable(song.requestable && onClick != null, onClick = { onClick?.invoke(song) }),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tooltip(stringResource(id = faveDesc)) {
            Box(modifier = Modifier
                .width(LocalUiScreenConfig.current.listItemLineHeight)
                .fillMaxHeight()
                .clickable { onFaveClick(song) }
            ) {
                Icon(painter = painterResource(id = faveDrawable),
                    tint = colorResource(id = faveColor),
                    contentDescription = stringResource(id = faveDesc),
                    modifier = Modifier.wrapContentSize().align(Alignment.Center)
                )
            }
        }
        Column(modifier = Modifier
            .padding(LocalUiScreenConfig.current.itemPadding)
            .weight(1F)
            .wrapContentHeight()) {
            titleContent()
        }
        RatingText(song = song, showGlobalRating = showGlobalRating, modifier = Modifier.wrapContentSize()
            .padding(end = 12.dp))
    }
}

@Preview
@Composable
private fun LibrarySongItemPreview(
    @PreviewParameter(SongItemStatePreviewProvider::class) song: SongState
) {
    PreviewTheme {
        Surface {
            LibrarySongItem(song = song, showGlobalRating = true, onClick = {}, onFaveClick = {})
        }
    }
}

private class SongItemStatePreviewProvider : PreviewParameterProvider<SongState> {
    override val values = sequenceOf(songStateData[3], songStateData[1])
}

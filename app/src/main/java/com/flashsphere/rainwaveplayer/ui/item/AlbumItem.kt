package com.flashsphere.rainwaveplayer.ui.item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import com.flashsphere.rainwaveplayer.ui.FaveAlbum
import com.flashsphere.rainwaveplayer.ui.Tooltip
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.rating.RatingText
import com.flashsphere.rainwaveplayer.ui.screen.Preview
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTheme
import com.flashsphere.rainwaveplayer.ui.screen.albumStateData
import com.flashsphere.rainwaveplayer.ui.theme.AppTypography
import com.flashsphere.rainwaveplayer.view.uistate.model.AlbumState

@Composable
fun AlbumItem(
    modifier: Modifier = Modifier,
    album: AlbumState,
    showGlobalRating: Boolean,
    onClick: (album: AlbumState) -> Unit,
    onFaveClick: (album: AlbumState) -> Unit,
) {
    val (faveDrawable, faveColor, faveDesc) = FaveAlbum(album.favorite.value)

    val cooldownBgColor = if (album.cool) {
        Modifier.background(colorResource(id = R.color.cooldown_background))
    } else {
        Modifier
    }

    Row(modifier = modifier.then(cooldownBgColor)
            .fillMaxWidth()
            .heightIn(min = LocalUiScreenConfig.current.listItemLineHeight)
            .height(IntrinsicSize.Min)
            .clickable { onClick(album) },
            verticalAlignment = Alignment.CenterVertically,
    ) {
        Tooltip(stringResource(id = faveDesc)) {
            Box(modifier = Modifier
                .width(LocalUiScreenConfig.current.listItemLineHeight)
                .fillMaxHeight()
                .clickable { onFaveClick(album) }
            ) {
                Icon(painter = painterResource(id = faveDrawable),
                    tint = colorResource(id = faveColor),
                    contentDescription = stringResource(id = faveDesc),
                    modifier = Modifier.wrapContentSize().align(Alignment.Center)
                )
            }
        }
        Text(text = album.name,
            style = AppTypography.bodyMedium,
            modifier = Modifier
                .padding(LocalUiScreenConfig.current.itemPadding)
                .weight(1F)
                .wrapContentHeight()
        )
        RatingText(album = album, showGlobalRating = showGlobalRating, modifier = Modifier.padding(end = 12.dp))
    }
}

private class AlbumItemPreviewProvider : PreviewParameterProvider<AlbumState> {
    override val values = sequenceOf(albumStateData[0], albumStateData[1])
}

@Preview
@Composable
private fun AlbumItemPreview(@PreviewParameter(AlbumItemPreviewProvider::class) album: AlbumState) {
    PreviewTheme {
        Surface {
            AlbumItem(album = album, showGlobalRating = true, onClick = {}, onFaveClick = {})
        }
    }
}

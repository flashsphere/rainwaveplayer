package com.flashsphere.rainwaveplayer.ui.item

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.screen.Preview
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTheme
import com.flashsphere.rainwaveplayer.ui.screen.songStateData
import com.flashsphere.rainwaveplayer.ui.theme.AppTypography
import com.flashsphere.rainwaveplayer.view.uistate.model.AlbumState

@Composable
fun AlbumHeaderItem(album: AlbumState, onClick: ((album: AlbumState) -> Unit)?) {
    Column(Modifier.padding(top = 16.dp)) {
        Row(modifier = Modifier
            .heightIn(LocalUiScreenConfig.current.listItemLineHeight)
            .clickable(enabled = onClick != null, onClick = {
                onClick?.invoke(album)
            })
            .padding(start = LocalUiScreenConfig.current.listItemPadding,
                top = LocalUiScreenConfig.current.itemPadding,
                end = LocalUiScreenConfig.current.itemPadding,
                bottom = LocalUiScreenConfig.current.itemPadding)
            .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(modifier = Modifier.weight(1F),
                text = album.name, style = AppTypography.bodyMedium)
            if (onClick != null) {
                Icon(modifier = Modifier.padding(start = LocalUiScreenConfig.current.itemPadding),
                    imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                    contentDescription = null)
            }
        }
        HorizontalDivider()
    }
}

private class AlbumHeaderItemPreviewProvider : PreviewParameterProvider<AlbumState> {
    override val values: Sequence<AlbumState> = sequenceOf(AlbumState(
        id = 1,
        name = songStateData[3].albumName
    ), AlbumState(
        id = 2,
        name = songStateData[4].albumName
    ))
}

@Preview
@Composable
private fun AlbumHeaderItemPreview(
    @PreviewParameter(AlbumHeaderItemPreviewProvider::class) album: AlbumState
) {
    PreviewTheme {
        Surface {
            AlbumHeaderItem(album = album, onClick = {})
        }
    }
}

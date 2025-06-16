package com.flashsphere.rainwaveplayer.ui.item

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.screen.Preview
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTheme
import com.flashsphere.rainwaveplayer.ui.theme.AppTypography
import com.flashsphere.rainwaveplayer.view.uistate.model.RequestLineState

@Composable
fun RequestLineItem(item: RequestLineState) {
    val listItemPadding = LocalUiScreenConfig.current.listItemPadding
    val itemPadding = LocalUiScreenConfig.current.itemPadding
    val title = StringBuilder().append(item.position).append(". ").append(item.username).toString()
    val albumName = item.albumName ?: stringResource(id = R.string.no_song_selected)

    Column(modifier = Modifier
        .padding(start = listItemPadding, top = itemPadding, end = listItemPadding,
            bottom = itemPadding)
        .fillMaxWidth()
    ) {
        Text(text = title, style = AppTypography.bodyLarge,
            lineHeight = AppTypography.bodyLarge.fontSize)
        Text(text = albumName, style = AppTypography.bodyMedium,
            lineHeight = AppTypography.bodyMedium.fontSize,
            modifier = Modifier.padding(start = 20.dp, top = 4.dp))
        if (item.songTitle != null) {
            Text(text = item.songTitle, style = AppTypography.bodyMedium,
                lineHeight = AppTypography.bodyMedium.fontSize,
                modifier = Modifier.padding(start = 20.dp, top = 4.dp))
        }
    }
}

private class RequestLineItemPreviewProvider : PreviewParameterProvider<RequestLineState> {
    override val values = sequenceOf(
        RequestLineState(
            userId = 444,
            username = "Laharl",
            position = 1,
            albumName = null,
            songTitle = null,
        ), RequestLineState(
            userId = 333,
            username = "some username some username some username some username some username ",
            position = 2,
            albumName = "Streets of Rage",
            songTitle = "The Street of Rage",
        )
    )
}

@Preview
@Composable
private fun RequestLineItemPreview(
    @PreviewParameter(RequestLineItemPreviewProvider::class) requestLine: RequestLineState
) {
    PreviewTheme {
        Surface {
            RequestLineItem(item = requestLine)
        }
    }
}

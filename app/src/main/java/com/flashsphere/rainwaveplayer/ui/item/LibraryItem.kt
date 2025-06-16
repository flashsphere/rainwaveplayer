package com.flashsphere.rainwaveplayer.ui.item

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.screen.Preview
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTheme
import com.flashsphere.rainwaveplayer.ui.theme.AppTypography

@Composable
fun LibraryItem(modifier: Modifier = Modifier, title: String, onClick: (() -> Unit)? = {}) {
    Box(modifier = modifier
        .heightIn(LocalUiScreenConfig.current.listItemLineHeight)
        .clickable(enabled = onClick != null, onClick = {
            onClick?.invoke()
        })
        .padding(start = LocalUiScreenConfig.current.listItemPadding,
            top = LocalUiScreenConfig.current.itemPadding,
            end = LocalUiScreenConfig.current.listItemPadding,
            bottom = LocalUiScreenConfig.current.itemPadding)
        .fillMaxWidth()
    ) {
        Text(text = title, style = AppTypography.bodyMedium,
            modifier = Modifier.align(Alignment.CenterStart))
    }
}

private class LibraryPreviewProvider : PreviewParameterProvider<String> {
    override val values = sequenceOf(
        "Library item Library item Library item Library item Library item Library item",
        "Library item"
    )
}

@Preview
@Composable
private fun LibraryItemPreview(@PreviewParameter(LibraryPreviewProvider::class) title: String) {
    PreviewTheme {
        Surface {
            LibraryItem(title = title)
        }
    }
}

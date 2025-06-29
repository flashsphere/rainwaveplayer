package com.flashsphere.rainwaveplayer.ui.item

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.screen.Preview
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTheme
import com.flashsphere.rainwaveplayer.ui.theme.AppTypography

@Composable
fun OneLineWithSubtitleText(params: OneLineWithSubtitleParams) {
    Column(
        modifier = Modifier
            .clickable(enabled = params.onClick != null, onClick = {
                params.onClick?.invoke()
            })
            .padding(LocalUiScreenConfig.current.listItemPadding)
            .fillMaxWidth()
    ) {

        Text(params.text, style = AppTypography.bodyMedium)
        if (!params.subtitle.isNullOrBlank()) {
            Text(params.subtitle, style = AppTypography.bodySmall)
        }
    }
}

data class OneLineWithSubtitleParams(
    val text: String,
    val subtitle: String? = null,
    val onClick: (() -> Unit)? = null,
)

private class OneLineWithSubtitleParamsProvider : PreviewParameterProvider<OneLineWithSubtitleParams> {
    override val values = sequenceOf(OneLineWithSubtitleParams(
        "One line text",
        "Subtitle"
    ), OneLineWithSubtitleParams(
        "One line text",
        ""
    ))
}

@Preview
@Composable
private fun OneLineWithSubtitleTextPreview(
    @PreviewParameter(OneLineWithSubtitleParamsProvider::class) params: OneLineWithSubtitleParams,
) {
    PreviewTheme {
        Surface {
            OneLineWithSubtitleText(params)
        }
    }
}

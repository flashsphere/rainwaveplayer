package com.flashsphere.rainwaveplayer.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import com.flashsphere.rainwaveplayer.ui.theme.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Tooltip(
    text: String,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            PlainTooltip { Text(text = text, style = AppTypography.bodyMedium) }
        },
        state = rememberTooltipState()
    ) {
        content()
    }
}

package com.flashsphere.rainwaveplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.flashsphere.rainwaveplayer.ui.theme.AppTypography

@Composable
fun AppBarTitle(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    subtitle: @Composable (() -> Unit) = {},
    onClick: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()
        .clickable(interactionSource = null, indication = null, enabled = onClick != null,
            onClick = { onClick?.invoke() })
    ) {
        title()
        subtitle()
    }
}

@Composable
fun AppBarTitle(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val titleMaxLines = if (subtitle == null) {
        2
    } else {
        1
    }
    AppBarTitle(
        modifier = modifier,
        title = { AppBarTitleText(text = title, maxLines = titleMaxLines) },
        subtitle = {
            if (subtitle != null) {
                AppBarSubtitleText(text = subtitle)
            }
        },
        onClick = onClick,
    )
}

@Composable
fun AppBarTitleText(
    modifier: Modifier = Modifier,
    text: String,
    maxLines: Int,
) {
    Text(modifier = modifier, text = text, style = AppTypography.titleLarge,
        maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@Composable
fun AppBarSubtitleText(
    modifier: Modifier = Modifier,
    text: String,
) {
    Text(modifier = modifier, text = text, style = AppTypography.titleSmall,
        maxLines = 1, overflow = TextOverflow.Ellipsis)
}

package com.flashsphere.rainwaveplayer.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.mediarouter.app.MediaRouteButton
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.Tooltip
import com.flashsphere.rainwaveplayer.ui.theme.AppTypography
import com.google.android.gms.cast.framework.CastButtonFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.min

@Composable
fun CastButton() {
    Tooltip(stringResource(R.string.action_cast)) {
        AndroidView(
            factory = { ctx ->
                MediaRouteButton(ctx).also {
                    CastButtonFactory.setUpMediaRouteButton(ctx, it)
                }
            }
        )
    }
}

@Composable
fun CastInfo(modifier: Modifier = Modifier, castState: MutableStateFlow<String>) {
    val message = castState.collectAsStateWithLifecycle().value
    if (message.isEmpty()) return

    Surface(
        modifier = modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .wrapContentWidth()
            .heightIn(min = 56.dp),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .wrapContentSize()
                .padding(start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = message,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = AppTypography.bodyMedium,
                modifier = Modifier
                    .layout { measurable, constraints ->
                        var maxWidthExcludingFab = constraints.maxWidth - 72.dp.roundToPx()
                        if (maxWidthExcludingFab < 0) {
                            maxWidthExcludingFab = constraints.maxWidth
                        }
                        val maxContainerWidth = min(maxWidthExcludingFab, 400.dp.roundToPx())
                        val measuredWidth = measurable.measure(constraints).width
                        val placeable = measurable.measure(
                            constraints.copy(maxWidth = min(measuredWidth, maxContainerWidth))
                        )
                        layout(placeable.width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
            )
        }
    }
}

@Preview
@Composable
private fun CastInfoPreview() {
    PreviewTheme {
        CastInfo(castState = MutableStateFlow("Casting to SHIELD - Playing"))
    }
}

package com.flashsphere.rainwaveplayer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import com.flashsphere.rainwaveplayer.BuildConfig
import com.flashsphere.rainwaveplayer.util.PixelizeTransformation

@Composable
fun CoilImage(
    modifier: Modifier = Modifier,
    image: String?,
    contentDescription: String? = null,
    contentScale: ContentScale,
    placeholder: Painter? = null,
    fallback: Painter? = null,
    error: Painter? = null,
) {
    val request: Any? = if (BuildConfig.DEBUG && BuildConfig.PIXELIZE_IMAGE) {
        ImageRequest.Builder(LocalContext.current)
            .data(image)
            .transformations(listOf(PixelizeTransformation(0.1F)))
            .build()
    } else {
        image
    }

    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        fallback = fallback,
        error = error,
        placeholder = placeholder,
        modifier = modifier,
    )
}

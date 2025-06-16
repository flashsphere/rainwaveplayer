package com.flashsphere.rainwaveplayer.ui.item

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.CoilImage

@Composable
fun AlbumArt(
    modifier: Modifier,
    art: String,
) {
    CoilImage(
        image = art,
        contentScale = ContentScale.Crop,
        placeholder = if (LocalInspectionMode.current) painterResource(R.drawable.ic_rainwave_24dp) else null,
        modifier = modifier,
    )
}

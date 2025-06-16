package com.flashsphere.rainwaveplayer.ui.item.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flashsphere.rainwaveplayer.ui.OtherRequestor
import com.flashsphere.rainwaveplayer.ui.SelfRequestor
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.composition.LocalUserCredentials
import com.flashsphere.rainwaveplayer.ui.item.AlbumArt
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTv
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTvTheme
import com.flashsphere.rainwaveplayer.ui.screen.stationInfoData
import com.flashsphere.rainwaveplayer.ui.theme.SansSerifCondensed
import com.flashsphere.rainwaveplayer.ui.theme.tv.TvAppTypography
import com.flashsphere.rainwaveplayer.view.uistate.model.StationInfoSongItem

@Composable
fun TvStationInfoAlbumArt(
    item: StationInfoSongItem,
    imageSize: Dp,
) {
    val requestorId = item.data.requestorId
    val requestorName = item.data.requestorName

    Box(Modifier.size(imageSize)) {
        AlbumArt(art = item.data.album.art, modifier = Modifier.size(imageSize))

        if (requestorName.isNotEmpty()) {
            val userCredentials = LocalUserCredentials.current
            val (requestorBgColor, requestorTextColor, requestorText) = if (userCredentials?.userId == requestorId) {
                SelfRequestor
            } else {
                OtherRequestor
            }

            val lineHeightDp: Dp = with(LocalDensity.current) {
                TvAppTypography.bodySmall.lineHeight.toDp()
            }

            Text(text = stringResource(id = requestorText).uppercase(),
                color = colorResource(id = requestorTextColor),
                style = TvAppTypography.bodySmall,
                fontFamily = SansSerifCondensed,
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .width(imageSize)
                    .rotate(-90F)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val x = (placeable.width - placeable.height) / 2
                        val y = placeable.width / 2 - placeable.height / 2
                        layout(placeable.width, placeable.height) {
                            placeable.place(-x, -y)
                        }
                    }
                    .background(colorResource(id = requestorBgColor))
                    .padding(end = lineHeightDp))
            Text(
                text = requestorName.uppercase(),
                color = colorResource(id = requestorTextColor),
                style = TvAppTypography.bodySmall,
                fontFamily = SansSerifCondensed,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .width(imageSize)
                    .background(colorResource(id = requestorBgColor))
                    .padding(start = lineHeightDp)
            )
        }
    }
}

@PreviewTv
@Composable
private fun TvStationInfoAlbumArtPreview() {
    PreviewTvTheme {
        Surface {
            TvStationInfoAlbumArt(
                item = stationInfoData.nowPlaying.item,
                imageSize = LocalUiScreenConfig.current.tvCardWidth / 2,
            )
        }
    }
}

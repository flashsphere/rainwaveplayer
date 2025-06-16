package com.flashsphere.rainwaveplayer.ui.rating

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.screen.Preview
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTheme
import com.flashsphere.rainwaveplayer.ui.screen.albumStateData
import com.flashsphere.rainwaveplayer.ui.screen.songStateData
import com.flashsphere.rainwaveplayer.ui.theme.AppTypography
import com.flashsphere.rainwaveplayer.util.Formatter
import com.flashsphere.rainwaveplayer.view.uistate.model.AlbumState
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState

@Composable
fun RatingText(modifier: Modifier = Modifier, song: SongState, showGlobalRating: Boolean) {
    val rated = song.ratingUser.floatValue > 0F
    val rating = if (rated) {
        song.ratingUser.floatValue
    } else if (showGlobalRating) {
        song.rating
    } else {
        0F
    }
    RatingText(modifier = modifier, rated = rated, rating = rating)
}

@Composable
fun RatingText(modifier: Modifier = Modifier, album: AlbumState, showGlobalRating: Boolean) {
    val rated = album.ratingUser > 0F
    val rating = if (rated) {
        album.ratingUser
    } else if (showGlobalRating) {
        album.rating
    } else {
        0F
    }
    RatingText(modifier = modifier, rated = rated, rating = rating)
}

@Composable
private fun RatingText(modifier: Modifier = Modifier, rated: Boolean, rating: Float) {
    val color = if (rated) {
        colorResource(id = R.color.rated)
    } else {
        colorResource(id = R.color.unrated)
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text = Formatter.formatRating(rating),
            style = AppTypography.bodySmall,
            lineHeight = 14.sp,
            color = color)
        Icon(painter = painterResource(id = R.drawable.ic_star_rate_white_18dp),
            contentDescription = null,
            tint = color)
    }
}

@Preview
@Composable
private fun RatingTextPreview() {
    PreviewTheme {
        Surface {
            Column {
                RatingText(rated = false, rating = 4.8F)
                RatingText(rated = true, rating = 4.8F)

                RatingText(song = songStateData[0], showGlobalRating = false)
                RatingText(album = albumStateData[1], showGlobalRating = false)
                RatingText(song = songStateData[0], showGlobalRating = true)
                RatingText(album = albumStateData[1], showGlobalRating = true)
            }
        }
    }
}

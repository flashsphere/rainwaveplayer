package com.flashsphere.rainwaveplayer.ui.rating.tv

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTv
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTvTheme
import com.flashsphere.rainwaveplayer.ui.theme.tv.TvAppTypography
import com.flashsphere.rainwaveplayer.util.Formatter
import com.flashsphere.rainwaveplayer.view.uistate.model.AlbumState
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState

@Composable
fun TvRatingText(modifier: Modifier = Modifier, song: SongState, showGlobalRating: Boolean) {
    val rated = song.ratingUser.floatValue > 0F
    val rating = if (rated) {
        song.ratingUser.floatValue
    } else if (showGlobalRating) {
        song.rating
    } else {
        0F
    }
    TvRatingText(modifier = modifier, rated = rated, rating = rating)
}

@Composable
fun TvRatingText(modifier: Modifier = Modifier, album: AlbumState, showGlobalRating: Boolean) {
    val rated = album.ratingUser > 0F
    val rating = if (rated) {
        album.ratingUser
    } else if (showGlobalRating) {
        album.rating
    } else {
        0F
    }
    TvRatingText(modifier = modifier, rated = rated, rating = rating)
}

@Composable
private fun TvRatingText(modifier: Modifier = Modifier, rated: Boolean, rating: Float) {
    val color = if (rated) {
        colorResource(id = R.color.rated)
    } else {
        colorResource(id = R.color.unrated)
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = Formatter.formatRating(rating),
            style = TvAppTypography.bodySmall,
            lineHeight = TvAppTypography.bodySmall.fontSize,
            color = color,
        )
        Icon(
            painter = painterResource(R.drawable.ic_star_rate_white_18dp),
            contentDescription = null,
            tint = color,
        )
    }
}

@PreviewTv
@Composable
private fun TvRatingTextPreview() {
    PreviewTvTheme {
        Surface {
            TvRatingText(
                rated = true,
                rating = 4.3F,
            )
        }
    }
}

package com.flashsphere.rainwaveplayer.ui

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.flashsphere.rainwaveplayer.R

interface Fave {
    operator fun component1(): Int = drawable
    operator fun component2(): Int = color
    operator fun component3(): Int = contentDescription

    val favorite: Boolean

    @get:DrawableRes
    val drawable: Int
    @get:ColorRes
    val color: Int
    @get:StringRes
    val contentDescription: Int
}

@Immutable
class FaveSong(
    override val favorite: Boolean
) : Fave {
    override val drawable: Int = if (favorite) {
        R.drawable.ic_favorite_white_20dp
    } else {
        R.drawable.ic_favorite_border_white_20dp
    }
    override val color: Int = if (favorite) {
        R.color.favorite
    } else {
        R.color.unfavorite
    }
    override val contentDescription: Int = if (favorite) {
        R.string.unfavorite_song
    } else {
        R.string.favorite_song
    }
}

@Immutable
class FaveAlbum(
    override val favorite: Boolean
) : Fave {
    override val drawable: Int = if (favorite) {
        R.drawable.ic_favorite_white_20dp
    } else {
        R.drawable.ic_favorite_border_white_20dp
    }
    override val color: Int = if (favorite) {
        R.color.favorite
    } else {
        R.color.unfavorite
    }
    override val contentDescription: Int = if (favorite) {
        R.string.unfavorite_album
    } else {
        R.string.favorite_album
    }
}

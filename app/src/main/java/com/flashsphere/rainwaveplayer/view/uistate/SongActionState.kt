package com.flashsphere.rainwaveplayer.view.uistate

import android.os.Parcelable
import com.flashsphere.rainwaveplayer.view.uistate.model.IdName
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState
import kotlinx.parcelize.Parcelize

@Parcelize
data class SongActionState(
    val song: IdName,
    val rating: Float,
    val ratingAllowed: Boolean,
    val albums: List<IdName>,
    val artists: List<IdName>,
) : Parcelable {
    constructor(song: SongState) : this(
        song = IdName(song.id, song.title),
        rating = song.ratingUser.floatValue,
        ratingAllowed = song.ratingAllowed,
        albums = song.albums.map { IdName(it.id, it.name) },
        artists = song.artists.map { IdName(it.id, it.name) },
    )

    val songId
        get() = song.id

    val songTitle
        get() = song.name

    fun toRatingState() = RatingState(
        song = song,
        rating = rating,
    )
}

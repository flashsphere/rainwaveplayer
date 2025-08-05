package com.flashsphere.rainwaveplayer.view.uistate

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.saveable.Saver
import com.flashsphere.rainwaveplayer.view.uistate.model.IdName
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState

data class SongActionState(
    val song: IdName,
    val rating: Float,
    val ratingAllowed: Boolean,
    val albums: List<IdName>,
    val artists: List<IdName>,
    val scrollState: ScrollState,
) {
    constructor(song: SongState) : this(
        song = IdName(song.id, song.title),
        rating = song.ratingUser.floatValue,
        ratingAllowed = song.ratingAllowed,
        albums = song.albums.map { IdName(it.id, it.name) },
        artists = song.artists.map { IdName(it.id, it.name) },
        scrollState = ScrollState(0),
    )

    val songId
        get() = song.id

    val songTitle
        get() = song.name

    fun toRatingState() = RatingState(
        song = song,
        rating = rating,
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        val Saver = Saver<SongActionState?, List<Any>>(
            save = {
                if (it == null)
                    emptyList()
                else
                    listOf(
                        it.song,
                        it.rating,
                        it.ratingAllowed,
                        it.albums,
                        it.artists,
                        it.scrollState.value,
                    )
            },
            restore = {
                if (it.isEmpty())
                    null
                else
                    SongActionState(
                        it[0] as IdName,
                        it[1] as Float,
                        it[2] as Boolean,
                        it[3] as List<IdName>,
                        it[4] as List<IdName>,
                        ScrollState(it[5] as Int),
                    )
            },
        )
    }
}

package com.flashsphere.rainwaveplayer.view.uistate.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import com.flashsphere.rainwaveplayer.model.song.Song

@Immutable
class SongState(
    val id: Int,
    val title: String,
    val albumName: String,
    val artistName: String,
    val rating: Float,
    val ratingUser: MutableFloatState,
    val favorite: MutableState<Boolean>,
    val cool: Boolean,
    val requestable: Boolean,
    val entryId: Int,
    val voted: MutableState<Boolean>,
    val votingAllowed: Boolean,
    val ratingAllowed: Boolean,
    val length: Long,
    override val key: String = "song-${id}",
): CategoryDetailItem, ArtistDetailItem {
    constructor(song: Song) : this(
        id = song.id,
        title = song.title,
        albumName = song.getAlbumName(),
        artistName = song.getArtistName(),
        rating = song.rating,
        ratingUser = mutableFloatStateOf(song.ratingUser),
        favorite = mutableStateOf(song.favorite),
        cool = song.cool,
        requestable = song.requestable,
        entryId = song.entryId,
        voted = mutableStateOf(song.voted),
        votingAllowed = song.votingAllowed,
        ratingAllowed = song.ratingAllowed,
        length = song.length,
    )
}

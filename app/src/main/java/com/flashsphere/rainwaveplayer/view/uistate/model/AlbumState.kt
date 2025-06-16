package com.flashsphere.rainwaveplayer.view.uistate.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.flashsphere.rainwaveplayer.model.album.Album
import java.util.Collections

@Immutable
class AlbumState(
    override val id: Int,
    override val name: String,
    val art: String,
    val rating: Float,
    val ratingUser: Float,
    val cool: Boolean,
    val ratingCount: Int,
    val favorite: MutableState<Boolean>,
    val ratingDistribution: Map<Int, Float>,
    val songsOnCooldown: Boolean,
    val songs: SnapshotStateList<SongState>,
) : CategoryDetailItem, LibraryItem, SearchItem {
    override val key: String = "album-${id}"
    override val searchable: String = name

    constructor(id: Int, name: String) : this(
        id = id,
        name = name,
        art = "",
        rating = 0F,
        ratingUser = 0F,
        cool = false,
        ratingCount = 0,
        favorite = mutableStateOf(false),
        ratingDistribution = emptyMap(),
        songsOnCooldown = false,
        songs = mutableStateListOf()
    )

    constructor(album: Album) : this(
        id = album.id,
        name = album.name,
        art = if (album.art.isNotEmpty()) album.getArtUrl() else "",
        rating = album.rating,
        ratingUser = album.ratingUser,
        cool = album.cool,
        ratingCount = album.ratingCount,
        favorite = mutableStateOf(album.favorite),
        ratingDistribution = Collections.unmodifiableMap(album.ratingDistribution),
        songsOnCooldown = album.songs.any { it.cool },
        songs = album.songs.asSequence().map { SongState(it) }.toCollection(mutableStateListOf())
    )
}

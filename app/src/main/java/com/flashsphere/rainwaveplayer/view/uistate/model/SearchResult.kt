package com.flashsphere.rainwaveplayer.view.uistate.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.flashsphere.rainwaveplayer.model.song.Song

@Immutable
class SearchResult(
    val message: String? = null,
    val items: SnapshotStateList<SearchItem>? = null,
)

interface SearchHeaderItem : SearchItem

object ArtistSearchHeaderItem : SearchHeaderItem {
    override val id: Int = -1001
    override val key: String = "artist-header"
}

object AlbumSearchHeaderItem : SearchHeaderItem {
    override val id: Int = -1002
    override val key: String = "album-header"
}

object SongSearchHeaderItem : SearchHeaderItem {
    override val id: Int = -1003
    override val key: String = "song-header"
}

@Immutable
class SearchSongItem(
    val song: SongState,
) : SearchItem {
    override val id = song.id
    override val key: String = "song-${song.id}"

    constructor(song: Song) : this(
        song = SongState(song),
    )
}

package com.flashsphere.rainwaveplayer.view.uistate.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.flashsphere.rainwaveplayer.model.album.Album
import com.flashsphere.rainwaveplayer.model.artist.Artist
import com.flashsphere.rainwaveplayer.model.song.Song
import com.flashsphere.rainwaveplayer.model.station.Station

@Immutable
class ArtistState(
    override val id: Int,
    override val name: String,
    val items: SnapshotStateList<ArtistDetailItem>,
) : LibraryItem, SearchItem {
    override val key: String = "artist-${id}"
    override val searchable: String = name

    constructor(id: Int, name: String) : this(
        id = id,
        name = name,
        items = mutableStateListOf()
    )

    constructor(artist: Artist, currentStation: Station, stations: Map<Int, Station>) : this(
        id = artist.id,
        name = artist.name,
        items = mutableStateListOf<ArtistDetailItem>().apply {
            val addToResult = { station: Station, albumsForStation: Map<Album, List<Song>> ->
                for ((key, value) in albumsForStation) {
                    add(ArtistAlbumState(station, key,
                        currentStation.id != station.id))
                    value.forEach { song ->
                        add(SongState(song))
                    }
                }
            }

            val groupedSongs = artist.groupedSongs
            // add album + songs for the current station 1st
            groupedSongs[currentStation.id]?.let {
                addToResult(currentStation, it)
            }
            for ((key, value) in groupedSongs.entries) {
                val station = stations[key]
                if (currentStation.id != key && station != null) {
                    addToResult(station, value)
                }
            }
        }
    )
}

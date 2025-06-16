package com.flashsphere.rainwaveplayer.view.uistate.model

import androidx.compose.runtime.Immutable
import com.flashsphere.rainwaveplayer.model.album.Album
import com.flashsphere.rainwaveplayer.model.station.Station

@Immutable
class ArtistAlbumState(
    val stationId: Int,
    val stationName: String,
    val albumId: Int,
    val albumName: String,
    val showStationName: Boolean,
    override val key: String = "station-${stationId}-album-${albumId}"
) : ArtistDetailItem {
    constructor(station: Station, album: Album, showStationName: Boolean) : this(
        stationId = station.id,
        stationName = station.name,
        albumId = album.id,
        albumName = album.name,
        showStationName = showStationName,
    )
}

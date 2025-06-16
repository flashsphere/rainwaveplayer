package com.flashsphere.rainwaveplayer.internal.datastore.model

import com.flashsphere.rainwaveplayer.model.station.Station
import kotlinx.serialization.Serializable

@Serializable
class SavedStations(
    val timestamp: Long,
    val stations: List<Station>
)

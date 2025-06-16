package com.flashsphere.rainwaveplayer.model.station

import kotlinx.serialization.Serializable

@Serializable
class StationsResponse(
    val stations: List<@Serializable(with = StationSerializer::class) Station> = emptyList()
)

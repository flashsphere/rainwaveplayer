package com.flashsphere.rainwaveplayer.model.artist

import kotlinx.serialization.Serializable

@Serializable
class ArtistResponse(
    @Serializable(with = ArtistSerializer::class)
    val artist: Artist
)

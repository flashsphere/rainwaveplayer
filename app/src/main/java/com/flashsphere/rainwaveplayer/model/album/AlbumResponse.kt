package com.flashsphere.rainwaveplayer.model.album

import kotlinx.serialization.Serializable

@Serializable
class AlbumResponse(
    @Serializable(with = AlbumSerializer::class)
    val album: Album
)

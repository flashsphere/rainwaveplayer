package com.flashsphere.rainwaveplayer.model.favorite

import com.flashsphere.rainwaveplayer.model.song.Song
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class AllFavesResponse(
    @SerialName("all_faves")
    val songs: List<Song>,
)

package com.flashsphere.rainwaveplayer.model.request

import com.flashsphere.rainwaveplayer.model.song.Song
import com.flashsphere.rainwaveplayer.model.song.SongSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class RequestHistoryResponse(
    @SerialName("user_requested_history")
    val songs: List<@Serializable(with = SongSerializer::class) Song>,
)

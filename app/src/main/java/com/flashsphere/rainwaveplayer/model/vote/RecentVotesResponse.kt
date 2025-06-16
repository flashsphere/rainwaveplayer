package com.flashsphere.rainwaveplayer.model.vote

import com.flashsphere.rainwaveplayer.model.song.Song
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class RecentVotesResponse(
    @SerialName("user_recent_votes")
    val songs: List<Song>,
)

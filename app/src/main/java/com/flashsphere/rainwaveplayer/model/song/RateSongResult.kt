package com.flashsphere.rainwaveplayer.model.song

import com.flashsphere.rainwaveplayer.model.ResponseResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class RateSongResult(
    @SerialName("rating_user")
    val rating: Float = 0F,

    @SerialName("song_id")
    val songId: Int = 0,
) : ResponseResult()

package com.flashsphere.rainwaveplayer.model.song

import com.flashsphere.rainwaveplayer.model.HasResponseResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class RateSongResponse(
    @SerialName("rate_result")
    override val result: RateSongResult
) : HasResponseResult<RateSongResult>

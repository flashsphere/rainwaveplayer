package com.flashsphere.rainwaveplayer.model.song

import com.flashsphere.rainwaveplayer.model.HasResponseResult
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
class RateSongResponse(
    @SerialName("rate_result")
    @OptIn(ExperimentalSerializationApi::class)
    @JsonNames("error")
    override val result: RateSongResult,
) : HasResponseResult<RateSongResult>

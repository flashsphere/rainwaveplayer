package com.flashsphere.rainwaveplayer.model.favorite

import com.flashsphere.rainwaveplayer.model.HasResponseResult
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
class FaveSongResponse(
    @SerialName("fave_song_result")
    @OptIn(ExperimentalSerializationApi::class)
    @JsonNames("error")
    override val result: FaveResult,
) : HasResponseResult<FaveResult>

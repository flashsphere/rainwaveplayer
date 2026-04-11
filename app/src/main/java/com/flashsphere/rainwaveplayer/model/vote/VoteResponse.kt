package com.flashsphere.rainwaveplayer.model.vote

import com.flashsphere.rainwaveplayer.model.HasResponseResult
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
class VoteResponse(
    @SerialName("vote_result")
    @OptIn(ExperimentalSerializationApi::class)
    @JsonNames("error")
    override val result: VoteResult,
) : HasResponseResult<VoteResult>

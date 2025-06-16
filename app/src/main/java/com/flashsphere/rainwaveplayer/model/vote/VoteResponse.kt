package com.flashsphere.rainwaveplayer.model.vote

import com.flashsphere.rainwaveplayer.model.HasResponseResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VoteResponse(
    @SerialName("vote_result")
    override val result: VoteResult,
) : HasResponseResult<VoteResult>

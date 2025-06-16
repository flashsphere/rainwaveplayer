package com.flashsphere.rainwaveplayer.model.vote

import com.flashsphere.rainwaveplayer.model.ResponseResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VoteResult(
    @SerialName("entry_id")
    val entryId: Int = 0,
    @SerialName("elec_id")
    val eventId: Int = 0,
) : ResponseResult()

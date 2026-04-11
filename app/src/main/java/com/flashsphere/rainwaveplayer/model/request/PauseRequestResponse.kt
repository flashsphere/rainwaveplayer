package com.flashsphere.rainwaveplayer.model.request

import com.flashsphere.rainwaveplayer.model.HasResponseResult
import com.flashsphere.rainwaveplayer.model.ResponseResult
import com.flashsphere.rainwaveplayer.model.user.User
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
class PauseRequestResponse(
    @SerialName("pause_request_queue_result")
    @OptIn(ExperimentalSerializationApi::class)
    @JsonNames("error")
    override val result: ResponseResult,

    @SerialName("user")
    val user: User? = null,
) : HasResponseResult<ResponseResult>

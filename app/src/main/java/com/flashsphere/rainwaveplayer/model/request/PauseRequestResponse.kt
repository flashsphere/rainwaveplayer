package com.flashsphere.rainwaveplayer.model.request

import com.flashsphere.rainwaveplayer.model.HasResponseResult
import com.flashsphere.rainwaveplayer.model.ResponseResult
import com.flashsphere.rainwaveplayer.model.user.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class PauseRequestResponse(
    @SerialName("pause_request_queue_result")
    override val result: ResponseResult,

    @SerialName("user")
    val user: User? = null,
) : HasResponseResult<ResponseResult>

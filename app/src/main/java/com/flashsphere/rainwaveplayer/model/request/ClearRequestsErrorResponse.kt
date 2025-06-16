package com.flashsphere.rainwaveplayer.model.request

import com.flashsphere.rainwaveplayer.model.HasResponseResult
import com.flashsphere.rainwaveplayer.model.ResponseResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ClearRequestsErrorResponse(
    @SerialName("clear_requests_result")
    override val result: ResponseResult,
) : HasResponseResult<ResponseResult>

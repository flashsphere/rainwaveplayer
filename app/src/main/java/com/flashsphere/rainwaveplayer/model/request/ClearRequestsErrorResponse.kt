package com.flashsphere.rainwaveplayer.model.request

import com.flashsphere.rainwaveplayer.model.HasResponseResult
import com.flashsphere.rainwaveplayer.model.ResponseResult
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
class ClearRequestsErrorResponse(
    @SerialName("clear_requests_result")
    @OptIn(ExperimentalSerializationApi::class)
    @JsonNames("error")
    override val result: ResponseResult,
) : HasResponseResult<ResponseResult>

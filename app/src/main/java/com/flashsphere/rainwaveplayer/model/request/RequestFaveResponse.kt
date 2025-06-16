package com.flashsphere.rainwaveplayer.model.request

import com.flashsphere.rainwaveplayer.model.HasResponseResult
import com.flashsphere.rainwaveplayer.model.ResponseResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class RequestFaveResponse(
    @SerialName("request_favorited_songs_result")
    override val result: ResponseResult,
    @SerialName("requests")
    val requests: List<Request> = emptyList(),
) : HasResponseResult<ResponseResult>

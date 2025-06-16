package com.flashsphere.rainwaveplayer.model.station

import com.flashsphere.rainwaveplayer.model.HasResponseResult
import com.flashsphere.rainwaveplayer.model.ResponseResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class StationsErrorResponse(
    @SerialName("stations")
    override val result: ResponseResult
) : HasResponseResult<ResponseResult>

package com.flashsphere.rainwaveplayer.model.stationInfo

import com.flashsphere.rainwaveplayer.model.HasResponseResult
import com.flashsphere.rainwaveplayer.model.ResponseResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class InfoErrorResponse(
    @SerialName("info_result")
    override val result: ResponseResult
) : HasResponseResult<ResponseResult>

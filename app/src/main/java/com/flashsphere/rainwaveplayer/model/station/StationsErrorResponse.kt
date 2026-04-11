package com.flashsphere.rainwaveplayer.model.station

import com.flashsphere.rainwaveplayer.model.HasResponseResult
import com.flashsphere.rainwaveplayer.model.ResponseResult
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
class StationsErrorResponse(
    @SerialName("stations")
    @OptIn(ExperimentalSerializationApi::class)
    @JsonNames("error")
    override val result: ResponseResult,
) : HasResponseResult<ResponseResult>

package com.flashsphere.rainwaveplayer.model.favorite

import com.flashsphere.rainwaveplayer.model.ResponseResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class FaveResult(
    @SerialName("fave")
    val favorite: Boolean = false,

    @SerialName("id")
    val id: Int = 0,

    @SerialName("sid")
    val stationId: Int = 0,
) : ResponseResult()

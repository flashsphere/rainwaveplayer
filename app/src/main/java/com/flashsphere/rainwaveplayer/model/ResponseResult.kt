package com.flashsphere.rainwaveplayer.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
open class ResponseResult(
    var success: Boolean = false,
    var text: String = "",
    @SerialName("tl_key")
    var tlKey: String = "",
    var code: Int = 0,
)

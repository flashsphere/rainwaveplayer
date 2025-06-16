package com.flashsphere.rainwaveplayer.model

import kotlinx.serialization.Serializable

@Serializable
open class ResponseResult(
    var success: Boolean = false,
    var text: String = "",
    var code: Int = 0,
)

package com.flashsphere.rainwaveplayer.model.user

import com.flashsphere.rainwaveplayer.model.HasResponseResult
import com.flashsphere.rainwaveplayer.model.ResponseResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class UserInfoErrorResponse(
    @SerialName("user_info_result")
    override val result: ResponseResult
) : HasResponseResult<ResponseResult>

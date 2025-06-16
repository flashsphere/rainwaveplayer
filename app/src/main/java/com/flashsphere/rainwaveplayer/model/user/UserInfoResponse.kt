package com.flashsphere.rainwaveplayer.model.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class UserInfoResponse(
    @SerialName("user_info")
    val user: User
)

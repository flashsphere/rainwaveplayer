package com.flashsphere.rainwaveplayer.model.user

import com.flashsphere.rainwaveplayer.repository.RainwaveService
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class User(
    @SerialName("id")
    val id: Int = 0,

    @SerialName("name")
    val name: String = "",

    @SerialName("avatar")
    val avatar: String = "",

    @SerialName("requests_paused")
    val requestsPaused: Boolean = false,

    @SerialName("request_position")
    val requestPosition: Int = 0,
) {
    fun getAvatarUrl(): String? {
        if (avatar.isNotEmpty()) {
            if (avatar.startsWith("/")) {
                return RainwaveService.BASE_URL + avatar
            }
            return avatar
        }
        return null
    }
}

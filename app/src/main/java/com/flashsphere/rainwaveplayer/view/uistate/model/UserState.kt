package com.flashsphere.rainwaveplayer.view.uistate.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.flashsphere.rainwaveplayer.model.user.User
import kotlinx.parcelize.Parcelize

@Immutable
@Parcelize
class UserState(
    val id: Int,
    val name: String,
    val avatar: String?,
    val requestsPaused: Boolean,
    val requestPosition: Int,
): Parcelable {
    constructor(user: User) : this(
        id = user.id,
        name = user.name,
        avatar = user.getAvatarUrl(),
        requestsPaused = user.requestsPaused,
        requestPosition = user.requestPosition,
    )

    fun isAnon() = ANON_USER_ID == id

    companion object {
        private const val ANON_USER_ID = 1 // Anonymous user has id = 1
    }
}

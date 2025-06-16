package com.flashsphere.rainwaveplayer.view.helper

import android.net.Uri
import androidx.core.net.toUri

object Urls {
    val LOGIN_URL: Uri by lazy { "https://rainwave.cc/oauth/login?destination=app".toUri() }
    val PATREON_URL: Uri by lazy { "https://www.patreon.com/rainwave".toUri() }
    val DISCORD_URL: Uri by lazy { "https://discord.gg/rNCBhSz".toUri() }
    val PRIVACY_POLICY_URL: Uri by lazy { "https://player-for-rainwave-d3cf6.firebaseapp.com/".toUri() }
}

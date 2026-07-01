package com.flashsphere.rainwaveplayer.util

import android.app.PendingIntent

object PendingIntentUtils {
    fun getPendingIntentFlags(flags: Int = PendingIntent.FLAG_UPDATE_CURRENT): Int {
        return PendingIntent.FLAG_IMMUTABLE or flags
    }
}

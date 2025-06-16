package com.flashsphere.rainwaveplayer.util

import android.app.PendingIntent
import android.os.Build

object PendingIntentUtils {
    fun getPendingIntentFlags(flags: Int = PendingIntent.FLAG_UPDATE_CURRENT): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or flags
        } else flags
    }
}

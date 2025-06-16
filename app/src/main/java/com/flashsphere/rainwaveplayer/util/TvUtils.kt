package com.flashsphere.rainwaveplayer.util

import android.content.Context
import android.content.pm.PackageManager

fun Context.isTv(): Boolean {
    return applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
}

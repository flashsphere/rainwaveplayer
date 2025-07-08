package com.flashsphere.rainwaveplayer.util

import android.content.Intent
import androidx.core.content.IntentCompat

object IntentUtils {
    fun <T> getParcelableExtra(intent: Intent, name: String, clazz: Class<T>): T? {
        intent.setExtrasClassLoader(clazz.classLoader)
        return IntentCompat.getParcelableExtra(intent, name, clazz)
    }
}

package com.flashsphere.rainwaveplayer.util

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.collection.SieveCache
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap

object DrawableUtils {
    private val bitmapCache: SieveCache<Int, Bitmap>

    init {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        // Use 1/8th of the available memory for bitmap cache.
        val cacheSize = maxMemory / 8
        bitmapCache = SieveCache(
            maxSize = cacheSize,
            // The cache size will be measured in kilobytes rather than number of items.
            sizeOf = { _, bitmap -> bitmap.byteCount / 1024 },
        )
    }

    fun getDrawableBitmap(context: Context, @DrawableRes resId: Int): Bitmap {
        val bitmap = bitmapCache[resId].let {
            if (it == null) {
                val drawableBitmap = ContextCompat.getDrawable(context, resId)!!.toBitmap()
                bitmapCache.put(resId, drawableBitmap)
                drawableBitmap
            } else {
                it
            }
        }
        return bitmap
    }
}

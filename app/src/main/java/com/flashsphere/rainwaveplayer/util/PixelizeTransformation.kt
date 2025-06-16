package com.flashsphere.rainwaveplayer.util

import android.graphics.Bitmap
import androidx.core.graphics.scale
import coil3.size.Size
import coil3.transform.Transformation
import kotlin.math.max

class PixelizeTransformation(
    private val pixelizationFactor: Float
) : Transformation() {
    override val cacheKey = "${javaClass.name}-$pixelizationFactor"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val width = input.width
        val height = input.height

        val downScaleFactorWidth = max((pixelizationFactor * width).toInt(), 1)
        val downScaleFactorHeight = max((pixelizationFactor * height).toInt(), 1)
        val downScaledWidth = width / downScaleFactorWidth
        val downScaledHeight = height / downScaleFactorHeight
        val pixelatedBitmap = input.scale(downScaledWidth, downScaledHeight, true)
        return pixelatedBitmap.scale(width, height, true)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PixelizeTransformation

        return pixelizationFactor == other.pixelizationFactor
    }

    override fun hashCode(): Int {
        return pixelizationFactor.hashCode()
    }
}

package com.flashsphere.rainwaveplayer.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.ParsePosition

object BufferMinSettingHelper {
    private val displayFormat = DecimalFormat("#.#").apply {
        isParseBigDecimal = true
    }

    fun formatToString(value: Float): String {
        return this.displayFormat.format(value)
    }

    fun parseToValue(value: String, parsePosition: ParsePosition = ParsePosition(0)): Float {
        val number = displayFormat.parse(value, parsePosition) as BigDecimal
        return number.setScale(1, RoundingMode.HALF_DOWN).toFloat()
    }

    fun validateBufferSetting(value: String): Boolean {
        if (value.isEmpty()) return true
        if (value.length > 3) return false
        return runCatching {
            val parsePosition = ParsePosition(0)
            val number = parseToValue(value, parsePosition)
            if (parsePosition.index != value.length || parsePosition.errorIndex != -1) {
                false
            } else {
                number >= 1
            }
        }.getOrElse { false }
    }
}

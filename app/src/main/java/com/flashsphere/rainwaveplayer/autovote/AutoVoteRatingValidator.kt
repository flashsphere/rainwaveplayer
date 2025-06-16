package com.flashsphere.rainwaveplayer.autovote

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.ParsePosition

object AutoVoteRatingValidator {
    private val displayFormat = DecimalFormat("#.#").apply {
        isParseBigDecimal = true
    }

    fun formatToString(value: Float): String {
        return this.displayFormat.format(value)
    }

    fun parseToValue(value: String, parsePosition: ParsePosition = ParsePosition(0)): Float {
        val number = displayFormat.parse(value, parsePosition) as BigDecimal? ?: BigDecimal.ZERO
        return number.toFloat()
    }

    fun validate(value: String): Boolean {
        if (value.isEmpty()) return true
        if (value.length > 3) return false
        return runCatching {
            val parsePosition = ParsePosition(0)
            val number = parseToValue(value, parsePosition)
            if (parsePosition.index != value.length || parsePosition.errorIndex != -1) {
                false
            } else {
                number in 0.0..5.0
            }
        }.getOrElse { false }
    }
}

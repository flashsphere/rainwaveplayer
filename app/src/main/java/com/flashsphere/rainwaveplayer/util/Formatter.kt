package com.flashsphere.rainwaveplayer.util

import android.content.Context
import androidx.core.i18n.MessageFormat
import com.flashsphere.rainwaveplayer.R
import java.text.DecimalFormat

object Formatter {
    private val ratingFormatter = DecimalFormat("#,###.0")

    fun formatRating(rating: Float): String {
        return if (rating > 0) {
            ratingFormatter.format(rating.toDouble())
        } else {
            "-"
        }
    }

    fun formatNumberOrdinal(context: Context, number: Int): String {
        return MessageFormat.format(context, R.string.position_ordinal, mapOf("position" to number))
    }
}

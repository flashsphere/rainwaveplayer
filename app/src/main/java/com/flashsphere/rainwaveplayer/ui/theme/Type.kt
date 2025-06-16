package com.flashsphere.rainwaveplayer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

val AppTypography = Typography()

val SansSerifCondensed: FontFamily = runCatching {
    FontFamily(Font(DeviceFontFamilyName("sans-serif-condensed"), FontWeight.Normal))
}.getOrElse { FontFamily.SansSerif }

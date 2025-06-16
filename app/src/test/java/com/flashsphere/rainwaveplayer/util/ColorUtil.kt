package com.flashsphere.rainwaveplayer.util

fun main(args: Array<String>) {
    val background = Color("#101418")

    println("voted: " + votedColor(background))
    println("fave: " + faveColor(background))
    println("unfave: " + unfaveColor(background))
    println("rated: " + ratedColor(background))
    println("unrated: " + unratedColor(background))
}

fun flattenColor(foreground: Color, background: Color, opacity: Float): Color {
    return Color(
        r = (opacity * foreground.r + (1F - opacity) * background.r).toInt(),
        g = (opacity * foreground.g + (1F - opacity) * background.g).toInt(),
        b = (opacity * foreground.b + (1F - opacity) * background.b).toInt(),
    )
}

fun votedColor(background: Color): String {
    return flattenColor(Color("#005140"), background, 0.8F).toHex()
}

fun faveColor(background: Color): String {
    return flattenColor(Color("#A3F2D8"), background, 0.8F).toHex()
}

fun unfaveColor(background: Color): String {
    return flattenColor(Color("#E0E2E8"), background, 0.7F).toHex()
}

fun ratedColor(background: Color): String {
    return flattenColor(Color("#A3F2D8"), background, 0.9F).toHex()
}

fun unratedColor(background: Color): String {
    return flattenColor(Color("#E0E2E8"), background, 0.9F).toHex()
}

data class Color(
    val r: Int,
    val g: Int,
    val b: Int,
) {
    constructor(hexColor: String) : this(
        r = Integer.valueOf(hexColor.substring(1, 3), 16),
        g = Integer.valueOf(hexColor.substring(3, 5), 16),
        b = Integer.valueOf(hexColor.substring(5, 7), 16),
    )

    fun toHex(): String {
        return String.format("#%02x%02x%02x", r, g, b).uppercase()
    }
}

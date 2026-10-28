package tv.iptv.app.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import tv.iptv.app.R

import androidx.compose.ui.graphics.Color

val avroraFont: FontFamily = FontFamily(
    Font(R.font.montserrat_light, FontWeight.Light),
    Font(R.font.montserrat_semibold, FontWeight.SemiBold),
)

object Palette {
    val Background = Color(0xFF170E09)
    val Card = Color(0x28FFD9A8)
    val CardFocused = Color(0x45FFC46B)
    val Border = Color(0x33FFC46B)
    val LogoBox = Color(0x73000000)

    /** sun gold, the "Avrora" accent */
    val Accent = Color(0xFFFFC46B)
    val Mint = Color(0xFFFFD79A)
    val TextDim = Color(0xFFE0CDBA)
}

fun formatCount(value: Int): String =
    value.toString().reversed().chunked(3).joinToString(" ").reversed()

fun greeting(hour: Int): String = when (hour) {
    in 5..11 -> "Доброе утро"
    in 12..17 -> "Добрый день"
    else -> "Добрый вечер"
}

/** Russian first, then English, everything else after. */
fun languagePriority(code: String, country: String): Int = when {
    code == "rus" || country == "ru" -> 0
    code == "eng" || country == "us" || country == "gb" -> 1
    else -> 2
}

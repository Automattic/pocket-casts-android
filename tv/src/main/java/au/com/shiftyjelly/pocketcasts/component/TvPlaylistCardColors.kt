package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.ui.graphics.Color

object TvPlaylistCardColors {

    fun cardColor(podcastTint: Int?, seed: String): Color {
        return podcastTint?.let(::clampedTint) ?: fallbackColor(seed)
    }

    private fun clampedTint(tint: Int): Color? {
        val (hue, saturation, value) = tint.toHsv()
        if (saturation <= 0.15f || value <= 0.1f) return null
        return Color.hsv(
            hue = hue,
            saturation = saturation.coerceIn(0.5f, 0.85f),
            value = value.coerceIn(0.3f, 0.45f),
        )
    }

    private fun fallbackColor(seed: String): Color {
        val index = seed.sumOf { character -> character.code } % fallbackPalette.size
        return fallbackPalette[index]
    }

    private fun Int.toHsv(): Triple<Float, Float, Float> {
        val red = (this shr 16 and 0xFF) / 255f
        val green = (this shr 8 and 0xFF) / 255f
        val blue = (this and 0xFF) / 255f
        val max = maxOf(red, green, blue)
        val min = minOf(red, green, blue)
        val delta = max - min
        val hue = when {
            delta == 0f -> 0f
            max == red -> 60f * (((green - blue) / delta).mod(6f))
            max == green -> 60f * ((blue - red) / delta + 2f)
            else -> 60f * ((red - green) / delta + 4f)
        }
        val saturation = if (max == 0f) 0f else delta / max
        return Triple(hue, saturation, max)
    }

    private val fallbackPalette = listOf(
        Color(red = 0.15f, green = 0.25f, blue = 0.5f),
        Color(red = 0.5f, green = 0.17f, blue = 0.15f),
        Color(red = 0.21f, green = 0.22f, blue = 0.14f),
        Color(red = 0.5f, green = 0.35f, blue = 0.12f),
        Color(red = 0.15f, green = 0.4f, blue = 0.3f),
        Color(red = 0.3f, green = 0.2f, blue = 0.45f),
    )
}

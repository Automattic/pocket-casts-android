package au.com.shiftyjelly.pocketcasts.views.compose

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import au.com.shiftyjelly.pocketcasts.compose.ThemeColors
import au.com.shiftyjelly.pocketcasts.compose.theme

@Immutable
data class EpisodeRowColors(
    val tint: Color,
    val theme: ThemeColors,
) {
    internal fun statusIconTint(tint: EpisodeStatusIconTint) = when (tint) {
        EpisodeStatusIconTint.Downloaded -> theme.support02
        EpisodeStatusIconTint.Secondary -> theme.primaryIcon02
    }

    internal fun background(isSelected: Boolean) = if (isSelected) theme.primaryUi02Selected else theme.primaryUi02

    internal fun title(isGreyedOut: Boolean) = theme.primaryText01.greyedOut(isGreyedOut)

    internal fun secondaryText(isGreyedOut: Boolean) = theme.primaryText02.greyedOut(isGreyedOut)

    private fun Color.greyedOut(isGreyedOut: Boolean) = if (isGreyedOut) copy(alpha = 128 / 255f) else this
}

object EpisodeRowDefaults {
    @Composable
    fun colors(
        tint: Color = MaterialTheme.theme.colors.primaryIcon01,
        theme: ThemeColors = MaterialTheme.theme.colors,
    ) = EpisodeRowColors(tint = tint, theme = theme)
}

package au.com.shiftyjelly.pocketcasts.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.Border
import androidx.tv.material3.ButtonBorder
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.IconButtonDefaults

object TvButtonDefaults {

    @Composable
    fun filledButtonColors(): ButtonColors = ButtonDefaults.colors(
        containerColor = TvColors.BackgroundActive50,
        contentColor = TvColors.TextPrimary,
        focusedContainerColor = TvColors.BackgroundActive,
        focusedContentColor = TvColors.TextPrimaryActive,
    )

    @Composable
    fun prominentButtonColors(): ButtonColors = ButtonDefaults.colors(
        containerColor = TvColors.BackgroundActive,
        contentColor = TvColors.TextPrimaryActive,
        focusedContainerColor = TvColors.BackgroundActive,
        focusedContentColor = TvColors.TextPrimaryActive,
    )

    @Composable
    fun borderlessButtonColors(): ButtonColors = ButtonDefaults.colors(
        containerColor = Color.Transparent,
        contentColor = TvColors.TextSecondary,
        focusedContainerColor = TvColors.BackgroundActive.copy(alpha = 0.1f),
        focusedContentColor = TvColors.TextPrimary,
    )

    @Composable
    fun borderlessButtonBorder(): ButtonBorder = ButtonDefaults.border(
        border = Border.None,
        focusedBorder = Border.None,
    )

    @Composable
    fun iconButtonColors(containerColor: Color = TvColors.BackgroundActive50) = IconButtonDefaults.colors(
        containerColor = containerColor,
        contentColor = TvColors.TextPrimary,
        focusedContainerColor = TvColors.BackgroundActive,
        focusedContentColor = TvColors.TextPrimaryActive,
    )
}

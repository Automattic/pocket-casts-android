package au.com.shiftyjelly.pocketcasts.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.Border
import androidx.tv.material3.ButtonBorder
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults

object TvButtonDefaults {

    @Composable
    fun filledButtonColors(): ButtonColors = ButtonDefaults.colors(
        containerColor = TvColors.BgActive20,
        contentColor = TvColors.TextSecondary,
        focusedContainerColor = Color.White,
        focusedContentColor = TvColors.Dark,
    )

    @Composable
    fun borderlessButtonColors(): ButtonColors = ButtonDefaults.colors(
        containerColor = Color.Transparent,
        contentColor = TvColors.TextSecondary,
        focusedContainerColor = Color.White.copy(alpha = 0.1f),
        focusedContentColor = Color.White,
    )

    @Composable
    fun borderlessButtonBorder(): ButtonBorder = ButtonDefaults.border(
        border = Border.None,
        focusedBorder = Border.None,
    )
}

package au.com.shiftyjelly.pocketcasts.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.Border
import androidx.tv.material3.ButtonBorder
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme

object TvButtonDefaults {

    @Composable
    fun filledButtonColors(): ButtonColors = ButtonDefaults.colors(
        containerColor = MaterialTheme.tvColors.backgroundActive20,
        contentColor = MaterialTheme.tvColors.textPrimary,
        focusedContainerColor = MaterialTheme.tvColors.backgroundActive,
        focusedContentColor = MaterialTheme.tvColors.textPrimaryActive,
    )

    @Composable
    fun prominentButtonColors(): ButtonColors = ButtonDefaults.colors(
        containerColor = MaterialTheme.tvColors.backgroundActive,
        contentColor = MaterialTheme.tvColors.textPrimaryActive,
        focusedContainerColor = MaterialTheme.tvColors.backgroundActive,
        focusedContentColor = MaterialTheme.tvColors.textPrimaryActive,
    )

    @Composable
    fun borderlessButtonColors(): ButtonColors = ButtonDefaults.colors(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.tvColors.textSecondary,
        focusedContainerColor = MaterialTheme.tvColors.backgroundActive.copy(alpha = 0.1f),
        focusedContentColor = MaterialTheme.tvColors.textPrimary,
    )

    @Composable
    fun borderlessButtonBorder(): ButtonBorder = ButtonDefaults.border(
        border = Border.None,
        focusedBorder = Border.None,
    )

    @Composable
    fun iconButtonColors(
        containerColor: Color = MaterialTheme.tvColors.backgroundActive20,
        contentColor: Color = MaterialTheme.tvColors.textPrimary,
    ) = IconButtonDefaults.colors(
        containerColor = containerColor,
        contentColor = contentColor,
        focusedContainerColor = MaterialTheme.tvColors.backgroundActive,
        focusedContentColor = MaterialTheme.tvColors.textPrimaryActive,
    )

    @Composable
    fun controlBarIconButtonColors() = iconButtonColors(
        contentColor = MaterialTheme.tvColors.textSecondary,
    )
}

package au.com.shiftyjelly.pocketcasts.compose.swipe

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import au.com.shiftyjelly.pocketcasts.compose.theme
import androidx.compose.material.MaterialTheme as Material
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Immutable
data class SwipeRowAction(
    @DrawableRes val iconId: Int,
    val contentDescription: String,
    val backgroundColor: Color,
    val iconTint: Color = Color.White,
    val isFullSwipeEnabled: Boolean = false,
    val onClick: (SwipeRowState) -> Unit,
)

object SwipeRowActionDefaults {
    @Composable
    fun share(
        contentDescription: String = stringResource(LR.string.share),
        isFullSwipeEnabled: Boolean = false,
        onClick: (SwipeRowState) -> Unit,
    ) = SwipeRowAction(
        iconId = IR.drawable.ic_share,
        contentDescription = contentDescription,
        backgroundColor = Material.theme.colors.support01,
        isFullSwipeEnabled = isFullSwipeEnabled,
        onClick = onClick,
    )

    @Composable
    fun delete(
        contentDescription: String = stringResource(LR.string.delete),
        isFullSwipeEnabled: Boolean = false,
        onClick: (SwipeRowState) -> Unit,
    ) = SwipeRowAction(
        iconId = IR.drawable.ic_delete,
        contentDescription = contentDescription,
        backgroundColor = Material.theme.colors.support05,
        isFullSwipeEnabled = isFullSwipeEnabled,
        onClick = onClick,
    )
}

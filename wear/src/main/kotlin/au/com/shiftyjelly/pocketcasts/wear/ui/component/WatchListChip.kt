package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipColors
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.wear.theme.theme

@Composable
fun WatchListChip(
    title: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ChipColors = ChipDefaults.secondaryChipColors(
        secondaryContentColor = MaterialTheme.theme.colors.primaryText02
    ),
    overflow: TextOverflow? = null,
    secondaryLabel: String? = null,
) {
    WatchListChip(
        title = title,
        onClick = onClick,
        colors = colors,
        overflow = overflow,
        secondaryLabel = secondaryLabel,
        icon = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = title,
            )
        },
        modifier = modifier,
    )
}

@Composable
fun WatchListChip(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable BoxScope.() -> Unit)? = null,
    colors: ChipColors = ChipDefaults.secondaryChipColors(
        secondaryContentColor = MaterialTheme.theme.colors.primaryText02
    ),
    overflow: TextOverflow? = null,
    secondaryLabel: String? = null,
) {
    Chip(
        onClick = onClick,
        colors = colors,
        label = {
            Text(
                text = title,
                maxLines = if (secondaryLabel == null) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        secondaryLabel = {
            if (secondaryLabel != null) {
                Text(
                    text = secondaryLabel,
                    overflow = overflow ?: TextOverflow.Ellipsis,
                )
            }
        },
        icon = icon,
        modifier = modifier.fillMaxWidth()
    )
}

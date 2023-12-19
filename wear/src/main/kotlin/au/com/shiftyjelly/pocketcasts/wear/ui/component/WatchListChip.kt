package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipColors
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.horologist.compose.material.util.adjustChipHeightToFontScale
import com.google.android.horologist.compose.material.Chip as StandardChip

@Composable
fun WatchListChip(
    title: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
) {
    WatchListChip(
        title = title,
        onClick = onClick,
        secondaryLabel = secondaryLabel,
        icon = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = title,
                modifier = Modifier.size(24.dp)
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
    secondaryLabel: String? = null,
) {
    StandardChip(
        label = title,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        secondaryLabel = secondaryLabel,
        icon = icon,
        colors = ChipDefaults.secondaryChipColors(),
    )
}

@Composable
fun WatchListChip(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable BoxScope.() -> Unit)? = null,
    secondaryLabel: String? = null,
    colors: ChipColors = ChipDefaults.secondaryChipColors(
        secondaryContentColor = MaterialTheme.colors.onPrimary
    ),
) {
    Chip(
        label = {
            Text(
                text = title,
                style = MaterialTheme.typography.button,
                color = MaterialTheme.colors.onPrimary,
                overflow = TextOverflow.Ellipsis,
                maxLines = if (secondaryLabel != null) 1 else 2,
            )
        },
        onClick = onClick,
        modifier = modifier
            .adjustChipHeightToFontScale(LocalConfiguration.current.fontScale),
        icon = icon,
        secondaryLabel = {
            if (secondaryLabel != null) {
                Text(
                    text = secondaryLabel,
                    color = MaterialTheme.colors.onSecondary,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        colors = colors,
    )
}

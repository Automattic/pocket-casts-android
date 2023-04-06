package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.wear.theme.theme

@Composable
fun WatchListChip(
    @StringRes titleRes: Int,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
) {
    val title = stringResource(titleRes)
    Chip(
        onClick = onClick,
        colors = ChipDefaults.secondaryChipColors(
            secondaryContentColor = MaterialTheme.theme.colors.primaryText02
        ),
        label = {
            Text(title)
        },
        secondaryLabel = {
            if (secondaryLabel != null) {
                Text(
                    text = secondaryLabel,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        icon = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = title
            )
        },
        modifier = modifier.fillMaxWidth()
    )
}

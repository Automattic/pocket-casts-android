package au.com.shiftyjelly.pocketcasts.compose.bookmark

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.PlayerColors
import au.com.shiftyjelly.pocketcasts.compose.ThemeColors
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun HeaderRow(
    title: String,
    onOptionsMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: HeaderRowColors = HeaderRowColors.default(MaterialTheme.theme.colors),
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
    ) {
        TextH40(
            text = title,
            color = colors.text,
        )
        IconButton(
            onClick = { onOptionsMenuClick() },
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_more_vert_black_24dp),
                contentDescription = stringResource(LR.string.more_options),
                tint = colors.icon,
            )
        }
    }
}

data class HeaderRowColors(
    val text: Color,
    val icon: Color,
) {
    companion object {
        fun default(colors: ThemeColors) = HeaderRowColors(
            text = colors.primaryText02,
            icon = colors.primaryIcon02,
        )

        fun player(colors: PlayerColors) = HeaderRowColors(
            text = colors.contrast02,
            icon = colors.contrast01,
        )
    }
}

@Preview()
@Composable
private fun HeaderRowPlayerPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        HeaderRow(
            title = "Header Row",
            onOptionsMenuClick = {},
        )
    }
}

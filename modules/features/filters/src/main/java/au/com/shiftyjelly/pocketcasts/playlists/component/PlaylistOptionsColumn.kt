package au.com.shiftyjelly.pocketcasts.playlists.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.images.R as IR

data class PlaylistOption(
    val title: String,
    @DrawableRes val iconId: Int,
    val onClick: () -> Unit,
    val description: String? = null,
)

@Composable
internal fun PlaylistOptionsColumn(
    options: List<PlaylistOption>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        options.forEachIndexed { index, option ->
            OptionRow(
                title = option.title,
                description = option.description,
                iconId = option.iconId,
                onClick = option.onClick,
                showDivider = index != options.lastIndex,
            )
        }
    }
}

@Composable
private fun OptionRow(
    @DrawableRes iconId: Int,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    description: String? = null,
) {
    Column(
        modifier = modifier
            .clickable(
                role = Role.Button,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {},
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 26.dp, horizontal = 20.dp),
        ) {
            Icon(
                painter = painterResource(iconId),
                tint = MaterialTheme.theme.colors.primaryIcon01,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Spacer(
                modifier = Modifier.size(24.dp),
            )
            TextH30(
                text = title,
                modifier = Modifier.weight(1f),
            )
            if (description != null) {
                TextH50(
                    text = description,
                    color = MaterialTheme.theme.colors.primaryText02,
                )
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .padding(start = 20.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.theme.colors.primaryUi05),
            )
        } else {
            Spacer(
                modifier = Modifier.height(1.dp),
            )
        }
    }
}

@Preview
@Composable
private fun OptionsColumnPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppThemeWithBackground(themeType) {
        PlaylistOptionsColumn(
            options = listOf(
                PlaylistOption(
                    title = "Select Episodes",
                    iconId = IR.drawable.ic_playlists_sort,
                    onClick = {},
                ),
                PlaylistOption(
                    title = "Sort By",
                    description = "Newest to oldest",
                    iconId = IR.drawable.ic_playlists_sort,
                    onClick = {},
                ),
                PlaylistOption(
                    title = "Download all",
                    iconId = IR.drawable.ic_playlists_download_all,
                    onClick = {},
                ),
            ),
        )
    }
}

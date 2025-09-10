package au.com.shiftyjelly.pocketcasts.playlists

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextC50
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun DownloadLimitPage(
    episodeLimit: Int,
    onSelectEpisodeLimit: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp)
                .background(MaterialTheme.theme.colors.primaryUi05, CircleShape)
                .size(56.dp, 4.dp),
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        TextC50(
            text = stringResource(LR.string.filters_download_title).uppercase(),
            color = MaterialTheme.theme.colors.support01,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        AvailableLimits.forEachIndexed { index, limit ->
            EpisodeLimitRow(
                limit = limit,
                isSelected = limit == episodeLimit,
                showDivider = index != AvailableLimits.lastIndex,
                onClick = { onSelectEpisodeLimit(limit) },
            )
        }
    }
}

@Composable
private fun EpisodeLimitRow(
    limit: Int,
    isSelected: Boolean,
    showDivider: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
            TextH30(
                text = pluralStringResource(LR.plurals.episode_count, limit, limit),
                modifier = Modifier.weight(1f),
            )
            if (isSelected) {
                Spacer(
                    modifier = Modifier.size(24.dp),
                )
                Icon(
                    painter = painterResource(IR.drawable.ic_check),
                    tint = MaterialTheme.theme.colors.primaryIcon01,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
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

private val AvailableLimits = listOf(3, 5, 10, 20, 40, 100)

@Preview
@Composable
private fun DownloadLimitPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        DownloadLimitPage(
            episodeLimit = 20,
            onSelectEpisodeLimit = {},
        )
    }
}

package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImageDeprecated
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun SuggestedFolderPodcastsPage(
    folder: SuggestedFolder?,
    onGoBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val folderColor = folder?.colorIndex
        ?.let { MaterialTheme.theme.colors.getFolderColor(it) }
        ?: MaterialTheme.theme.colors.primaryInteractive01

    Column(
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 64.dp),
        ) {
            IconButton(
                onClick = onGoBackClick,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(LR.string.back),
                    tint = folderColor,
                )
            }

            TextH30(
                text = folder?.name.orEmpty(),
                color = folderColor,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )

            Spacer(
                modifier = Modifier.size(48.dp),
            )
        }

        Spacer(
            modifier = Modifier.height(4.dp),
        )

        if (folder != null) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(110.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 8.dp)
                    .padding(horizontal = 16.dp),
            ) {
                items(folder.podcastIds) { podcastId ->
                    @Suppress("DEPRECATION")
                    PodcastImageDeprecated(podcastId)
                }
            }
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                TextP40(
                    text = stringResource(LR.string.error_generic_message),
                )
            }
        }
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun SuggestedFolderPodcastsPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        SuggestedFolderPodcastsPage(
            folder = SuggestedFolder(
                name = "Folder name",
                colorIndex = 0,
                podcastIds = listOf("1", "2", "3", "4", "5"),
            ),
            onGoBackClick = {},
        )
    }
}

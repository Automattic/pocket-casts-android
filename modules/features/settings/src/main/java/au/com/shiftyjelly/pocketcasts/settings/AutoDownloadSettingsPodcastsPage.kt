package au.com.shiftyjelly.pocketcasts.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.PreviewRegularDevice
import au.com.shiftyjelly.pocketcasts.compose.components.FadedLazyColumn
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AutoDownloadSettingsPodcastsPage(
    podcasts: List<Podcast>,
    onChangePodcast: (String, Boolean) -> Unit,
    onChangeAllPodcasts: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabledPodcastsCount = remember(podcasts) {
        podcasts.count { it.isAutoDownloadNewEpisodes }
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        ) {
            TextP50(
                text = pluralStringResource(LR.plurals.podcasts_selected_count, enabledPodcastsCount, enabledPodcastsCount),
                color = MaterialTheme.theme.colors.primaryText02,
                modifier = Modifier.weight(1f),
            )
            Spacer(
                modifier = Modifier.width(12.dp),
            )
            TextP50(
                text = if (podcasts.size == enabledPodcastsCount) {
                    stringResource(LR.string.select_none)
                } else {
                    stringResource(LR.string.select_all)
                },
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.theme.colors.primaryIcon01,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(
                        role = Role.Button,
                        onClick = {
                            val enable = podcasts.size != enabledPodcastsCount
                            onChangeAllPodcasts(enable)
                        },
                    )
                    .padding(8.dp),
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp),
        )
        FadedLazyColumn(
            modifier = Modifier.weight(1f),
        ) {
            itemsIndexed(
                items = podcasts,
                key = { _, podcast -> podcast.uuid },
            ) { index, podcast ->
                PodcastRow(
                    podcast = podcast,
                    isSelected = podcast.isAutoDownloadNewEpisodes,
                    showDivider = index != podcasts.lastIndex,
                    modifier = Modifier.toggleable(
                        role = Role.Checkbox,
                        value = podcast.isAutoDownloadNewEpisodes,
                        onValueChange = { value -> onChangePodcast(podcast.uuid, value) },
                    ),
                )
            }
        }
    }
}

@Composable
private fun PodcastRow(
    podcast: Podcast,
    isSelected: Boolean,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            PodcastImage(
                uuid = podcast.uuid,
                imageSize = 56.dp,
            )
            Spacer(
                modifier = Modifier.width(16.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
            ) {
                TextH40(
                    text = podcast.title,
                )
                TextP50(
                    text = podcast.author,
                    color = MaterialTheme.theme.colors.primaryText02,
                )
            }
            Spacer(
                modifier = Modifier.width(16.dp),
            )
            Checkbox(
                checked = isSelected,
                onCheckedChange = null,
            )
        }
        if (showDivider) {
            HorizontalDivider(startIndent = 16.dp)
        }
    }
}

@PreviewRegularDevice
@Composable
private fun AutoDownloadSettingsPlaylistsPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppThemeWithBackground(themeType) {
        AutoDownloadSettingsPodcastsPage(
            podcasts = List(3) { index ->
                Podcast(
                    uuid = "podcast-uuid-$index",
                    title = "Podcast $index",
                    author = "Podcast author $index",
                    autoDownloadStatus = if (index % 2 == 0) Podcast.AUTO_DOWNLOAD_OFF else Podcast.AUTO_DOWNLOAD_NEW_EPISODES,
                )
            },
            onChangePodcast = { _, _ -> },
            onChangeAllPodcasts = {},
        )
    }
}

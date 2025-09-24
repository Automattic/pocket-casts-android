package au.com.shiftyjelly.pocketcasts.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import au.com.shiftyjelly.pocketcasts.compose.components.PlaylistArtwork
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistIcon
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.PodcastsRule
import au.com.shiftyjelly.pocketcasts.repositories.extensions.drawableId
import au.com.shiftyjelly.pocketcasts.repositories.playlist.ManualPlaylistPreview
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreview
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylistPreview
import au.com.shiftyjelly.pocketcasts.ui.extensions.getColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AutoDownloadSettingsPlaylistsPage(
    playlists: List<PlaylistPreview>,
    onChangePlaylist: (String, Boolean) -> Unit,
    onChangeAllPlaylists: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val usePlaylists = FeatureFlag.isEnabled(Feature.PLAYLISTS_REBRANDING, immutable = true)
    val enabledPlaylistsCount = remember(playlists) {
        playlists.count { it.settings.isAutoDownloadEnabled }
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
                text = if (usePlaylists) {
                    pluralStringResource(LR.plurals.playlists_selected_count, enabledPlaylistsCount, enabledPlaylistsCount)
                } else {
                    when (enabledPlaylistsCount) {
                        0 -> stringResource(LR.string.filters_chosen_none)
                        1 -> stringResource(LR.string.filters_chosen_singular)
                        else -> stringResource(LR.string.filters_chosen_plural, enabledPlaylistsCount)
                    }
                },
                color = MaterialTheme.theme.colors.primaryText02,
                modifier = Modifier.weight(1f),
            )
            Spacer(
                modifier = Modifier.width(12.dp),
            )
            TextP50(
                text = if (playlists.size == enabledPlaylistsCount) {
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
                            val enable = playlists.size != enabledPlaylistsCount
                            onChangeAllPlaylists(enable)
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
                items = playlists,
                key = { _, playlist -> playlist.uuid },
            ) { index, playlist ->
                PlaylistRow(
                    playlist = playlist,
                    isSelected = playlist.settings.isAutoDownloadEnabled,
                    showDivider = index != playlists.lastIndex,
                    usePlaylists = usePlaylists,
                    modifier = Modifier.toggleable(
                        role = Role.Checkbox,
                        value = playlist.settings.isAutoDownloadEnabled,
                        onValueChange = { value -> onChangePlaylist(playlist.uuid, value) },
                    ),
                )
            }
        }
    }
}

@Composable
private fun PlaylistRow(
    playlist: PlaylistPreview,
    isSelected: Boolean,
    showDivider: Boolean,
    usePlaylists: Boolean,
    modifier: Modifier = Modifier,
) {
    if (usePlaylists) {
        Column(
            modifier = modifier,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                PlaylistArtwork(
                    podcastUuids = playlist.artworkPodcastUuids,
                    artworkSize = 56.dp,
                )
                Spacer(
                    modifier = Modifier.width(16.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    TextH40(
                        text = playlist.title,
                    )
                    if (playlist.type == Playlist.Type.Smart) {
                        TextP50(
                            text = stringResource(LR.string.smart_playlist),
                            color = MaterialTheme.theme.colors.primaryText02,
                        )
                    }
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
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Image(
                painter = painterResource(playlist.icon.drawableId),
                colorFilter = ColorFilter.tint(Color(playlist.icon.getColor(LocalContext.current))),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
            Spacer(
                modifier = Modifier.width(16.dp),
            )
            TextH40(
                text = playlist.title,
                modifier = Modifier.weight(1f),
            )
            Spacer(
                modifier = Modifier.width(16.dp),
            )
            Checkbox(
                checked = isSelected,
                onCheckedChange = null,
            )
        }
    }
}

@PreviewRegularDevice
@Composable
private fun AutoDownloadSettingsPlaylistsPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppThemeWithBackground(themeType) {
        AutoDownloadSettingsPlaylistsPage(
            playlists = listOf(
                SmartPlaylistPreview(
                    uuid = "playlist-uuid-0",
                    title = "Smart Playlist 0",
                    episodeCount = 0,
                    artworkPodcastUuids = emptyList(),
                    settings = Playlist.Settings.ForPreview.copy(
                        isAutoDownloadEnabled = true,
                    ),
                    smartRules = SmartRules.Default,
                    icon = PlaylistIcon(0),
                ),
                SmartPlaylistPreview(
                    uuid = "playlist-uuid-1",
                    title = "Smart Playlist 1",
                    episodeCount = 0,
                    artworkPodcastUuids = emptyList(),
                    settings = Playlist.Settings.ForPreview,
                    smartRules = SmartRules.Default,
                    icon = PlaylistIcon(0),
                ),
                ManualPlaylistPreview(
                    uuid = "playlist-uuid-2",
                    title = "Manual Playlist 2",
                    episodeCount = 0,
                    artworkPodcastUuids = emptyList(),
                    settings = Playlist.Settings.ForPreview,
                    icon = PlaylistIcon(0),
                ),
                ManualPlaylistPreview(
                    uuid = "playlist-uuid-3",
                    title = "Manual Playlist 3",
                    episodeCount = 0,
                    artworkPodcastUuids = emptyList(),
                    settings = Playlist.Settings.ForPreview.copy(
                        isAutoDownloadEnabled = true,
                    ),
                    icon = PlaylistIcon(0),
                ),

            ),
            onChangePlaylist = { _, _ -> },
            onChangeAllPlaylists = {},
        )
    }
}

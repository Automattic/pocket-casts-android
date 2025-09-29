package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.FadedLazyColumn
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.PlaylistArtwork
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistIcon
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.PodcastsRule
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastSettingsViewModel.UiState
import au.com.shiftyjelly.pocketcasts.repositories.extensions.drawableId
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylistPreview
import au.com.shiftyjelly.pocketcasts.ui.extensions.getColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun PodcastSettingsPlaylistsPage(
    uiState: UiState,
    onAddPodcastToPlaylists: (List<String>) -> Unit,
    onRemovePodcastFromPlaylists: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val usePlaylists = FeatureFlag.isEnabled(Feature.PLAYLISTS_REBRANDING, immutable = true)
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
                    pluralStringResource(LR.plurals.playlists_selected_count, uiState.selectedPlaylists.size, uiState.selectedPlaylists.size)
                } else {
                    when (uiState.selectedPlaylists.size) {
                        0 -> stringResource(LR.string.filters_chosen_none)
                        1 -> stringResource(LR.string.filters_chosen_singular)
                        else -> stringResource(LR.string.filters_chosen_plural, uiState.selectedPlaylists.size)
                    }
                },
                color = MaterialTheme.theme.colors.primaryText02,
                modifier = Modifier.weight(1f),
            )
            Spacer(
                modifier = Modifier.width(12.dp),
            )
            TextP50(
                text = if (uiState.playlists.size == uiState.selectedPlaylists.size) {
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
                            val call = if (uiState.playlists.size == uiState.selectedPlaylists.size) {
                                onRemovePodcastFromPlaylists
                            } else {
                                onAddPodcastToPlaylists
                            }
                            call.invoke(uiState.playlists.map(SmartPlaylistPreview::uuid))
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
                items = uiState.playlists,
                key = { _, playlist -> playlist.uuid },
            ) { index, playlist ->
                val isSelected = when (val rule = playlist.smartRules.podcasts) {
                    is PodcastsRule.Any -> true
                    is PodcastsRule.Selected -> uiState.podcast.uuid in rule.uuids
                }
                PlaylistRow(
                    playlist = playlist,
                    isSelected = isSelected,
                    showDivider = index != uiState.playlists.lastIndex,
                    usePlaylists = usePlaylists,
                    modifier = Modifier.clickable(
                        role = Role.Button,
                        onClick = {
                            val call = if (isSelected) {
                                onRemovePodcastFromPlaylists
                            } else {
                                onAddPodcastToPlaylists
                            }
                            call.invoke(listOf(playlist.uuid))
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun PlaylistRow(
    playlist: SmartPlaylistPreview,
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
                    TextP50(
                        text = stringResource(LR.string.smart_playlist),
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

@Preview
@Composable
private fun PodcastSettingsPlaylistsPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppTheme(themeType) {
        PodcastSettingsPlaylistsPage(
            uiState = UiState(
                podcast = Podcast(
                    uuid = "podcast-uuid",
                    isShowNotifications = true,
                    autoDownloadStatus = Podcast.AUTO_DOWNLOAD_NEW_EPISODES,
                    autoAddToUpNext = Podcast.AutoAddUpNext.PLAY_NEXT,
                    overrideGlobalEffects = true,
                    playbackSpeed = 2.3,
                    startFromSecs = 30,
                    skipLastSecs = 60,
                ),
                playlists = List(3) { index ->
                    SmartPlaylistPreview(
                        uuid = "playlist-uuid-$index",
                        title = "Playlist $index",
                        episodeCount = 0,
                        artworkPodcastUuids = emptyList(),
                        settings = Playlist.Settings.ForPreview,
                        smartRules = SmartRules.Default.copy(
                            podcasts = PodcastsRule.Selected(uuids = setOf("podcast-uuid-$index")),
                        ),
                        icon = PlaylistIcon(0),
                    )
                },
                globalUpNextLimit = 100,
            ),
            onAddPodcastToPlaylists = {},
            onRemovePodcastFromPlaylists = {},
            modifier = Modifier.background(MaterialTheme.theme.colors.primaryUi02),
        )
    }
}

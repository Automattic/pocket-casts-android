package au.com.shiftyjelly.pocketcasts.playlists.smart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.FadedLazyColumn
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBanner
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.PodcastsRule
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AppliedRulesPage(
    playlistName: String,
    appliedRules: AppliedRules,
    availableEpisodes: List<PlaylistEpisode.Available>,
    totalEpisodeCount: Int,
    useEpisodeArtwork: Boolean,
    onClickRule: (RuleType) -> Unit,
    onClickClose: () -> Unit,
    modifier: Modifier = Modifier,
    areOtherOptionsExpanded: Boolean = false,
    toggleOtherOptions: (() -> Unit)? = null,
    onCreatePlaylist: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ThemedTopAppBar(
            navigationButton = NavigationButton.Close,
            style = ThemedTopAppBar.Style.Immersive,
            iconColor = MaterialTheme.theme.colors.primaryIcon03,
            windowInsets = WindowInsets(0),
            onNavigationClick = onClickClose,
        )
        Column(
            modifier = Modifier.weight(1f),
        ) {
            val activeRules = rememberActiveRules(appliedRules)
            val inactiveRules = rememberInactiveRules(activeRules)

            FadedLazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.weight(1f),
            ) {
                if (activeRules.isNotEmpty()) {
                    item(
                        key = "active-rules",
                        contentType = "active-rules",
                    ) {
                        ActiveRulesContent(
                            rules = activeRules,
                            episodeCount = totalEpisodeCount,
                            appliedRules = appliedRules,
                            onClickRule = onClickRule,
                        )
                    }
                    if (inactiveRules.isNotEmpty() && toggleOtherOptions != null) {
                        item(
                            key = "inactive-rules",
                            contentType = "inactive-rules",
                        ) {
                            InactiveRulesContent(
                                rules = inactiveRules,
                                isExpanded = areOtherOptionsExpanded,
                                onClickRule = onClickRule,
                                onToggleExpand = toggleOtherOptions,
                                modifier = Modifier.padding(top = 32.dp),
                            )
                        }
                    }
                    item(
                        key = "playlist-header",
                        contentType = "playlist-header",
                    ) {
                        TextH20(
                            text = stringResource(LR.string.preview_playlist, playlistName),
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 32.dp),
                        )
                    }
                    if (availableEpisodes.isNotEmpty()) {
                        items(
                            items = availableEpisodes,
                            key = { episode -> episode.uuid },
                            contentType = { "episode" },
                        ) { episodeWrapper ->
                            SmartEpisodeRow(
                                episode = episodeWrapper.episode,
                                useEpisodeArtwork = useEpisodeArtwork,
                            )
                        }
                    } else {
                        item(
                            key = "no-episodes",
                            contentType = "no-episodes",
                        ) {
                            Box(
                                contentAlignment = Alignment.TopCenter,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                NoContentBanner(
                                    iconResourceId = IR.drawable.ic_info,
                                    title = stringResource(LR.string.smart_playlist_create_no_content_title),
                                    body = stringResource(LR.string.smart_playlist_create_no_content_body),
                                    modifier = Modifier.padding(top = 56.dp),
                                )
                            }
                        }
                    }
                } else {
                    item(
                        key = "no-rules",
                        contentType = "no-rules",
                    ) {
                        NoRulesContent(
                            title = playlistName,
                            onClickRule = onClickRule,
                        )
                    }
                }
            }
            if (onCreatePlaylist != null) {
                RowButton(
                    text = stringResource(LR.string.create_smart_playlist),
                    enabled = appliedRules.isAnyRuleApplied,
                    onClick = onCreatePlaylist,
                    includePadding = false,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .navigationBarsPadding(),
                )
            }
        }
    }
}

@Composable
private fun ActiveRulesContent(
    rules: List<RuleType>,
    appliedRules: AppliedRules,
    episodeCount: Int,
    onClickRule: (RuleType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        TextH20(
            text = stringResource(LR.string.enabled_rules),
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        AppliedRulesColumn(
            rules = rules,
            appliedRules = appliedRules,
            episodeCount = episodeCount,
            onClickRule = onClickRule,
        )
    }
}

@Composable
private fun InactiveRulesContent(
    rules: List<RuleType>,
    isExpanded: Boolean,
    onClickRule: (RuleType) -> Unit,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    role = Role.Button,
                    onClick = onToggleExpand,
                    indication = null,
                    interactionSource = null,
                )
                .padding(horizontal = 16.dp)
                .semantics(true) {},
        ) {
            TextH20(
                text = stringResource(LR.string.other_options),
            )
            Spacer(
                modifier = Modifier.weight(1f),
            )
            val rotation by animateFloatAsState(if (isExpanded) 90f else 0f)
            Icon(
                painter = painterResource(IR.drawable.ic_chevron_trimmed),
                contentDescription = null,
                tint = MaterialTheme.theme.colors.primaryInteractive01,
                modifier = Modifier.rotate(rotation),
            )
            Spacer(
                modifier = Modifier.width(16.dp),
            )
        }
        AnimatedVisibility(
            visible = isExpanded,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Column {
                Spacer(
                    modifier = Modifier.height(16.dp),
                )
                RulesColumn(
                    rules = rules,
                    onClickRule = onClickRule,
                )
            }
        }
    }
}

@Composable
private fun NoRulesContent(
    title: String,
    onClickRule: (RuleType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        TextH20(
            text = title,
        )
        Spacer(
            modifier = Modifier.height(2.dp),
        )
        TextP50(
            text = stringResource(LR.string.smart_rules_description),
            color = MaterialTheme.theme.colors.primaryText02,
        )
        Spacer(
            modifier = Modifier.height(24.dp),
        )
        RulesColumn(
            rules = RuleType.entries,
            onClickRule = onClickRule,
        )
        Spacer(
            modifier = Modifier.height(24.dp),
        )
    }
}

@Composable
private fun rememberActiveRules(rules: AppliedRules): List<RuleType> {
    return remember(rules) {
        RuleType.entries.filter { type ->
            when (type) {
                RuleType.EpisodeStatus -> rules.episodeStatus != null
                RuleType.DownloadStatus -> rules.downloadStatus != null
                RuleType.MediaType -> rules.mediaType != null
                RuleType.ReleaseDate -> rules.releaseDate != null
                RuleType.Starred -> rules.starred != null
                RuleType.Podcasts -> rules.podcasts != null
                RuleType.EpisodeDuration -> rules.episodeDuration != null
            }
        }
    }
}

@Composable
private fun rememberInactiveRules(activeRules: List<RuleType>): List<RuleType> {
    return remember(activeRules) {
        RuleType.entries - activeRules
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun AppliedRulesPageNoRulesPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    var expanded by remember { mutableStateOf(false) }
    AppThemeWithBackground(themeType) {
        AppliedRulesPage(
            playlistName = "Comedy",
            appliedRules = AppliedRules.Companion.Empty,
            availableEpisodes = emptyList(),
            totalEpisodeCount = 0,
            useEpisodeArtwork = false,
            areOtherOptionsExpanded = expanded,
            onCreatePlaylist = {},
            onClickRule = {},
            onClickClose = {},
            toggleOtherOptions = { expanded = !expanded },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun AppliedRulesPageEpisodesPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    var expanded by remember { mutableStateOf(false) }
    AppThemeWithBackground(themeType) {
        AppliedRulesPage(
            playlistName = "Comedy",
            appliedRules = AppliedRules.Companion.Empty.copy(
                podcasts = PodcastsRule.Any,
            ),
            availableEpisodes = List(10) { index ->
                PlaylistEpisode.Available(
                    PodcastEpisode(
                        uuid = "uuid-$index",
                        title = "Episode $index",
                        duration = 6000.0,
                        publishedDate = Date(0),
                    ),
                )
            },
            totalEpisodeCount = 10,
            useEpisodeArtwork = false,
            areOtherOptionsExpanded = expanded,
            onCreatePlaylist = {},
            onClickRule = {},
            onClickClose = {},
            toggleOtherOptions = { expanded = !expanded },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun AppliedRulesPageNoEpisodesPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    var expanded by remember { mutableStateOf(false) }
    AppThemeWithBackground(themeType) {
        AppliedRulesPage(
            playlistName = "Comedy",
            appliedRules = AppliedRules.Companion.Empty.copy(
                podcasts = PodcastsRule.Any,
            ),
            availableEpisodes = emptyList(),
            totalEpisodeCount = 0,
            useEpisodeArtwork = false,
            areOtherOptionsExpanded = expanded,
            onCreatePlaylist = {},
            onClickRule = {},
            onClickClose = {},
            toggleOtherOptions = { expanded = !expanded },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

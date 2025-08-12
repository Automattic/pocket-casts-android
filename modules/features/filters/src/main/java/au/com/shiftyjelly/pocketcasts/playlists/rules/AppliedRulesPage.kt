package au.com.shiftyjelly.pocketcasts.playlists.rules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.FadedLazyColumn
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBanner
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.DownloadStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.EpisodeDurationRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.MediaTypeRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.PodcastsRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.ReleaseDateRule
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun AppliedRulesPage(
    playlistTitle: String,
    appliedRules: AppliedRules,
    availableEpisodes: List<PodcastEpisode>,
    totalEpisodeCount: Int,
    useEpisodeArtwork: Boolean,
    areOtherOptionsExpanded: Boolean,
    onCreatePlaylist: () -> Unit,
    onClickRule: (RuleType) -> Unit,
    toggleOtherOptions: () -> Unit,
    onClickClose: () -> Unit,
    modifier: Modifier = Modifier,
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
                            totalEpisodeCount = totalEpisodeCount,
                            appliedRules = appliedRules,
                            onClickRule = onClickRule,
                        )
                    }
                    if (inactiveRules.isNotEmpty()) {
                        item(
                            key = "inactive-rules",
                            contentType = "inactive-rules",
                        ) {
                            InactiveRulesContent(
                                rules = inactiveRules,
                                isExpanded = areOtherOptionsExpanded,
                                onClickRule = onClickRule,
                                onToggleExpand = toggleOtherOptions,
                                modifier = Modifier.padding(vertical = 32.dp),
                            )
                        }
                    }
                    item(
                        key = "playlist-header",
                        contentType = "playlist-header",
                    ) {
                        TextH20(
                            text = stringResource(LR.string.preview_playlist, playlistTitle),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    if (availableEpisodes.isNotEmpty()) {
                        items(
                            items = availableEpisodes,
                            key = { episode -> episode.uuid },
                            contentType = { "episode" },
                        ) { episode ->
                            EpisodeRow(
                                episode = episode,
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
                            title = playlistTitle,
                            onClickRule = onClickRule,
                        )
                    }
                }
            }
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

@Composable
private fun ActiveRulesContent(
    rules: List<RuleType>,
    appliedRules: AppliedRules,
    totalEpisodeCount: Int,
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
        Rules(
            ruleTypes = rules,
            description = { rule -> appliedRules.description(rule, totalEpisodeCount) },
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
                Rules(
                    ruleTypes = rules,
                    description = { null },
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
        Rules(
            ruleTypes = RuleType.entries,
            description = { null },
            onClickRule = onClickRule,
        )
        Spacer(
            modifier = Modifier.height(24.dp),
        )
    }
}

@Composable
private fun Rules(
    ruleTypes: List<RuleType>,
    description: @Composable (RuleType) -> String?,
    onClickRule: (RuleType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.theme.colors.primaryUi02Active),
    ) {
        ruleTypes.forEachIndexed { index, type ->
            RuleRow(
                ruleType = type,
                description = description(type),
                onClick = { onClickRule(type) },
                showDivider = index != ruleTypes.lastIndex,
            )
        }
    }
}

@Composable
private fun RuleRow(
    ruleType: RuleType,
    description: String?,
    showDivider: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(
                role = Role.Button,
                onClick = { onClick() },
            )
            .semantics(mergeDescendants = true) {},
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Icon(
                painter = painterResource(ruleType.iconId),
                contentDescription = null,
                tint = MaterialTheme.theme.colors.primaryIcon03,
                modifier = Modifier.size(24.dp),
            )
            Spacer(
                modifier = Modifier.width(16.dp),
            )
            Row(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(ruleType.titleId),
                    color = MaterialTheme.theme.colors.primaryText01,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(
                    modifier = Modifier.weight(1f),
                )
                if (description != null) {
                    Spacer(
                        modifier = Modifier.width(8.dp),
                    )
                    Text(
                        text = description,
                        color = MaterialTheme.theme.colors.primaryText02,
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                    )
                }
            }
            Spacer(
                modifier = Modifier.width(16.dp),
            )
            Image(
                painter = painterResource(IR.drawable.ic_chevron_trimmed),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon02),
            )
        }
        if (showDivider) {
            HorizontalDivider(
                thickness = 0.5.dp,
                startIndent = 56.dp,
            )
        } else {
            Spacer(
                modifier = Modifier.height(0.5.dp),
            )
        }
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

@Composable
@ReadOnlyComposable
private fun AppliedRules.description(ruleType: RuleType, episodeCount: Int) = when (ruleType) {
    RuleType.Podcasts -> when (podcasts) {
        is PodcastsRule.Any -> stringResource(LR.string.all)
        is PodcastsRule.Selected -> podcasts.uuids.size.toString()
        null -> null
    }

    RuleType.EpisodeStatus -> {
        if (episodeStatus != null) {
            when {
                episodeStatus.unplayed -> when {
                    episodeStatus.inProgress && episodeStatus.completed -> {
                        stringResource(LR.string.episode_status_rule_description, stringResource(LR.string.unplayed), 2)
                    }
                    episodeStatus.inProgress || episodeStatus.completed -> {
                        stringResource(LR.string.episode_status_rule_description, stringResource(LR.string.unplayed), 1)
                    }
                    else -> {
                        stringResource(LR.string.unplayed)
                    }
                }

                episodeStatus.inProgress -> when {
                    episodeStatus.completed -> {
                        stringResource(LR.string.episode_status_rule_description, stringResource(LR.string.in_progress_uppercase), 1)
                    }

                    else -> {
                        stringResource(LR.string.in_progress_uppercase)
                    }
                }

                episodeStatus.completed -> {
                    stringResource(LR.string.played)
                }
                else -> null
            }
        } else {
            null
        }
    }

    RuleType.ReleaseDate -> when (releaseDate) {
        ReleaseDateRule.AnyTime -> stringResource(LR.string.filters_time_anytime)
        ReleaseDateRule.Last24Hours -> stringResource(LR.string.filters_time_24_hours)
        ReleaseDateRule.Last3Days -> stringResource(LR.string.filters_time_3_days)
        ReleaseDateRule.LastWeek -> stringResource(LR.string.filters_time_week)
        ReleaseDateRule.Last2Weeks -> stringResource(LR.string.filters_time_2_weeks)
        ReleaseDateRule.LastMonth -> stringResource(LR.string.filters_time_month)
        null -> null
    }

    RuleType.EpisodeDuration -> when (episodeDuration) {
        is EpisodeDurationRule.Any -> stringResource(LR.string.off)
        is EpisodeDurationRule.Constrained -> {
            val context = LocalContext.current
            val min = TimeHelper.getTimeDurationShortString(episodeDuration.longerThan.inWholeMilliseconds, context)
            val max = TimeHelper.getTimeDurationShortString(episodeDuration.shorterThan.inWholeMilliseconds, context)
            "$min - $max"
        }
        null -> null
    }

    RuleType.DownloadStatus -> when (downloadStatus) {
        DownloadStatusRule.Any -> stringResource(LR.string.all)
        DownloadStatusRule.Downloaded -> stringResource(LR.string.downloaded)
        DownloadStatusRule.NotDownloaded -> stringResource(LR.string.not_downloaded)
        null -> null
    }

    RuleType.MediaType -> when (mediaType) {
        MediaTypeRule.Any -> stringResource(LR.string.all)
        MediaTypeRule.Audio -> stringResource(LR.string.audio)
        MediaTypeRule.Video -> stringResource(LR.string.video)
        null -> null
    }

    RuleType.Starred -> when (starred) {
        SmartRules.StarredRule.Any -> stringResource(LR.string.off)
        SmartRules.StarredRule.Starred -> episodeCount.toString()
        null -> null
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
            playlistTitle = "Comedy",
            appliedRules = AppliedRules.Empty,
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
private fun AppliedRulesPageEpisodessPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    var expanded by remember { mutableStateOf(false) }
    AppThemeWithBackground(themeType) {
        AppliedRulesPage(
            playlistTitle = "Comedy",
            appliedRules = AppliedRules.Empty.copy(
                podcasts = PodcastsRule.Any,
            ),
            availableEpisodes = List(10) { index ->
                PodcastEpisode(
                    uuid = "uuid-$index",
                    title = "Episode $index",
                    duration = 6000.0,
                    publishedDate = Date(0),
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
            playlistTitle = "Comedy",
            appliedRules = AppliedRules.Empty.copy(
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

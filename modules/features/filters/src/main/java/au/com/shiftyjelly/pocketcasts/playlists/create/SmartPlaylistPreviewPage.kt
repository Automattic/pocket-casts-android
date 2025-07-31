package au.com.shiftyjelly.pocketcasts.playlists.create

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.IconButton
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.EpisodeImage
import au.com.shiftyjelly.pocketcasts.compose.components.FadedLazyColumn
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBanner
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.text.toAnnotatedString
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.PodcastsRule
import au.com.shiftyjelly.pocketcasts.playlists.create.CreatePlaylistViewModel.AppliedRules
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getSummaryText
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory.PlaceholderType
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

enum class RuleType(
    @DrawableRes val iconId: Int,
    @StringRes val titleId: Int,
) {
    Podcasts(
        iconId = IR.drawable.ic_podcasts,
        titleId = LR.string.podcasts,
    ),
    EpisodeStatus(
        iconId = IR.drawable.ic_filters_play,
        titleId = LR.string.filters_chip_episode_status,
    ),
    ReleaseDate(
        iconId = IR.drawable.ic_calendar,
        titleId = LR.string.filters_release_date,
    ),
    EpisodeDuration(
        iconId = IR.drawable.ic_filters_clock,
        titleId = LR.string.filters_duration,
    ),
    DownloadStatus(
        iconId = IR.drawable.ic_profile_download,
        titleId = LR.string.filters_chip_download_status,
    ),
    MediaType(
        iconId = IR.drawable.ic_headphone,
        titleId = LR.string.filters_chip_media_type,
    ),
    Starred(
        iconId = IR.drawable.ic_star,
        titleId = LR.string.filters_chip_starred,
    ),
}

@Composable
fun SmartPlaylistPreviewPage(
    playlistTitle: String,
    appliedRules: AppliedRules,
    availableEpisodes: List<PodcastEpisode>,
    useEpisodeArtwork: Boolean,
    areOtherOptionsExpanded: Boolean,
    onCreateSmartPlaylist: () -> Unit,
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
        IconButton(
            onClick = onClickClose,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(au.com.shiftyjelly.pocketcasts.localization.R.string.close),
                tint = MaterialTheme.theme.colors.primaryIcon03,
            )
        }
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
                onClick = onCreateSmartPlaylist,
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
            description = { rule -> appliedRules.description(rule) },
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
private fun EpisodeRow(
    episode: PodcastEpisode,
    useEpisodeArtwork: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(vertical = 12.dp, horizontal = 12.dp),
        ) {
            EpisodeImage(
                episode = episode,
                useEpisodeArtwork = useEpisodeArtwork,
                placeholderType = PlaceholderType.Small,
                corners = 4.dp,
                modifier = Modifier.size(56.dp),
            )
            Spacer(
                modifier = Modifier.width(12.dp),
            )
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight(),
            ) {
                TextC70(
                    text = episode.rememberHeaderText(),
                )
                TextH40(
                    text = episode.title,
                    lineHeight = 15.sp,
                    maxLines = 2,
                )
                TextH60(
                    text = episode.rememberTimeLeftText(),
                    color = MaterialTheme.theme.colors.primaryText02,
                )
            }
        }
        HorizontalDivider(
            startIndent = 12.dp,
        )
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
            Text(
                text = stringResource(ruleType.titleId),
                color = MaterialTheme.theme.colors.primaryText01,
                fontSize = 17.sp,
                lineHeight = 22.sp,
            )
            Spacer(
                modifier = Modifier.weight(1f),
            )
            if (description != null) {
                Text(
                    text = description,
                    color = MaterialTheme.theme.colors.primaryText02,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                )
                Spacer(
                    modifier = Modifier.width(14.dp),
                )
            }
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
private fun PodcastEpisode.rememberHeaderText(): AnnotatedString {
    val context = LocalContext.current
    val formatter = remember(context) { RelativeDateFormatter(context) }
    return remember(playingStatus, isArchived) {
        val tintColor = context.getThemeColor(UR.attr.primary_icon_01)
        val spannable = getSummaryText(formatter, tintColor, showDuration = false, context)
        spannable.toAnnotatedString()
    }
}

@Composable
private fun PodcastEpisode.rememberTimeLeftText(): String {
    val context = LocalContext.current
    return remember(playedUpToMs, durationMs, isInProgress, context) {
        TimeHelper.getTimeLeft(playedUpToMs, durationMs.toLong(), isInProgress, context).text
    }
}

@Composable
@ReadOnlyComposable
private fun AppliedRules.description(ruleType: RuleType) = when (ruleType) {
    RuleType.Podcasts -> when (podcasts) {
        is PodcastsRule.Any -> stringResource(LR.string.all)
        is PodcastsRule.Selected -> podcasts.uuids.size.toString()
        null -> null
    }

    RuleType.EpisodeStatus -> TODO()
    RuleType.ReleaseDate -> TODO()
    RuleType.EpisodeDuration -> TODO()
    RuleType.DownloadStatus -> TODO()
    RuleType.MediaType -> TODO()
    RuleType.Starred -> TODO()
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun SmartPlaylistsPreviewNoRulesPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    var expanded by remember { mutableStateOf(false) }
    AppThemeWithBackground(themeType) {
        SmartPlaylistPreviewPage(
            playlistTitle = "Comedy",
            appliedRules = AppliedRules.Empty,
            availableEpisodes = emptyList(),
            useEpisodeArtwork = false,
            areOtherOptionsExpanded = expanded,
            onCreateSmartPlaylist = {},
            onClickRule = {},
            onClickClose = {},
            toggleOtherOptions = { expanded = !expanded },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun SmartPlaylistsPreviewEpisodessPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    var expanded by remember { mutableStateOf(false) }
    AppThemeWithBackground(themeType) {
        SmartPlaylistPreviewPage(
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
            useEpisodeArtwork = false,
            areOtherOptionsExpanded = expanded,
            onCreateSmartPlaylist = {},
            onClickRule = {},
            onClickClose = {},
            toggleOtherOptions = { expanded = !expanded },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun SmartPlaylistsPreviewNoEpisodesPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    var expanded by remember { mutableStateOf(false) }
    AppThemeWithBackground(themeType) {
        SmartPlaylistPreviewPage(
            playlistTitle = "Comedy",
            appliedRules = AppliedRules.Empty.copy(
                podcasts = PodcastsRule.Any,
            ),
            availableEpisodes = emptyList(),
            useEpisodeArtwork = false,
            areOtherOptionsExpanded = expanded,
            onCreateSmartPlaylist = {},
            onClickRule = {},
            onClickClose = {},
            toggleOtherOptions = { expanded = !expanded },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

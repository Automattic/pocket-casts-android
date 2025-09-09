package au.com.shiftyjelly.pocketcasts.playlists.smart

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.DownloadStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.EpisodeDurationRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.MediaTypeRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.PodcastsRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.ReleaseDateRule
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AppliedRulesColumn(
    rules: List<RuleType>,
    appliedRules: AppliedRules,
    episodeCount: Int,
    onClickRule: (RuleType) -> Unit,
    modifier: Modifier = Modifier,
) {
    RulesColumn(
        rules = rules,
        description = { rule -> appliedRules.description(rule, episodeCount) },
        onClickRule = onClickRule,
        modifier = modifier,
    )
}

@Composable
internal fun RulesColumn(
    rules: List<RuleType>,
    onClickRule: (RuleType) -> Unit,
    modifier: Modifier = Modifier,
    description: @Composable (RuleType) -> String? = { null },
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.theme.colors.primaryUi02Active),
    ) {
        rules.forEachIndexed { index, type ->
            RuleRow(
                ruleType = type,
                description = description(type),
                onClick = { onClickRule(type) },
                showDivider = index != rules.lastIndex,
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

@Preview
@Composable
private fun AppliedRulesColumnPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppTheme(themeType) {
        AppliedRulesColumn(
            rules = RuleType.entries,
            appliedRules = AppliedRules(
                episodeStatus = SmartRules.Default.episodeStatus,
                downloadStatus = DownloadStatusRule.Downloaded,
                mediaType = SmartRules.Default.mediaType,
                releaseDate = SmartRules.Default.releaseDate,
                starred = SmartRules.StarredRule.Starred,
                podcasts = SmartRules.Default.podcasts,
                episodeDuration = EpisodeDurationRule.Constrained(
                    longerThan = 15.minutes,
                    shorterThan = 1.hours + 10.minutes,
                ),
            ),
            episodeCount = 17,
            onClickRule = {},
        )
    }
}

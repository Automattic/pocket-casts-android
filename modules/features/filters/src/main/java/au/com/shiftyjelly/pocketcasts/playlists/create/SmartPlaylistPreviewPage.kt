package au.com.shiftyjelly.pocketcasts.playlists.create

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.FadedLazyColumn
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.playlists.create.CreatePlaylistViewModel.AppliedRules
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylistDraft
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class RuleType(
    @DrawableRes val iconId: Int,
    @StringRes val titleId: Int,
) {
    Podcasts(
        iconId = IR.drawable.ic_rule_podcasts,
        titleId = LR.string.podcasts,
    ),
    EpisodeStatus(
        iconId = IR.drawable.ic_rule_episode_status,
        titleId = LR.string.filters_chip_episode_status,
    ),
    ReleaseDate(
        iconId = IR.drawable.ic_rule_release_date,
        titleId = LR.string.filters_release_date,
    ),
    EpisodeDuration(
        iconId = IR.drawable.ic_rule_duration,
        titleId = LR.string.filters_duration,
    ),
    DownloadStatus(
        iconId = IR.drawable.ic_rule_download_status,
        titleId = LR.string.filters_chip_download_status,
    ),
    MediaType(
        iconId = IR.drawable.ic_rule_media_type,
        titleId = LR.string.filters_chip_media_type,
    ),
    Starred(
        iconId = IR.drawable.ic_rule_starred,
        titleId = LR.string.filters_chip_starred,
    ),
}

@Composable
fun SmartPlaylistPreviewPage(
    playlistTitle: String,
    appliedRules: AppliedRules,
    onCreateSmartPlaylist: () -> Unit,
    onClickRule: (RuleType) -> Unit,
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
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        ) {
            val activeRules = rememberAppliedRules(appliedRules)
            FadedLazyColumn(
                modifier = Modifier.weight(1f),
            ) {
                if (activeRules.isEmpty()) {
                    item {
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
                modifier = Modifier.navigationBarsPadding(),
            )
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
        modifier = modifier,
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
    description: (RuleType) -> String?,
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
private fun rememberAppliedRules(rules: AppliedRules): List<RuleType> {
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

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun SmartPlaylistPreviewPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppThemeWithBackground(themeType) {
        SmartPlaylistPreviewPage(
            playlistTitle = "Comedy",
            appliedRules = AppliedRules.Empty,
            onCreateSmartPlaylist = {},
            onClickRule = {},
            onClickClose = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

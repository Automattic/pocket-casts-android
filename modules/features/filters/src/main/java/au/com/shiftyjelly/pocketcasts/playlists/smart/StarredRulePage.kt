package au.com.shiftyjelly.pocketcasts.playlists.smart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.PreviewRegularDevice
import au.com.shiftyjelly.pocketcasts.compose.components.FadedLazyColumn
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.StarredRule
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import java.util.Date
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun StarredRulePage(
    selectedRule: StarredRule,
    starredEpisodes: List<PlaylistEpisode.Available>,
    useEpisodeArtwork: Boolean,
    onChangeUseStarredEpisodes: (Boolean) -> Unit,
    onSaveRule: () -> Unit,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val useStarredEpisodes = when (selectedRule) {
        StarredRule.Starred -> true
        StarredRule.Any -> false
    }

    RulePage(
        title = stringResource(LR.string.filters_title_starred),
        onSaveRule = onSaveRule,
        onClickBack = onClickBack,
        modifier = modifier,
    ) { bottomPadding ->
        Column(
            modifier = Modifier.padding(
                top = 24.dp,
                bottom = if (useStarredEpisodes) 0.dp else bottomPadding,
            ),
        ) {
            StarredSwitchRow(
                useStarredEpisodes = useStarredEpisodes,
                onChangeUseStarredEpisodes = onChangeUseStarredEpisodes,
            )
            Spacer(
                modifier = Modifier.height(12.dp),
            )
            AnimatedVisibility(
                visible = useStarredEpisodes,
                enter = fadeIn,
                exit = fadeOut,
                modifier = Modifier.weight(1f),
            ) {
                EpisodesColumn(
                    episodes = starredEpisodes,
                    useEpisodeArtwork = useEpisodeArtwork,
                    bottomPadding = bottomPadding,
                )
            }
        }
    }
}

@Composable
private fun StarredSwitchRow(
    useStarredEpisodes: Boolean,
    onChangeUseStarredEpisodes: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .toggleable(
                role = Role.Switch,
                value = useStarredEpisodes,
                onValueChange = onChangeUseStarredEpisodes,
            )
            .padding(horizontal = 16.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .weight(1f)
                .heightIn(min = 32.dp),
        ) {
            TextH30(
                text = stringResource(LR.string.smart_rule_starred_label),
                modifier = Modifier.widthIn(max = 280.dp),
            )
            Spacer(
                modifier = Modifier.height(4.dp),
            )
            TextP50(
                text = if (useStarredEpisodes) {
                    stringResource(LR.string.smart_rule_starred_description)
                } else {
                    stringResource(LR.string.smart_rule_starred_description_disabled)
                },
                color = MaterialTheme.theme.colors.primaryText02,
                modifier = Modifier.widthIn(max = 280.dp),
            )
        }
        Spacer(
            modifier = Modifier.width(16.dp),
        )
        Switch(
            checked = useStarredEpisodes,
            onCheckedChange = null,
        )
    }
}

@Composable
private fun EpisodesColumn(
    episodes: List<PlaylistEpisode.Available>,
    useEpisodeArtwork: Boolean,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    FadedLazyColumn(
        contentPadding = PaddingValues(bottom = bottomPadding),
        modifier = modifier,
    ) {
        items(episodes) { episodeWrapper ->
            SmartEpisodeRow(
                episode = episodeWrapper.episode,
                useEpisodeArtwork = useEpisodeArtwork,
            )
        }
    }
}

private val fadeIn = fadeIn()
private val fadeOut = fadeOut()

@Composable
@PreviewRegularDevice
private fun StarredRulePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    var rule by remember { mutableStateOf(StarredRule.Starred) }

    AppThemeWithBackground(themeType) {
        StarredRulePage(
            selectedRule = rule,
            starredEpisodes = List(10) { index ->
                PlaylistEpisode.Available(
                    PodcastEpisode(
                        uuid = "uuid-$index",
                        title = "Episode $index",
                        duration = 6000.0,
                        publishedDate = Date(0),
                    ),
                )
            },
            useEpisodeArtwork = false,
            onChangeUseStarredEpisodes = { useStarred ->
                rule = if (useStarred) StarredRule.Starred else StarredRule.Any
            },
            onSaveRule = {},
            onClickBack = {},
        )
    }
}

package au.com.shiftyjelly.pocketcasts.playlists.rules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.PreviewRegularDevice
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.FadedLazyColumn
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun PodcastsRulePage(
    useAllPodcasts: Boolean,
    selectedPodcastUuids: Set<String>,
    podcasts: List<Podcast>,
    onToggleAllPodcasts: (Boolean) -> Unit,
    onSelectPodcast: (String) -> Unit,
    onDeselectPodcast: (String) -> Unit,
    onSaveRule: () -> Unit,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RulePage(
        title = stringResource(LR.string.smart_rule_podcasts_title),
        onSaveRule = onSaveRule,
        onClickBack = onClickBack,
        modifier = modifier,
    ) { bottomPadding ->
        Column(
            modifier = Modifier.padding(top = 24.dp),
        ) {
            AllPodcastsToggle(
                useAllPodcasts = useAllPodcasts,
                onToggleAllPodcasts = onToggleAllPodcasts,
            )
            Spacer(
                modifier = Modifier.height(12.dp),
            )
            PodcastsColumn(
                useAllPodcasts = useAllPodcasts,
                selectedPodcastUuids = selectedPodcastUuids,
                podcasts = podcasts,
                onSelectPodcast = onSelectPodcast,
                onDeselectPodcast = onDeselectPodcast,
                bottomPadding = bottomPadding,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AllPodcastsToggle(
    useAllPodcasts: Boolean,
    onToggleAllPodcasts: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .toggleable(
                role = Role.Switch,
                value = useAllPodcasts,
                onValueChange = onToggleAllPodcasts,
            )
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .weight(1f)
                .heightIn(min = 32.dp),
        ) {
            TextH30(
                text = stringResource(LR.string.smart_rule_podcasts_all_label),
                modifier = Modifier.widthIn(max = 280.dp),
            )
            AnimatedVisibility(
                visible = useAllPodcasts,
            ) {
                Column {
                    Spacer(
                        modifier = Modifier.height(4.dp),
                    )
                    TextP50(
                        text = stringResource(LR.string.smart_rule_podcasts_all_description),
                        modifier = Modifier.widthIn(max = 280.dp),
                    )
                }
            }
        }
        Spacer(
            modifier = Modifier.width(16.dp),
        )
        Switch(
            checked = useAllPodcasts,
            onCheckedChange = null,
        )
    }
}

@Composable
private fun PodcastsColumn(
    useAllPodcasts: Boolean,
    selectedPodcastUuids: Set<String>,
    podcasts: List<Podcast>,
    onSelectPodcast: (String) -> Unit,
    onDeselectPodcast: (String) -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val alpha by animateFloatAsState(if (useAllPodcasts) 0.4f else 1f)
    FadedLazyColumn(
        contentPadding = PaddingValues(bottom = bottomPadding),
        modifier = modifier.alpha(alpha),
    ) {
        items(podcasts) { podcast ->
            PodcastRow(
                title = podcast.title,
                author = podcast.author,
                uuid = podcast.uuid,
                isSelected = if (useAllPodcasts) {
                    true
                } else {
                    podcast.uuid in selectedPodcastUuids
                },
                enabled = !useAllPodcasts,
                onToggle = { isSelected ->
                    if (isSelected) {
                        onSelectPodcast(podcast.uuid)
                    } else {
                        onDeselectPodcast(podcast.uuid)
                    }
                },
            )
        }
    }
}

@Composable
private fun PodcastRow(
    title: String,
    author: String,
    uuid: String,
    isSelected: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .toggleable(
                role = Role.Button,
                value = isSelected,
                enabled = enabled,
                onValueChange = onToggle,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        PodcastImage(
            uuid = uuid,
            dropShadow = false,
            cornerSize = 4.dp,
            modifier = Modifier.size(56.dp),
        )
        Spacer(
            modifier = Modifier.width(12.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
        ) {
            TextH40(
                text = title,
                maxLines = 2,
            )
            Spacer(
                modifier = Modifier.height(2.dp),
            )
            TextH60(
                text = author,
                color = MaterialTheme.theme.colors.primaryText02,
                maxLines = 1,
            )
        }
        Spacer(
            modifier = Modifier.width(12.dp),
        )
        Checkbox(
            checked = isSelected,
            onCheckedChange = null,
        )
    }
}

@Composable
@PreviewRegularDevice
private fun PodcastsRulePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    var useAllPodcasts by remember { mutableStateOf(true) }
    var podcastUuids by remember { mutableStateOf(emptySet<String>()) }

    AppThemeWithBackground(themeType) {
        PodcastsRulePage(
            useAllPodcasts = useAllPodcasts,
            selectedPodcastUuids = podcastUuids,
            podcasts = List(10) { index ->
                Podcast(uuid = "id-$index", title = "Title $index", author = "Author $index")
            },
            onToggleAllPodcasts = { useAllPodcasts = it },
            onSelectPodcast = { podcastUuids += it },
            onDeselectPodcast = { podcastUuids -= it },
            onSaveRule = {},
            onClickBack = {},
        )
    }
}

package au.com.shiftyjelly.pocketcasts.playlists.smart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.PreviewRegularDevice
import au.com.shiftyjelly.pocketcasts.compose.components.FadedLazyColumn
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImageDeprecated
import au.com.shiftyjelly.pocketcasts.compose.components.SearchBar
import au.com.shiftyjelly.pocketcasts.compose.components.SearchBarStyle
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun PodcastsRulePage(
    useAllPodcasts: Boolean,
    selectedPodcastUuids: Set<String>,
    podcasts: List<Podcast>,
    onChangeUseAllPodcasts: (Boolean) -> Unit,
    onSelectPodcast: (String) -> Unit,
    onDeselectPodcast: (String) -> Unit,
    onSaveRule: () -> Unit,
    modifier: Modifier = Modifier,
    searchState: TextFieldState = rememberTextFieldState(),
) {
    RulePage(
        title = stringResource(LR.string.filters_choose_podcasts),
        onSaveRule = onSaveRule,
        isSaveEnabled = useAllPodcasts || selectedPodcastUuids.isNotEmpty(),
        modifier = modifier,
    ) { bottomPadding ->
        val imePadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier.padding(top = 24.dp),
        ) {
            AllPodcastsToggle(
                useAllPodcasts = useAllPodcasts,
                onChangeUseAllPodcasts = onChangeUseAllPodcasts,
            )
            SearchBar(
                state = searchState,
                placeholder = stringResource(LR.string.search),
                style = SearchBarStyle.Small,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
            )
            PodcastsColumn(
                useAllPodcasts = useAllPodcasts,
                selectedPodcastUuids = selectedPodcastUuids,
                podcasts = podcasts,
                onSelectPodcast = onSelectPodcast,
                onDeselectPodcast = onDeselectPodcast,
                bottomPadding = bottomPadding + imePadding,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun PodcastRulesActions(
    useAllPodcasts: Boolean,
    selectedPodcastUuids: Set<String>,
    podcasts: List<Podcast>,
    onSelectAllPodcasts: () -> Unit,
    onDeselectAllPodcasts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        enter = fadeIn,
        exit = fadeOut,
        visible = !useAllPodcasts,
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable(
                    role = Role.Button,
                    onClick = {
                        if (podcasts.size == selectedPodcastUuids.size) {
                            onDeselectAllPodcasts()
                        } else {
                            onSelectAllPodcasts()
                        }
                    },
                )
                .padding(horizontal = 16.dp)
                .semantics(mergeDescendants = true) {},
        ) {
            Text(
                text = if (podcasts.size == selectedPodcastUuids.size) {
                    stringResource(LR.string.deselect_all)
                } else {
                    stringResource(LR.string.select_all)
                },
                color = MaterialTheme.theme.colors.primaryText02,
                fontSize = 17.sp,
                lineHeight = 22.sp,
            )
        }
    }
}

@Composable
private fun AllPodcastsToggle(
    useAllPodcasts: Boolean,
    onChangeUseAllPodcasts: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .toggleable(
                role = Role.Switch,
                value = useAllPodcasts,
                onValueChange = onChangeUseAllPodcasts,
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
            Spacer(
                modifier = Modifier.height(4.dp),
            )
            TextP50(
                text = if (useAllPodcasts) {
                    stringResource(LR.string.smart_rule_podcasts_all_description)
                } else {
                    stringResource(LR.string.smart_rule_podcasts_all_description_disabled)
                },
                color = MaterialTheme.theme.colors.primaryText02,
                modifier = Modifier.widthIn(max = 280.dp),
            )
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
    val listState = rememberLazyListState()
    LaunchedEffect(podcasts) {
        listState.scrollToItem(0)
    }

    val alpha by animateFloatAsState(if (useAllPodcasts) 0.4f else 1f)

    FadedLazyColumn(
        contentPadding = PaddingValues(bottom = bottomPadding),
        state = listState,
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
        @Suppress("DEPRECATION")
        PodcastImageDeprecated(
            uuid = uuid,
            cornerSize = 4.dp,
            elevation = 2.dp,
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

private val fadeIn = fadeIn()
private val fadeOut = fadeOut()

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
            onChangeUseAllPodcasts = { useAllPodcasts = it },
            onSelectPodcast = { podcastUuids += it },
            onDeselectPodcast = { podcastUuids -= it },
            onSaveRule = {},
        )
    }
}

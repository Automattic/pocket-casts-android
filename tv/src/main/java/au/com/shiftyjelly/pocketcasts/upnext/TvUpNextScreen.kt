package au.com.shiftyjelly.pocketcasts.upnext

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeActionsModal
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeListItem
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.theme.TvTextStyles
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvUpNextScreen(
    onNavigateToDiscover: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TvUpNextViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onShown()
    }

    TvUpNextContent(
        uiState = uiState,
        onNavigateToDiscover = onNavigateToDiscover,
        modifier = modifier,
    )
}

@Composable
private fun TvUpNextContent(
    uiState: TvUpNextUiState,
    onNavigateToDiscover: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is TvUpNextUiState.Loading -> {
                LoadingView(color = Color.White, modifier = Modifier.fillMaxSize())
            }

            is TvUpNextUiState.Empty -> {
                UpNextEmpty(
                    onNavigateToDiscover = onNavigateToDiscover,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is TvUpNextUiState.Loaded -> {
                UpNextList(
                    episodes = uiState.episodes,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun UpNextList(
    episodes: List<PodcastEpisode>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dateFormatter = remember(context) { RelativeDateFormatter(context) }
    val firstEpisodeFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        firstEpisodeFocusRequester.requestFocus()
    }
    var actionsEpisode by remember { mutableStateOf<PodcastEpisode?>(null) }

    Column(
        modifier = modifier
            .padding(start = 32.dp, top = 16.dp, end = 32.dp)
            .widthIn(max = ContentWidth),
    ) {
        UpNextHeader(episodes = episodes)
        Spacer(Modifier.height(24.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            modifier = Modifier.weight(1f),
        ) {
            itemsIndexed(
                items = episodes,
                key = { _, episode -> episode.uuid },
            ) { index, episode ->
                TvEpisodeListItem(
                    episode = episode,
                    dateFormatter = dateFormatter,
                    onClick = {},
                    onOpenActions = { actionsEpisode = episode },
                    episodeFocusRequester = firstEpisodeFocusRequester.takeIf { index == 0 },
                )
            }
        }
    }

    actionsEpisode?.let { episode ->
        TvEpisodeActionsModal(
            episode = episode,
            onDismissRequest = { actionsEpisode = null },
        )
    }
}

@Composable
private fun UpNextHeader(
    episodes: List<PodcastEpisode>,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(LR.string.up_next),
            style = TvTextStyles.ScreenTitle,
            color = Color.White,
        )
        Text(
            text = episodeSummaryText(episodes),
            style = TvTextStyles.PlaylistCardCaption,
            color = TvColors.TextSecondary,
        )
    }
}

@Composable
private fun episodeSummaryText(episodes: List<PodcastEpisode>): String {
    val context = LocalContext.current
    val countText = pluralStringResource(LR.plurals.episode_count, episodes.size, episodes.size)
    val remainingMs = episodes.sumOf { episode ->
        ((episode.duration - episode.playedUpTo).coerceAtLeast(0.0) * 1000).toLong()
    }
    val timeLeftText = stringResource(LR.string.time_left, TimeHelper.getTimeDurationShortString(remainingMs, context))
    return "$countText · $timeLeftText"
}

@Composable
private fun UpNextEmpty(
    onNavigateToDiscover: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(LR.string.tv_up_next_empty_title),
                style = TvTextStyles.ScreenTitle,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(LR.string.tv_up_next_empty_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = TvColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 400.dp),
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onNavigateToDiscover,
                colors = TvButtonDefaults.filledButtonColors(),
                modifier = Modifier.focusRequester(focusRequester),
            ) {
                Text(stringResource(LR.string.tv_up_next_empty_action_title))
            }
        }
    }
}

private val ContentWidth = 800.dp

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvUpNextLoadedPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            Box(modifier = Modifier.background(TvColors.Dark)) {
                TvUpNextContent(
                    uiState = TvUpNextUiState.Loaded(
                        episodes = List(4) { index ->
                            PodcastEpisode(
                                uuid = "episode-$index",
                                title = "Episode $index title that may span multiple lines to test the layout",
                                duration = 3600.0,
                                playedUpTo = 600.0,
                                publishedDate = Date(0),
                            )
                        },
                    ),
                    onNavigateToDiscover = {},
                )
            }
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvUpNextEmptyPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            Box(modifier = Modifier.background(TvColors.Dark)) {
                TvUpNextContent(
                    uiState = TvUpNextUiState.Empty,
                    onNavigateToDiscover = {},
                )
            }
        }
    }
}

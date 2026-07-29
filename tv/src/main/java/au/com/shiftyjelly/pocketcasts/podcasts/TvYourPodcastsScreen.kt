package au.com.shiftyjelly.pocketcasts.podcasts

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
import au.com.shiftyjelly.pocketcasts.component.TvPodcastTile
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.theme.TvTextStyles
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvYourPodcastsScreen(
    onNavigateToDiscover: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TvYourPodcastsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TvYourPodcastsContent(
        uiState = uiState,
        onNavigateToDiscover = onNavigateToDiscover,
        modifier = modifier,
    )
}

@Composable
private fun TvYourPodcastsContent(
    uiState: TvYourPodcastsUiState,
    onNavigateToDiscover: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = uiState,
        transitionSpec = { fadeIn(tween(durationMillis = 300)) togetherWith fadeOut(tween(durationMillis = 300)) },
        contentKey = { state ->
            when (state) {
                is TvYourPodcastsUiState.Loading -> "loading"
                is TvYourPodcastsUiState.Empty -> "empty"
                is TvYourPodcastsUiState.Loaded -> "content"
            }
        },
        label = "TvYourPodcastsContent",
        modifier = modifier,
    ) { state ->
        when (state) {
            is TvYourPodcastsUiState.Loading -> LoadingView(color = Color.White, modifier = Modifier.fillMaxSize())

            is TvYourPodcastsUiState.Empty -> TvYourPodcastsEmpty(
                onNavigateToDiscover = onNavigateToDiscover,
                modifier = Modifier.fillMaxSize(),
            )

            is TvYourPodcastsUiState.Loaded -> TvYourPodcastsGrid(
                podcasts = state.podcasts,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun TvYourPodcastsGrid(
    podcasts: List<Podcast>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 32.dp)) {
        Text(
            text = stringResource(LR.string.tv_tab_your_podcasts),
            style = TvTextStyles.ScreenTitle,
            color = Color.White,
            modifier = Modifier.padding(top = 8.dp, bottom = 26.dp),
        )
        var lastFocusedIndex by rememberSaveable(podcasts.size) { mutableIntStateOf(0) }
        val focusRequesters = remember(podcasts.size) { List(podcasts.size) { FocusRequester() } }

        LazyVerticalGrid(
            columns = GridCells.Fixed(GRID_COLUMNS),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            modifier = Modifier
                .focusGroup()
                .focusProperties {
                    onEnter = {
                        focusRequesters.getOrNull(lastFocusedIndex)?.requestFocus()
                    }
                },
        ) {
            itemsIndexed(
                items = podcasts,
                key = { _, podcast -> podcast.uuid },
            ) { index, podcast ->
                TvPodcastTile(
                    artworkUrl = PodcastImage.getMediumArtworkUrl(podcast.uuid),
                    podcastTitle = podcast.title,
                    onClick = {},
                    imageModifier = Modifier.fillMaxWidth(),
                    modifier = Modifier
                        .focusRequester(focusRequesters[index])
                        .onFocusChanged { focusState ->
                            if (focusState.hasFocus) {
                                lastFocusedIndex = index
                            }
                        },
                )
            }
        }
    }
}

@Composable
private fun TvYourPodcastsEmpty(
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
                text = stringResource(LR.string.tv_podcasts_empty_title),
                style = TvTextStyles.ScreenTitle,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(LR.string.tv_podcasts_empty_subtitle),
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
                Text(stringResource(LR.string.tv_podcasts_empty_action_title))
            }
        }
    }
}

private const val GRID_COLUMNS = 6

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvYourPodcastsGridPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            Box(modifier = Modifier.background(TvColors.Dark)) {
                TvYourPodcastsContent(
                    uiState = TvYourPodcastsUiState.Loaded(
                        podcasts = List(12) { index -> Podcast(uuid = "podcast-$index", title = "Podcast $index") },
                    ),
                    onNavigateToDiscover = {},
                )
            }
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvYourPodcastsEmptyPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            Box(modifier = Modifier.background(TvColors.Dark)) {
                TvYourPodcastsContent(
                    uiState = TvYourPodcastsUiState.Empty,
                    onNavigateToDiscover = {},
                )
            }
        }
    }
}

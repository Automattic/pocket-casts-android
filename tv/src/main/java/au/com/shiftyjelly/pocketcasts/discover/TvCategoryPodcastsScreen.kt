package au.com.shiftyjelly.pocketcasts.discover

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.MaterialTheme
import au.com.shiftyjelly.pocketcasts.component.TvEmptyState
import au.com.shiftyjelly.pocketcasts.component.TvPodcastGridScaffold
import au.com.shiftyjelly.pocketcasts.component.TvPodcastTile
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import kotlinx.coroutines.CancellationException
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvCategoryPodcastsScreen(
    categoryName: String,
    categorySource: String,
    getCategoryPodcasts: suspend (String) -> TvCategoryPodcasts,
    onOpenPodcast: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onPodcastClick: (String?, TvDiscoverPodcast) -> Unit = { _, _ -> },
    restoreFocusTrigger: Int = 0,
) {
    var reloadTrigger by remember(categorySource) { mutableIntStateOf(0) }
    var uiState by remember(categorySource) { mutableStateOf<TvCategoryPodcastsUiState>(TvCategoryPodcastsUiState.Loading) }
    val currentGetCategoryPodcasts by rememberUpdatedState(getCategoryPodcasts)
    LaunchedEffect(categorySource, reloadTrigger) {
        uiState = TvCategoryPodcastsUiState.Loading
        uiState = runCatching { currentGetCategoryPodcasts(categorySource) }
            .fold(
                onSuccess = { result ->
                    if (result.podcasts.isEmpty()) {
                        TvCategoryPodcastsUiState.Empty
                    } else {
                        TvCategoryPodcastsUiState.Loaded(result.listId, result.podcasts)
                    }
                },
                onFailure = { exception ->
                    if (exception is CancellationException) throw exception
                    TvCategoryPodcastsUiState.Error
                },
            )
    }

    TvCategoryPodcastsContent(
        categoryName = categoryName,
        uiState = uiState,
        onOpenPodcast = onOpenPodcast,
        onClose = onClose,
        onRetry = { reloadTrigger++ },
        modifier = modifier,
        onPodcastClick = onPodcastClick,
        restoreFocusTrigger = restoreFocusTrigger,
    )
}

@Composable
private fun TvCategoryPodcastsContent(
    categoryName: String,
    uiState: TvCategoryPodcastsUiState,
    onOpenPodcast: (String) -> Unit,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onPodcastClick: (String?, TvDiscoverPodcast) -> Unit = { _, _ -> },
    restoreFocusTrigger: Int = 0,
) {
    AnimatedContent(
        targetState = uiState,
        transitionSpec = { fadeIn(tween(durationMillis = 300)) togetherWith fadeOut(tween(durationMillis = 300)) },
        contentKey = { state ->
            when (state) {
                is TvCategoryPodcastsUiState.Loading -> "loading"
                is TvCategoryPodcastsUiState.Empty -> "empty"
                is TvCategoryPodcastsUiState.Error -> "error"
                is TvCategoryPodcastsUiState.Loaded -> "content"
            }
        },
        label = "TvCategoryPodcastsContent",
        modifier = modifier,
    ) { state ->
        when (state) {
            is TvCategoryPodcastsUiState.Loading -> LoadingView(
                color = MaterialTheme.tvColors.textPrimary,
                modifier = Modifier.fillMaxSize(),
            )

            is TvCategoryPodcastsUiState.Empty -> TvEmptyState(
                title = stringResource(LR.string.tv_search_no_results_title),
                subtitle = stringResource(LR.string.tv_search_no_results_subtitle),
                actionLabel = stringResource(LR.string.ok),
                onAction = onClose,
                autoFocusAction = true,
                modifier = Modifier.fillMaxSize(),
            )

            is TvCategoryPodcastsUiState.Error -> TvEmptyState(
                title = stringResource(LR.string.error_generic_message),
                subtitle = stringResource(LR.string.tv_search_no_results_subtitle),
                actionLabel = stringResource(LR.string.retry),
                onAction = onRetry,
                autoFocusAction = true,
                modifier = Modifier.fillMaxSize(),
            )

            is TvCategoryPodcastsUiState.Loaded -> TvPodcastGridScaffold(
                title = stringResource(LR.string.discover_most_popular_in, categoryName),
                itemKeys = state.podcasts.map(TvDiscoverPodcast::uuid),
                autoFocusFirstItem = true,
                restoreFocusTrigger = restoreFocusTrigger,
                modifier = Modifier.fillMaxSize(),
            ) { index, itemModifier ->
                val podcast = state.podcasts[index]
                TvPodcastTile(
                    artworkUrl = podcast.artworkUrl,
                    podcastTitle = podcast.title,
                    onClick = {
                        onPodcastClick(state.listId, podcast)
                        onOpenPodcast(podcast.uuid)
                    },
                    imageModifier = Modifier.fillMaxWidth(),
                    modifier = itemModifier,
                    isSponsored = podcast.isSponsored,
                )
            }
        }
    }
}

private sealed interface TvCategoryPodcastsUiState {
    data object Loading : TvCategoryPodcastsUiState

    data object Empty : TvCategoryPodcastsUiState

    data object Error : TvCategoryPodcastsUiState

    data class Loaded(
        val listId: String?,
        val podcasts: List<TvDiscoverPodcast>,
    ) : TvCategoryPodcastsUiState
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvCategoryPodcastsPreview() {
    TvTheme {
        Box(modifier = Modifier.background(MaterialTheme.tvColors.backgroundSunken)) {
            TvCategoryPodcastsContent(
                categoryName = "True Crime",
                uiState = TvCategoryPodcastsUiState.Loaded(
                    listId = "list-id",
                    podcasts = (1..6).map { index ->
                        TvDiscoverPodcast(
                            uuid = "podcast-$index",
                            title = "Podcast $index",
                            author = "Author $index",
                            description = "Description $index",
                        )
                    },
                ),
                onOpenPodcast = {},
                onClose = {},
                onRetry = {},
            )
        }
    }
}

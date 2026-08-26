package au.com.shiftyjelly.pocketcasts.podcasts

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
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvFolderDetailScreen(
    folderUuid: String,
    folderName: String,
    getFolderPodcasts: suspend (String) -> List<Podcast>,
    onOpenPodcast: (String) -> Unit,
    onClose: () -> Unit,
    onFolderImpression: (podcastCount: Int) -> Unit,
    modifier: Modifier = Modifier,
    restoreFocusTrigger: Int = 0,
) {
    var uiState by remember(folderUuid) { mutableStateOf<TvFolderDetailUiState>(TvFolderDetailUiState.Loading) }
    val currentGetFolderPodcasts by rememberUpdatedState(getFolderPodcasts)
    val currentOnFolderImpression by rememberUpdatedState(onFolderImpression)
    LaunchedEffect(folderUuid) {
        val podcasts = currentGetFolderPodcasts(folderUuid)
        uiState = if (podcasts.isEmpty()) TvFolderDetailUiState.Empty else TvFolderDetailUiState.Loaded(podcasts)
        currentOnFolderImpression(podcasts.size)
    }

    TvFolderDetailContent(
        folderName = folderName,
        uiState = uiState,
        onOpenPodcast = onOpenPodcast,
        onClose = onClose,
        modifier = modifier,
        restoreFocusTrigger = restoreFocusTrigger,
    )
}

@Composable
private fun TvFolderDetailContent(
    folderName: String,
    uiState: TvFolderDetailUiState,
    onOpenPodcast: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    restoreFocusTrigger: Int = 0,
) {
    AnimatedContent(
        targetState = uiState,
        transitionSpec = { fadeIn(tween(durationMillis = 300)) togetherWith fadeOut(tween(durationMillis = 300)) },
        contentKey = { state ->
            when (state) {
                is TvFolderDetailUiState.Loading -> "loading"
                is TvFolderDetailUiState.Empty -> "empty"
                is TvFolderDetailUiState.Loaded -> "content"
            }
        },
        label = "TvFolderDetailContent",
        modifier = modifier,
    ) { state ->
        when (state) {
            is TvFolderDetailUiState.Loading -> LoadingView(
                color = MaterialTheme.tvColors.textPrimary,
                modifier = Modifier.fillMaxSize(),
            )

            is TvFolderDetailUiState.Empty -> TvEmptyState(
                title = stringResource(LR.string.podcasts_empty_folder),
                subtitle = stringResource(LR.string.tv_folder_empty_message),
                actionLabel = stringResource(LR.string.ok),
                onAction = onClose,
                autoFocusAction = true,
                modifier = Modifier.fillMaxSize(),
            )

            is TvFolderDetailUiState.Loaded -> TvPodcastGridScaffold(
                title = folderName,
                itemKeys = state.podcasts.map(Podcast::uuid),
                autoFocusFirstItem = true,
                restoreFocusTrigger = restoreFocusTrigger,
                modifier = Modifier.fillMaxSize(),
            ) { index, itemModifier ->
                val podcast = state.podcasts[index]
                TvPodcastTile(
                    artworkUrl = PodcastImage.getMediumArtworkUrl(podcast.uuid),
                    podcastTitle = podcast.title,
                    onClick = { onOpenPodcast(podcast.uuid) },
                    imageModifier = Modifier.fillMaxWidth(),
                    modifier = itemModifier,
                )
            }
        }
    }
}

private sealed interface TvFolderDetailUiState {
    data object Loading : TvFolderDetailUiState

    data object Empty : TvFolderDetailUiState

    data class Loaded(
        val podcasts: List<Podcast>,
    ) : TvFolderDetailUiState
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvFolderDetailPreview() {
    TvTheme {
        Box(modifier = Modifier.background(MaterialTheme.tvColors.backgroundSunken)) {
            TvFolderDetailContent(
                folderName = "Tech & Science",
                uiState = TvFolderDetailUiState.Loaded(
                    podcasts = List(6) { index -> Podcast(uuid = "podcast-$index", title = "Podcast $index") },
                ),
                onOpenPodcast = {},
                onClose = {},
            )
        }
    }
}

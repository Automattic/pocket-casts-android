package au.com.shiftyjelly.pocketcasts.podcasts

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
fun TvFolderDetailScreen(
    folderUuid: String,
    folderName: String,
    getFolderPodcasts: suspend (String) -> List<Podcast>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var uiState by remember(folderUuid) { mutableStateOf<TvFolderDetailUiState>(TvFolderDetailUiState.Loading) }
    LaunchedEffect(folderUuid, getFolderPodcasts) {
        val podcasts = getFolderPodcasts(folderUuid)
        uiState = if (podcasts.isEmpty()) TvFolderDetailUiState.Empty else TvFolderDetailUiState.Loaded(podcasts)
    }

    TvFolderDetailContent(
        folderName = folderName,
        uiState = uiState,
        onClose = onClose,
        modifier = modifier,
    )
}

@Composable
private fun TvFolderDetailContent(
    folderName: String,
    uiState: TvFolderDetailUiState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
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
            is TvFolderDetailUiState.Loading -> LoadingView(color = Color.White, modifier = Modifier.fillMaxSize())

            is TvFolderDetailUiState.Empty -> TvFolderDetailEmpty(
                onClose = onClose,
                modifier = Modifier.fillMaxSize(),
            )

            is TvFolderDetailUiState.Loaded -> TvPodcastGridScaffold(
                title = folderName,
                itemKeys = state.podcasts.map(Podcast::uuid),
                autoFocusFirstItem = true,
                modifier = Modifier.fillMaxSize(),
            ) { index, itemModifier ->
                val podcast = state.podcasts[index]
                TvPodcastTile(
                    artworkUrl = PodcastImage.getMediumArtworkUrl(podcast.uuid),
                    podcastTitle = podcast.title,
                    onClick = {},
                    imageModifier = Modifier.fillMaxWidth(),
                    modifier = itemModifier,
                )
            }
        }
    }
}

@Composable
private fun TvFolderDetailEmpty(
    onClose: () -> Unit,
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
                text = stringResource(LR.string.podcasts_empty_folder),
                style = TvTextStyles.ScreenTitle,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(LR.string.tv_folder_empty_message),
                style = MaterialTheme.typography.bodyLarge,
                color = TvColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 400.dp),
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onClose,
                colors = TvButtonDefaults.filledButtonColors(),
                modifier = Modifier.focusRequester(focusRequester),
            ) {
                Text(stringResource(LR.string.ok))
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
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            Box(modifier = Modifier.background(TvColors.Dark)) {
                TvFolderDetailContent(
                    folderName = "Tech & Science",
                    uiState = TvFolderDetailUiState.Loaded(
                        podcasts = List(6) { index -> Podcast(uuid = "podcast-$index", title = "Podcast $index") },
                    ),
                    onClose = {},
                )
            }
        }
    }
}

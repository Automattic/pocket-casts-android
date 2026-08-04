package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.TvTextStyles
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvEpisodeActionsModal(
    episode: PodcastEpisode,
    onDismissRequest: () -> Unit,
    onShowEpisodeDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvModal(
        onDismissRequest = onDismissRequest,
        contentPadding = ContentPadding,
        modifier = modifier,
    ) {
        TvEpisodeActionsModalContent(
            episode = episode,
            onDismissRequest = onDismissRequest,
            onShowEpisodeDetails = onShowEpisodeDetails,
        )
    }
}

@Composable
private fun ColumnScope.TvEpisodeActionsModalContent(
    episode: PodcastEpisode,
    onDismissRequest: () -> Unit,
    onShowEpisodeDetails: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val toastHostState = LocalTvToastHostState.current
    val episodeDetails = stringResource(LR.string.tv_episode_details)
    val goToPodcast = stringResource(LR.string.go_to_podcast)
    val playNext = stringResource(LR.string.add_to_up_next_top)
    val playLast = stringResource(LR.string.add_to_up_next_bottom)
    val playedToggle = stringResource(if (episode.isFinished) LR.string.mark_as_unplayed else LR.string.mark_as_played)
    val archiveToggle = stringResource(if (episode.isArchived) LR.string.unarchive else LR.string.archive)

    val playNextToast = stringResource(LR.string.tv_episode_will_play_next)
    val playLastToast = stringResource(LR.string.tv_episode_will_play_last)
    val playedToast = stringResource(
        if (episode.isFinished) LR.string.tv_episode_marked_as_unplayed else LR.string.tv_episode_marked_as_played,
    )
    val archiveToast = stringResource(
        if (episode.isArchived) LR.string.tv_episode_unarchived else LR.string.tv_episode_archived,
    )

    fun withToast(message: String): () -> Unit = {
        toastHostState.show(message)
        onDismissRequest()
    }

    val actions = listOf(
        episodeDetails to onShowEpisodeDetails,
        goToPodcast to onDismissRequest,
        playNext to withToast(playNextToast),
        playLast to withToast(playLastToast),
        playedToggle to withToast(playedToast),
        archiveToggle to withToast(archiveToast),
    )

    actions.forEachIndexed { index, (label, onClick) ->
        TvEpisodeActionButton(
            text = label,
            onClick = onClick,
            modifier = if (index == 0) Modifier.focusRequester(focusRequester) else Modifier,
        )
    }

    TvEpisodeActionButton(
        text = stringResource(LR.string.cancel),
        onClick = onDismissRequest,
    )
}

@Composable
private fun TvEpisodeActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        colors = TvButtonDefaults.filledButtonColors(),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = TvTextStyles.ModalButtonLabel,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview
@Composable
private fun TvEpisodeActionsModalPreview() {
    TvEpisodeActionsModalPreviewContent(
        episode = PodcastEpisode(
            uuid = "episode-uuid",
            title = "Episode title that might be quite long and wrap onto two lines",
            podcastUuid = "podcast-uuid",
            publishedDate = Date(0),
        ),
    )
}

@Preview
@Composable
private fun TvEpisodeActionsModalPlayedArchivedPreview() {
    TvEpisodeActionsModalPreviewContent(
        episode = PodcastEpisode(
            uuid = "episode-uuid",
            title = "Episode title that might be quite long and wrap onto two lines",
            podcastUuid = "podcast-uuid",
            publishedDate = Date(0),
            playingStatus = EpisodePlayingStatus.COMPLETED,
            isArchived = true,
        ),
    )
}

@Composable
private fun TvEpisodeActionsModalPreviewContent(episode: PodcastEpisode) {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            CompositionLocalProvider(LocalTvToastHostState provides remember { TvToastHostState() }) {
                TvModalSurface(contentPadding = ContentPadding) {
                    TvEpisodeActionsModalContent(
                        episode = episode,
                        onDismissRequest = {},
                        onShowEpisodeDetails = {},
                    )
                }
            }
        }
    }
}

private val ContentPadding = PaddingValues(horizontal = 24.dp, vertical = 27.dp)

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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
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
    context: TvEpisodeActionContext,
    onDismissRequest: () -> Unit,
    onShowEpisodeDetails: () -> Unit,
    modifier: Modifier = Modifier,
    onGoToPodcast: (() -> Unit)? = null,
    viewModel: TvEpisodeActionsViewModel = hiltViewModel(),
) {
    TvModal(
        onDismissRequest = onDismissRequest,
        contentPadding = ContentPadding,
        modifier = modifier,
    ) {
        TvEpisodeActionsModalContent(
            episode = episode,
            context = context,
            actions = viewModel,
            onDismissRequest = onDismissRequest,
            onShowEpisodeDetails = onShowEpisodeDetails,
            onGoToPodcast = onGoToPodcast,
        )
    }
}

@Composable
private fun ColumnScope.TvEpisodeActionsModalContent(
    episode: PodcastEpisode,
    context: TvEpisodeActionContext,
    actions: TvEpisodeActions,
    onDismissRequest: () -> Unit,
    onShowEpisodeDetails: () -> Unit,
    onGoToPodcast: (() -> Unit)?,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val toastHostState = LocalTvToastHostState.current
    val source = context.source

    val episodeDetails = stringResource(LR.string.tv_episode_details)
    val goToPodcast = stringResource(LR.string.go_to_podcast)
    val playNext = stringResource(LR.string.add_to_up_next_top)
    val playLast = stringResource(LR.string.add_to_up_next_bottom)
    val removeFromUpNext = stringResource(LR.string.remove_from_up_next)
    val playedToggle = stringResource(if (episode.isFinished) LR.string.mark_as_unplayed else LR.string.mark_as_played)
    val archiveToggle = stringResource(if (episode.isArchived) LR.string.unarchive else LR.string.archive)

    val playNextToast = stringResource(LR.string.tv_episode_will_play_next)
    val playLastToast = stringResource(LR.string.tv_episode_will_play_last)
    val removedToast = stringResource(LR.string.tv_episode_removed_from_up_next)
    val playedToast = stringResource(
        if (episode.isFinished) LR.string.tv_episode_marked_as_unplayed else LR.string.tv_episode_marked_as_played,
    )
    val archiveToast = stringResource(
        if (episode.isArchived) LR.string.tv_episode_unarchived else LR.string.tv_episode_archived,
    )

    fun perform(message: String, action: () -> Unit): () -> Unit = {
        action()
        toastHostState.show(message)
        onDismissRequest()
    }

    val buttons = buildList {
        add(episodeDetails to onShowEpisodeDetails)
        if (context != TvEpisodeActionContext.PodcastDetails && onGoToPodcast != null) {
            add(goToPodcast to { onGoToPodcast(); onDismissRequest() })
        }
        add(playNext to perform(playNextToast) { actions.playNext(episode, source) })
        add(playLast to perform(playLastToast) { actions.playLast(episode, source) })
        if (context == TvEpisodeActionContext.UpNext) {
            add(removeFromUpNext to perform(removedToast) { actions.removeFromUpNext(episode, source) })
        } else {
            add(
                playedToggle to perform(playedToast) {
                    if (episode.isFinished) actions.markAsUnplayed(episode, source) else actions.markAsPlayed(episode, source)
                },
            )
            add(
                archiveToggle to perform(archiveToast) {
                    if (episode.isArchived) actions.unarchive(episode, source) else actions.archive(episode, source)
                },
            )
        }
    }

    buttons.forEachIndexed { index, (label, onClick) ->
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
        context = TvEpisodeActionContext.PodcastDetails,
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
        context = TvEpisodeActionContext.Playlist,
    )
}

@Preview
@Composable
private fun TvEpisodeActionsModalUpNextPreview() {
    TvEpisodeActionsModalPreviewContent(
        episode = PodcastEpisode(
            uuid = "episode-uuid",
            title = "Episode title that might be quite long and wrap onto two lines",
            podcastUuid = "podcast-uuid",
            publishedDate = Date(0),
        ),
        context = TvEpisodeActionContext.UpNext,
    )
}

@Composable
private fun TvEpisodeActionsModalPreviewContent(
    episode: PodcastEpisode,
    context: TvEpisodeActionContext,
) {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            CompositionLocalProvider(LocalTvToastHostState provides remember { TvToastHostState() }) {
                TvModalSurface(contentPadding = ContentPadding) {
                    TvEpisodeActionsModalContent(
                        episode = episode,
                        context = context,
                        actions = NoOpTvEpisodeActions,
                        onDismissRequest = {},
                        onShowEpisodeDetails = {},
                        onGoToPodcast = {},
                    )
                }
            }
        }
    }
}

private object NoOpTvEpisodeActions : TvEpisodeActions {
    override fun playNext(episode: PodcastEpisode, source: SourceView) = Unit
    override fun playLast(episode: PodcastEpisode, source: SourceView) = Unit
    override fun markAsPlayed(episode: PodcastEpisode, source: SourceView) = Unit
    override fun markAsUnplayed(episode: PodcastEpisode, source: SourceView) = Unit
    override fun archive(episode: PodcastEpisode, source: SourceView) = Unit
    override fun unarchive(episode: PodcastEpisode, source: SourceView) = Unit
    override fun removeFromUpNext(episode: PodcastEpisode, source: SourceView) = Unit
}

private val ContentPadding = PaddingValues(horizontal = 24.dp, vertical = 27.dp)

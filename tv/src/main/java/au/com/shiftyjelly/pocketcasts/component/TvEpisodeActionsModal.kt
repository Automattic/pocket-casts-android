package au.com.shiftyjelly.pocketcasts.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import java.util.Date
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvEpisodeActionsModal(
    episode: PodcastEpisode,
    actionContext: TvEpisodeActionContext,
    onDismissRequest: () -> Unit,
    onShowEpisodeDetails: () -> Unit,
    modifier: Modifier = Modifier,
    onGoToPodcast: (() -> Unit)? = null,
    actions: TvEpisodeActions = hiltViewModel<TvEpisodeActionsViewModel>(),
) {
    TvModal(
        onDismissRequest = onDismissRequest,
        contentPadding = ContentPadding,
        modifier = modifier,
    ) {
        TvEpisodeActionsModalContent(
            episode = episode,
            actionContext = actionContext,
            actions = actions,
            onDismissRequest = onDismissRequest,
            onShowEpisodeDetails = onShowEpisodeDetails,
            onGoToPodcast = onGoToPodcast,
        )
    }
}

@Composable
private fun ColumnScope.TvEpisodeActionsModalContent(
    episode: PodcastEpisode,
    actionContext: TvEpisodeActionContext,
    actions: TvEpisodeActions,
    onDismissRequest: () -> Unit,
    onShowEpisodeDetails: () -> Unit,
    onGoToPodcast: (() -> Unit)?,
) {
    var pendingConfirmation by remember { mutableStateOf<TvEpisodeActionConfirmation?>(null) }
    var returnFocusLabel by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(pendingConfirmation) {
        if (pendingConfirmation == null) {
            focusRequester.requestFocus()
        }
    }

    val toastHostState = LocalTvToastHostState.current
    val openNowPlaying = LocalOpenNowPlaying.current
    val source = actionContext.source

    val play = stringResource(LR.string.play)
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

    val markPlayedTitle = stringResource(LR.string.tv_mark_played_confirmation_title)
    val archiveTitle = stringResource(LR.string.tv_archive_confirmation_title)
    val stopPlaybackMessage = stringResource(LR.string.tv_stop_playback_confirmation_message)

    fun confirm(title: String, confirmLabel: String, onConfirm: () -> Unit): () -> Unit = {
        pendingConfirmation = TvEpisodeActionConfirmation(title, confirmLabel, onConfirm)
    }

    val confirmation = pendingConfirmation
    if (confirmation != null) {
        fun cancelConfirmation() {
            returnFocusLabel = confirmation.confirmLabel
            pendingConfirmation = null
        }
        BackHandler(onBack = ::cancelConfirmation)
        TvConfirmationContent(
            title = confirmation.title,
            message = stopPlaybackMessage,
            confirmLabel = confirmation.confirmLabel,
            onConfirm = {
                pendingConfirmation = null
                confirmation.onConfirm()
            },
            onCancel = ::cancelConfirmation,
        )
    } else {
        val markAsPlayed = perform(playedToast) { actions.markAsPlayed(episode) }
        val archiveEpisode = perform(archiveToast) { actions.archive(episode) }

        val buttons = tvEpisodeActionTypes(actionContext, showGoToPodcast = onGoToPodcast != null).map { button ->
            when (button) {
                TvEpisodeActionType.Play -> play to {
                    actions.play(episode, source)
                    onDismissRequest()
                    openNowPlaying()
                }

                TvEpisodeActionType.Details -> episodeDetails to onShowEpisodeDetails

                TvEpisodeActionType.GoToPodcast -> goToPodcast to {
                    onGoToPodcast?.invoke()
                    onDismissRequest()
                }

                TvEpisodeActionType.PlayNext -> playNext to perform(playNextToast) { actions.playNext(episode, source) }

                TvEpisodeActionType.PlayLast -> playLast to perform(playLastToast) { actions.playLast(episode, source) }

                TvEpisodeActionType.RemoveFromUpNext -> removeFromUpNext to perform(removedToast) {
                    actions.removeFromUpNext(episode, source)
                }

                TvEpisodeActionType.TogglePlayed -> playedToggle to when {
                    episode.isFinished -> perform(playedToast) { actions.markAsUnplayed(episode) }

                    tvEpisodeActionRequiresConfirmation(actionContext, button, episode) ->
                        confirm(markPlayedTitle, playedToggle, markAsPlayed)

                    else -> markAsPlayed
                }

                TvEpisodeActionType.ToggleArchived -> archiveToggle to when {
                    episode.isArchived -> perform(archiveToast) { actions.unarchive(episode) }

                    tvEpisodeActionRequiresConfirmation(actionContext, button, episode) ->
                        confirm(archiveTitle, archiveToggle, archiveEpisode)

                    else -> archiveEpisode
                }
            }
        }

        val focusLabel = returnFocusLabel?.takeIf { wanted -> buttons.any { it.first == wanted } }
        buttons.forEachIndexed { index, (label, onClick) ->
            val isInitialFocus = if (focusLabel != null) label == focusLabel else index == 0
            TvModalButton(
                text = label,
                onClick = onClick,
                modifier = if (isInitialFocus) Modifier.focusRequester(focusRequester) else Modifier,
            )
        }

        TvModalButton(
            text = stringResource(LR.string.cancel),
            onClick = onDismissRequest,
        )
    }
}

private class TvEpisodeActionConfirmation(
    val title: String,
    val confirmLabel: String,
    val onConfirm: () -> Unit,
)

internal fun tvEpisodeActionRequiresConfirmation(
    actionContext: TvEpisodeActionContext,
    type: TvEpisodeActionType,
    episode: PodcastEpisode,
): Boolean = actionContext == TvEpisodeActionContext.NowPlaying && when (type) {
    TvEpisodeActionType.TogglePlayed -> !episode.isFinished
    TvEpisodeActionType.ToggleArchived -> !episode.isArchived
    else -> false
}

internal enum class TvEpisodeActionType {
    Play,
    Details,
    GoToPodcast,
    PlayNext,
    PlayLast,
    RemoveFromUpNext,
    TogglePlayed,
    ToggleArchived,
}

internal fun tvEpisodeActionTypes(
    actionContext: TvEpisodeActionContext,
    showGoToPodcast: Boolean,
): List<TvEpisodeActionType> = buildList {
    if (actionContext != TvEpisodeActionContext.NowPlaying) {
        add(TvEpisodeActionType.Play)
    }
    add(TvEpisodeActionType.Details)
    if (showGoToPodcast) {
        add(TvEpisodeActionType.GoToPodcast)
    }
    if (actionContext != TvEpisodeActionContext.NowPlaying) {
        add(TvEpisodeActionType.PlayNext)
        add(TvEpisodeActionType.PlayLast)
    }
    if (actionContext == TvEpisodeActionContext.UpNext) {
        add(TvEpisodeActionType.RemoveFromUpNext)
    } else {
        add(TvEpisodeActionType.TogglePlayed)
        add(TvEpisodeActionType.ToggleArchived)
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
        actionContext = TvEpisodeActionContext.PodcastDetails,
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
        actionContext = TvEpisodeActionContext.Playlist,
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
        actionContext = TvEpisodeActionContext.UpNext,
    )
}

@Composable
private fun TvEpisodeActionsModalPreviewContent(
    episode: PodcastEpisode,
    actionContext: TvEpisodeActionContext,
) {
    TvTheme {
        CompositionLocalProvider(
            LocalTvToastHostState provides remember { TvToastHostState() },
            LocalOpenNowPlaying provides {},
        ) {
            TvModalSurface(contentPadding = ContentPadding) {
                TvEpisodeActionsModalContent(
                    episode = episode,
                    actionContext = actionContext,
                    actions = NoOpTvEpisodeActions,
                    onDismissRequest = {},
                    onShowEpisodeDetails = {},
                    onGoToPodcast = {},
                )
            }
        }
    }
}

private object NoOpTvEpisodeActions : TvEpisodeActions {
    override fun play(episode: PodcastEpisode, source: SourceView) = Unit
    override fun playNext(episode: PodcastEpisode, source: SourceView) = Unit
    override fun playLast(episode: PodcastEpisode, source: SourceView) = Unit
    override fun markAsPlayed(episode: PodcastEpisode) = Unit
    override fun markAsUnplayed(episode: PodcastEpisode) = Unit
    override fun archive(episode: PodcastEpisode) = Unit
    override fun unarchive(episode: PodcastEpisode) = Unit
    override fun removeFromUpNext(episode: PodcastEpisode, source: SourceView) = Unit
}

private val ContentPadding = PaddingValues(horizontal = 24.dp, vertical = 27.dp)

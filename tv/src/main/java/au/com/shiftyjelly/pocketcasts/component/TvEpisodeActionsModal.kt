package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.TvTextStyles
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvEpisodeActionsModal(
    episode: PodcastEpisode,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvModal(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        TvEpisodeActionsModalContent(episode = episode)
    }
}

@Composable
private fun ColumnScope.TvEpisodeActionsModalContent(episode: PodcastEpisode) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Text(
        text = episode.title,
        color = Color.White,
        style = TvTextStyles.ModalTitle,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(bottom = 8.dp),
    )

    val episodeDetails = stringResource(LR.string.tv_episode_details)
    val goToPodcast = stringResource(LR.string.go_to_podcast)
    val playNext = stringResource(LR.string.add_to_up_next_top)
    val playLast = stringResource(LR.string.add_to_up_next_bottom)
    val playedToggle = stringResource(if (episode.isFinished) LR.string.mark_as_unplayed else LR.string.mark_as_played)
    val archiveToggle = stringResource(if (episode.isArchived) LR.string.unarchive else LR.string.archive)

    val actions = buildList {
        if (episode.podcastUuid.isNotBlank()) {
            add(episodeDetails)
        }
        add(goToPodcast)
        add(playNext)
        add(playLast)
        add(playedToggle)
        add(archiveToggle)
    }

    actions.forEachIndexed { index, label ->
        TvEpisodeActionButton(
            text = label,
            onClick = {},
            modifier = if (index == 0) Modifier.focusRequester(focusRequester) else Modifier,
        )
    }
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
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview
@Composable
private fun TvEpisodeActionsModalPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            TvModalSurface {
                TvEpisodeActionsModalContent(
                    episode = PodcastEpisode(
                        uuid = "episode-uuid",
                        title = "Episode title that might be quite long and wrap onto two lines",
                        podcastUuid = "podcast-uuid",
                        publishedDate = Date(0),
                    ),
                )
            }
        }
    }
}

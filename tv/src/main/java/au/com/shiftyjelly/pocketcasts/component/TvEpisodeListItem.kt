package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode

@Composable
fun TvEpisodeListItem(
    episode: PodcastEpisode,
    dateFormatter: RelativeDateFormatter,
    onClick: () -> Unit,
    onOpenActions: () -> Unit,
    modifier: Modifier = Modifier,
    episodeFocusRequester: FocusRequester? = null,
    leftFocusRequester: FocusRequester? = null,
) {
    TvEpisodeListItemContainer(
        onOpenActions = onOpenActions,
        modifier = modifier,
    ) { rowModifier ->
        TvEpisodeRow(
            episode = episode,
            onClick = onClick,
            dateFormatter = dateFormatter,
            modifier = rowModifier
                .then(if (leftFocusRequester != null) Modifier.focusProperties { left = leftFocusRequester } else Modifier)
                .then(if (episodeFocusRequester != null) Modifier.focusRequester(episodeFocusRequester) else Modifier),
        )
    }
}

@Composable
fun TvEpisodeListItemContainer(
    onOpenActions: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    var isItemFocused by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isItemFocused = it.hasFocus },
    ) {
        content(Modifier.weight(1f))
        AnimatedVisibility(
            visible = isItemFocused,
            enter = expandHorizontally(tween(MORE_BUTTON_ANIMATION_DURATION_MS), expandFrom = Alignment.Start) +
                slideInHorizontally(tween(MORE_BUTTON_ANIMATION_DURATION_MS), initialOffsetX = { it }) +
                fadeIn(tween(MORE_BUTTON_ANIMATION_DURATION_MS)),
            exit = shrinkHorizontally(tween(MORE_BUTTON_ANIMATION_DURATION_MS), shrinkTowards = Alignment.Start) +
                slideOutHorizontally(tween(MORE_BUTTON_ANIMATION_DURATION_MS), targetOffsetX = { it }) +
                fadeOut(tween(MORE_BUTTON_ANIMATION_DURATION_MS)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(12.dp))
                TvMoreButton(onClick = onOpenActions)
            }
        }
    }
}

private const val MORE_BUTTON_ANIMATION_DURATION_MS = 200

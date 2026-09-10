package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
        MoreButtonSlot(
            visible = isItemFocused,
            onClick = onOpenActions,
        )
    }
}

@Composable
private fun MoreButtonSlot(
    visible: Boolean,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(start = 12.dp)
            .size(TvMoreButtonSize),
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(MORE_BUTTON_ANIMATION_DURATION_MS)),
            exit = fadeOut(tween(MORE_BUTTON_ANIMATION_DURATION_MS)),
        ) {
            TvMoreButton(onClick = onClick)
        }
    }
}

private const val MORE_BUTTON_ANIMATION_DURATION_MS = 200

package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
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
    var isItemFocused by remember { mutableStateOf(false) }
    val moreButtonFocusRequester = remember { FocusRequester() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isItemFocused = it.hasFocus },
    ) {
        TvEpisodeRow(
            episode = episode,
            onClick = onClick,
            dateFormatter = dateFormatter,
            contentEndPadding = MoreButtonReservedWidth,
            modifier = Modifier
                .fillMaxWidth()
                .focusProperties {
                    if (isItemFocused) {
                        right = moreButtonFocusRequester
                    }
                    if (leftFocusRequester != null) {
                        left = leftFocusRequester
                    }
                }
                .then(if (episodeFocusRequester != null) Modifier.focusRequester(episodeFocusRequester) else Modifier),
        )
        MoreButtonSlot(
            visible = isItemFocused,
            onClick = onOpenActions,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = MoreButtonEndInset)
                .then(if (episodeFocusRequester != null) Modifier.focusProperties { left = episodeFocusRequester } else Modifier)
                .focusRequester(moreButtonFocusRequester),
        )
    }
}

@Composable
private fun MoreButtonSlot(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(TvMoreButtonSize),
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
private val MoreButtonEndInset = 16.dp
private val MoreButtonReservedWidth = TvMoreButtonSize + 16.dp

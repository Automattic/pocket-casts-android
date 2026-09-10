package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.MaterialTheme
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.tvColors

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
    val rowInteractionSource = remember { MutableInteractionSource() }
    val isRowFocused by rowInteractionSource.collectIsFocusedAsState()
    var isMoreButtonFocused by remember { mutableStateOf(false) }
    val moreButtonFocusRequester = remember { FocusRequester() }
    val showMoreButton = isRowFocused || isMoreButtonFocused
    Box(modifier = modifier.fillMaxWidth()) {
        TvEpisodeRow(
            episode = episode,
            onClick = onClick,
            dateFormatter = dateFormatter,
            contentEndPadding = MoreButtonReservedWidth,
            interactionSource = rowInteractionSource,
            modifier = Modifier
                .fillMaxWidth()
                .focusProperties {
                    if (showMoreButton) {
                        right = moreButtonFocusRequester
                    }
                    if (leftFocusRequester != null) {
                        left = leftFocusRequester
                    }
                }
                .then(if (episodeFocusRequester != null) Modifier.focusRequester(episodeFocusRequester) else Modifier),
        )
        MoreButtonSlot(
            visible = showMoreButton,
            onClick = onOpenActions,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .zIndex(1f)
                .padding(end = MoreButtonEndInset)
                .onFocusChanged { isMoreButtonFocused = it.hasFocus }
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
            TvMoreButton(
                onClick = onClick,
                colors = TvButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.tvColors.textPrimaryActive,
                ),
            )
        }
    }
}

private const val MORE_BUTTON_ANIMATION_DURATION_MS = 200
private val MoreButtonEndInset = 16.dp
private val MoreButtonReservedWidth = TvMoreButtonSize + 16.dp

package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.wrapContentHeight
import androidx.glance.layout.wrapContentSize
import au.com.shiftyjelly.pocketcasts.widget.action.OpenPocketCastsAction
import au.com.shiftyjelly.pocketcasts.widget.data.LargePlayerWidgetState
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun LargePlayer(state: LargePlayerWidgetState) {
    val headerAndPaddingHeight = 160.dp
    val availableContentHeight = LocalSize.current.height - headerAndPaddingHeight
    val fittingItemCount = (((availableContentHeight - 58.dp) / 66.dp).toInt().coerceAtLeast(0) + 1).coerceAtMost(LargePlayerWidgetState.QUEUE_LIMIT)
    val upNextEpisodes = state.upNextEpisodes.take(fittingItemCount)
    val actualDisplayedItemCount = upNextEpisodes.size
    val expectedContentHeight = contentHeight(fittingItemCount)
    val actualContentHeight = contentHeight(actualDisplayedItemCount)

    WidgetTheme(state.useDynamicColors) {
        RounderCornerBox(
            contentAlignment = Alignment.TopCenter,
            backgroundTint = LocalWidgetTheme.current.background,
            modifierCompat = GlanceModifier.fillMaxWidth().height(headerAndPaddingHeight + expectedContentHeight),
            modifier = GlanceModifier
                .clickable(OpenPocketCastsAction.action())
                .fillMaxWidth()
                .wrapContentSize(),
        ) {
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp),
            ) {
                LargePlayerHeader(state = state)
                if (upNextEpisodes.isNotEmpty()) {
                    Spacer(
                        modifier = GlanceModifier.height(12.dp),
                    )
                    LargePlayerQueue(
                        queue = upNextEpisodes,
                        useEpisodeArtwork = state.useEpisodeArtwork,
                        useDynamicColors = state.useDynamicColors,
                        modifier = GlanceModifier.height(actualContentHeight),
                    )
                    // This spacer is needed to enable tapping on empty content when queue doesn't fill the whole widget
                    Spacer(
                        modifier = GlanceModifier.fillMaxWidth().height(expectedContentHeight - actualContentHeight),
                    )
                } else {
                    Column(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(expectedContentHeight + 12.dp),
                    ) {
                        NonScalingText(
                            text = LocalContext.current.getString(LR.string.widget_nothing_in_up_next),
                            textSize = 16.dp,
                            useDynamicColors = state.useDynamicColors,
                            isBold = true,
                        )
                        Spacer(
                            modifier = GlanceModifier.height(4.dp),
                        )
                        NonScalingText(
                            text = LocalContext.current.getString(LR.string.widget_check_out_discover),
                            textSize = 13.dp,
                            useDynamicColors = state.useDynamicColors,
                            isTransparent = true,
                        )
                    }
                }
            }
        }
    }
}

private fun contentHeight(size: Int) = when (size) {
    0 -> 58
    else -> 58 + (size - 1) * 66
}.dp

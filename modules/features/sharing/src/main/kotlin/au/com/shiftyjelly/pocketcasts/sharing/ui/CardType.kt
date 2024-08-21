package au.com.shiftyjelly.pocketcasts.sharing.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp

sealed interface CardType {
    data object Vertical : VisualCardType {
        override val aspectRatio = 1.5f
    }
    data object Horizontal : VisualCardType {
        override val aspectRatio = 0.52f
    }
    data object Square : VisualCardType {
        override val aspectRatio = 1f
    }
    data object Audio : CardType

    companion object {
        val entires by lazy(LazyThreadSafetyMode.NONE) {
            listOf(
                CardType.Vertical,
                CardType.Horizontal,
                CardType.Square,
                CardType.Audio,
            )
        }

        val visualEntries by lazy(LazyThreadSafetyMode.NONE) {
            listOf(
                CardType.Vertical,
                CardType.Horizontal,
                CardType.Square,
            )
        }
    }
}

sealed interface VisualCardType : CardType {
    val aspectRatio: Float
}

@Composable
internal fun estimateCardCoordinates(
    topContentHeight: Int,
    scrollState: ScrollState,
): CardCoordinates {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    val maxWidth = when (screenHeight / screenWidth) {
        in 0f..1.35f -> 300.dp
        else -> 360.dp
    }
    val cardPadding = screenWidth / 8
    val availableWidth = (screenWidth - cardPadding * 2).coerceAtMost(maxWidth)
    val availableHeight = availableWidth * CardType.Vertical.aspectRatio // Vertical card has the most height so we calculate the size for it

    val density = LocalDensity.current

    return CardCoordinates(
        size = DpSize(availableWidth, availableHeight),
        padding = PaddingValues(horizontal = cardPadding),
        // Offset is helpful for scenrarios when card doesn't fit on the screen.
        // This way we can center horizontal and square cards in the view port
        // and not in the pager which can have content outside of the view port.
        offset = { type ->
            when (type) {
                CardType.Vertical -> IntOffset(0, 0)
                CardType.Horizontal, CardType.Square, CardType.Audio -> {
                    val viewPortHeight = scrollState.viewportSize
                    val isMeasured = viewPortHeight != 0

                    if (isMeasured && (scrollState.canScrollForward || scrollState.canScrollBackward)) {
                        val viewPortPagerPortion = density.run { (viewPortHeight - topContentHeight).toDp() }
                        val offsetValue = (viewPortPagerPortion - availableHeight) / 2
                        IntOffset(x = 0, y = density.run { offsetValue.roundToPx() })
                    } else {
                        IntOffset(0, 0)
                    }
                }
            }
        },
    )
}

internal class CardCoordinates(
    val size: DpSize,
    val padding: PaddingValues,
    val offset: (CardType) -> IntOffset,
)

package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class TooltipPositionProviderTest {
    private val density = Density(2f)
    private val anchorBounds = IntRect(left = 400, top = 100, right = 496, bottom = 196)
    private val popupSize = IntSize(width = 600, height = 300)

    @Test
    fun `popup below the anchor starts at the anchor bottom edge`() {
        for (tipPosition in TipPosition.entries.filter(TipPosition::isTopAligned)) {
            for (layoutDirection in LayoutDirection.entries) {
                val position = calculatePosition(tipPosition, layoutDirection)

                assertEquals("$tipPosition in $layoutDirection", anchorBounds.bottom, position.y)
            }
        }
    }

    @Test
    fun `popup above the anchor ends at the anchor top edge`() {
        for (tipPosition in TipPosition.entries.filter(TipPosition::isBottomAligned)) {
            for (layoutDirection in LayoutDirection.entries) {
                val position = calculatePosition(tipPosition, layoutDirection)

                assertEquals("$tipPosition in $layoutDirection", anchorBounds.top, position.y + popupSize.height)
            }
        }
    }

    @Test
    fun `anchor offset shifts the popup away from the anchor`() {
        val offset = DpOffset(8.dp, 4.dp)
        val offsetPx = density.run { 4.dp.roundToPx() }

        val belowAnchor = calculatePosition(TipPosition.TopStart, anchorOffset = offset)
        val aboveAnchor = calculatePosition(TipPosition.BottomStart, anchorOffset = offset)

        assertEquals(anchorBounds.bottom + offsetPx, belowAnchor.y)
        assertEquals(anchorBounds.top + offsetPx, aboveAnchor.y + popupSize.height)
    }

    private fun calculatePosition(
        tipPosition: TipPosition,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        anchorOffset: DpOffset = DpOffset.Zero,
    ) = TooltipPositionProvider(
        tipPosition = tipPosition,
        anchorOffset = anchorOffset,
        elevation = 24.dp,
        density = density,
    ).calculatePosition(
        anchorBounds = anchorBounds,
        windowSize = IntSize(1080, 1920),
        layoutDirection = layoutDirection,
        popupContentSize = popupSize,
    )
}

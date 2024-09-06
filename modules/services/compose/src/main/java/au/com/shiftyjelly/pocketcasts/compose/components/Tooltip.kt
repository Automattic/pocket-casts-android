@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "UNUSED_PARAMETER")

package au.com.shiftyjelly.pocketcasts.compose.components

import android.content.res.Configuration
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenuPositionProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * The Material3 TooltipBox comes with a couple of constraints:
 * Both TooltipBox and RichTooltipBox lack an onDismissRequest property: https://issuetracker.google.com/issues/349864868
 * Interaction with the rest of the screen is blocked until the tooltip is dismissed
 *
 * This is a custom Tooltip implementation using Popup based on
 * https://stackoverflow.com/a/69664787/193545
 * It permits screen interaction even when the tooltip is visible.
 */

@Composable
fun Tooltip(
    show: Boolean,
    modifier: Modifier = Modifier,
    offset: DpOffset = TooltipDefaults.tooltipOffset(),
    properties: PopupProperties = TooltipDefaults.PopupProperties,
    onDismissRequest: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val expandedStates = remember { MutableTransitionState(false) }
    expandedStates.targetState = show

    val density = LocalDensity.current
    val screenWidth = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val offsetFromEdge = with(density) { (TooltipDefaults.ArrowWidth + TooltipDefaults.ContentCornerRadius).toPx() }

    if (expandedStates.currentState || expandedStates.targetState) {
        // Tooltip anchorPointX calculation
        var anchorPointX by remember { mutableFloatStateOf(0f) }
        val popupPositionProvider = DropdownMenuPositionProvider(offset, density) { anchorBounds, tooltipBounds ->
            anchorPointX = if (anchorBounds.center.x + tooltipBounds.center.x / 2 > screenWidth) {
                tooltipBounds.width - offsetFromEdge
            } else {
                offsetFromEdge
            }
        }

        // Tooltip open/close animation
        val transition = updateTransition(expandedStates, "Tooltip")
        val alpha by transition.animateFloat(
            label = "alpha",
            transitionSpec = {
                if (false isTransitioningTo true) {
                    tween(durationMillis = TooltipDefaults.InTransitionDuration)
                } else {
                    tween(durationMillis = TooltipDefaults.OutTransitionDuration)
                }
            },
        ) { if (it) 1f else 0f }

        // Show popup
        Popup(
            onDismissRequest = onDismissRequest,
            popupPositionProvider = popupPositionProvider,
            properties = properties,
        ) {
            TooltipContent(
                modifier = modifier,
                shape = TooltipShape(anchorPointX),
                alpha = alpha,
                content = content,
            )
        }
    }
}

@Composable
private fun TooltipContent(
    modifier: Modifier,
    shape: TooltipShape,
    alpha: Float,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier
            .alpha(alpha),
        shape = shape,
        elevation = TooltipDefaults.ContentElevation,
    ) {
        Column(
            modifier = modifier
                .width(IntrinsicSize.Max)
                .verticalScroll(rememberScrollState()),
        ) {
            content()
        }
    }
}

class TooltipShape(
    private val anchorPointX: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = with(density) {
        val width = size.width
        val height = size.height
        val arrowWidthPx = TooltipDefaults.ArrowWidth.toPx()
        val arrowHeightPx = TooltipDefaults.ArrowHeight.toPx()
        val cornerRadiusPx = TooltipDefaults.ContentCornerRadius.toPx()

        val path = Path().apply {
            // Start from top-left corner
            moveTo(0f, arrowHeightPx + cornerRadiusPx)
            // Top-left corner
            quadraticBezierTo(0f, arrowHeightPx, cornerRadiusPx, arrowHeightPx)
            // Line to start of the arrow
            lineTo(anchorPointX - arrowWidthPx, arrowHeightPx)
            // Up to the tip of the arrow
            lineTo(anchorPointX, 0f)
            // Down to end of the arrow
            lineTo(anchorPointX + arrowWidthPx, arrowHeightPx)
            // Line to top-right corner
            lineTo(width - cornerRadiusPx, arrowHeightPx)
            // Top-right corner
            quadraticBezierTo(width, arrowHeightPx, width, arrowHeightPx + cornerRadiusPx)
            // Line to bottom-right corner
            lineTo(width, height - cornerRadiusPx)
            // Bottom-right corner
            quadraticBezierTo(width, height, width - cornerRadiusPx, height)
            // Line to bottom-left corner
            lineTo(cornerRadiusPx, height)
            // Bottom-left corner
            quadraticBezierTo(0f, height, 0f, height - cornerRadiusPx)
            close()
        }

        return Outline.Generic(path)
    }
}

object TooltipDefaults {
    val PopupProperties = PopupProperties(focusable = false)
    val ContentElevation = 8.dp
    val ContentCornerRadius = 4.dp
    val ArrowWidth = 10.dp
    val ArrowHeight = 10.dp

    @Composable
    fun tooltipOffset() =
        DpOffset(
            if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) -(16).dp else 16.dp,
            -(8).dp,
        )

    const val InTransitionDuration = 120
    const val OutTransitionDuration = 75
}
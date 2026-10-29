package au.com.shiftyjelly.pocketcasts.views.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import au.com.shiftyjelly.pocketcasts.compose.swipe.SwipeRow
import au.com.shiftyjelly.pocketcasts.compose.swipe.SwipeRowAction
import au.com.shiftyjelly.pocketcasts.compose.swipe.SwipeRowState
import com.google.android.material.checkbox.MaterialCheckBox
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun EpisodeRowContainer(
    swipeRowState: SwipeRowState,
    leadingActions: List<SwipeRowAction>,
    trailingActions: List<SwipeRowAction>,
    isMultiSelecting: Boolean,
    isSelected: Boolean,
    colors: EpisodeRowColors,
    contentDescription: String?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    SwipeRow(
        state = swipeRowState,
        leadingActions = leadingActions,
        trailingActions = trailingActions,
        isSwipeEnabled = !isMultiSelecting,
        actionIcon = { action -> SwipeActionIcon(action) },
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background(isMultiSelecting && isSelected))
                .semantics(mergeDescendants = true) {
                    if (contentDescription != null) {
                        this.contentDescription = contentDescription
                    }
                }
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .drawWithContent {
                    drawContent()
                    val dividerStart = 16.dp.roundToPx().toFloat()
                    val dividerHeight = 1.dp.roundToPx().toFloat()
                    drawRect(
                        color = colors.theme.primaryUi05,
                        topLeft = Offset(dividerStart, size.height - dividerHeight),
                        size = Size(size.width - dividerStart, dividerHeight),
                    )
                },
        ) {
            content()
        }
    }
}

@Composable
private fun SwipeActionIcon(action: SwipeRowAction) {
    Layout(
        content = {
            FitCenterDrawableIcon(action.iconId, action.iconTint, Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
        },
        modifier = Modifier
            .fillMaxHeight()
            .width(72.dp),
    ) { measurables, constraints ->
        val iconHeight = 48.dp.roundToPx()
        val placeable = measurables.single().measure(Constraints.fixed(constraints.maxWidth, iconHeight))
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(0, (constraints.maxHeight - iconHeight) / 2)
        }
    }
}

internal class MultiSelectionTransition(isMultiSelecting: Boolean) {
    val progress = Animatable(if (isMultiSelecting) 1f else 0f)
    val checkboxAlpha = Animatable(if (isMultiSelecting) 1f else 0f)
    val trailingAlpha = Animatable(if (isMultiSelecting) 0f else 1f)
    val isCheckboxShown by derivedStateOf { progress.value > 0f || checkboxAlpha.value > 0f }
    val isTrailingShown by derivedStateOf { progress.value < 1f || trailingAlpha.value > 0f }

    suspend fun update(isMultiSelecting: Boolean, animate: Boolean) {
        val target = if (isMultiSelecting) 1f else 0f
        val (outgoing, incoming) = if (isMultiSelecting) trailingAlpha to checkboxAlpha else checkboxAlpha to trailingAlpha
        if (animate) {
            outgoing.animateTo(0f, PhaseSpec)
            progress.animateTo(target, PhaseSpec)
            incoming.animateTo(1f, PhaseSpec)
        } else {
            outgoing.snapTo(0f)
            progress.snapTo(target)
            incoming.snapTo(1f)
        }
    }
}

private val AccelerateDecelerateEasing = Easing { fraction -> (cos((fraction + 1) * PI) / 2 + 0.5).toFloat() }
private val PhaseSpec = tween<Float>(durationMillis = 100, easing = AccelerateDecelerateEasing)

@Composable
internal fun rememberMultiSelectionTransition(isMultiSelecting: Boolean, animate: Boolean): MultiSelectionTransition {
    val transition = remember { MultiSelectionTransition(isMultiSelecting) }
    LaunchedEffect(isMultiSelecting) {
        transition.update(isMultiSelecting, animate)
    }
    return transition
}

@Composable
internal fun EpisodeRowCheckbox(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context -> MaterialCheckBox(context) },
        update = { checkbox ->
            checkbox.isChecked = isSelected
            checkbox.setOnClickListener {
                checkbox.isChecked = isSelected
                onClick()
            }
        },
        modifier = modifier,
    )
}

@Composable
internal fun EpisodeRowMainLayout(
    transition: MultiSelectionTransition,
    checkbox: (@Composable () -> Unit)?,
    artwork: (@Composable () -> Unit)?,
    trailing: (@Composable () -> Unit)?,
    content: @Composable () -> Unit,
) {
    val showCheckbox = checkbox != null && transition.isCheckboxShown
    val showTrailing = trailing != null && transition.isTrailingShown
    Layout(
        contents = listOf(
            { if (showCheckbox) Box(Modifier.graphicsLayer { alpha = transition.checkboxAlpha.value }, propagateMinConstraints = true) { checkbox() } else Unit },
            { artwork?.invoke() ?: Unit },
            content,
            { if (showTrailing) Box(Modifier.graphicsLayer { alpha = transition.trailingAlpha.value }, propagateMinConstraints = true) { trailing() } else Unit },
        ),
    ) { (checkboxes, artworks, contents, trailings), constraints ->
        val progress = transition.progress.value
        val width = constraints.maxWidth
        val margin = 16.dp.roundToPx()
        val verticalMargin = 12.dp.roundToPx()
        val artworkSize = 56.dp.roundToPx()
        val checkboxPlaceable = checkboxes.firstOrNull()?.measure(Constraints())
        val checkboxStart = 12.dp.roundToPx()
        val checkboxEnd = checkboxPlaceable?.let { ((checkboxStart + it.width) * progress).roundToInt() } ?: 0
        val artworkPlaceable = artworks.firstOrNull()?.measure(Constraints.fixed(artworkSize, artworkSize))
        val artworkEnd = if (artworkPlaceable != null) checkboxEnd + margin + artworkSize else checkboxEnd
        val trailingMeasurable = trailings.firstOrNull()
        val trailingWidth = trailingMeasurable?.maxIntrinsicWidth(constraints.maxHeight) ?: 0
        val contentStart = artworkEnd + margin
        val contentEnd = width - (trailingWidth * (1 - progress)).roundToInt() - 8.dp.roundToPx()
        val contentWidth = (contentEnd - contentStart).coerceAtLeast(0)
        val contentPlaceable = contents.single().measure(Constraints(minWidth = contentWidth, maxWidth = contentWidth))
        val height = maxOf(
            if (artworkPlaceable != null) artworkSize + verticalMargin * 2 else 0,
            verticalMargin + contentPlaceable.height,
            checkboxPlaceable?.height ?: 0,
        )
        val trailingPlaceable = trailingMeasurable?.measure(Constraints.fixed(trailingWidth, height))
        layout(width, height) {
            checkboxPlaceable?.place(checkboxStart, centered(height, checkboxPlaceable.height))
            artworkPlaceable?.place(checkboxEnd + margin, verticalMargin + centered(height - verticalMargin * 2, artworkSize))
            contentPlaceable.place(contentStart, verticalMargin)
            trailingPlaceable?.place(width - trailingWidth, 0)
        }
    }
}

private fun centered(space: Int, size: Int) = ((space - size) / 2f + 0.5f).toInt()

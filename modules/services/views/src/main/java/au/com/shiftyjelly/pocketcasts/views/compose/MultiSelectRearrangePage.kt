package au.com.shiftyjelly.pocketcasts.views.compose

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectAction
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodeAction
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectRearrangeTitle
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun MultiSelectRearrangePage(
    items: List<Any>,
    onNavigationClick: () -> Unit,
    onToolbarLongClick: () -> Unit,
    onItemMove: (from: Int, to: Int) -> Unit,
    onItemStartDrag: (position: Int) -> Unit,
    onItemDrop: (position: Int) -> Unit,
    modifier: Modifier = Modifier,
    includeStatusBarPadding: Boolean = true,
) {
    val currentItems by rememberUpdatedState(items)
    val listState = rememberLazyListState()
    val dragState = rememberRearrangeDragState(listState)
    dragState.isReorderable = { key -> currentItems.any { (it as? MultiSelectAction)?.groupId == key } }
    val indexOf = { key: Any -> currentItems.indexOfFirst { (it as? MultiSelectAction)?.groupId == key } }
    dragState.onMove = { from, to -> onItemMove(indexOf(from), indexOf(to)) }
    dragState.onStartDrag = { onItemStartDrag(indexOf(it)) }
    dragState.onDrop = { onItemDrop(indexOf(it)) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.theme.colors.primaryUi01),
    ) {
        RearrangeToolbar(
            onNavigationClick = onNavigationClick,
            onLongClick = onToolbarLongClick,
            includeStatusBarPadding = includeStatusBarPadding,
        )
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            modifier = Modifier
                .weight(1f)
                .rearrangeDragGestures(dragState),
        ) {
            items.forEach { item ->
                when (item) {
                    is MultiSelectRearrangeTitle -> item(key = "title_${item.title}") {
                        SectionTitle(stringResource(item.title), Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = MoveSpec))
                    }

                    is MultiSelectAction -> item(key = item.groupId) {
                        val key = item.groupId
                        MultiSelectActionRow(
                            action = item,
                            dragHandleModifier = Modifier.pointerInput(key) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    dragState.onHandleDown(key)
                                }
                            },
                            modifier = Modifier
                                .zIndex(if (dragState.isLifted(key)) 1f else 0f)
                                .animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = if (dragState.isActive(key)) null else MoveSpec)
                                .graphicsLayer {
                                    translationY = dragState.translationY(key)
                                    shadowElevation = dragState.elevation(key)
                                }
                                .background(dragState.background(key, MaterialTheme.theme.colors.primaryUi05)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberRearrangeDragState(listState: LazyListState): RearrangeDragState {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val density = LocalDensity.current
    return remember(listState, scope, haptics, context, density) {
        RearrangeDragState(
            listState = listState,
            scope = scope,
            haptics = haptics,
            liftElevation = 16.dpToPx(context).toFloat(),
            maxDragScroll = with(density) { 20.dp.roundToPx() },
        )
    }
}

private val MoveSpec = tween<IntOffset>(250, easing = AccelerateDecelerate)

@Composable
private fun RearrangeToolbar(
    onNavigationClick: () -> Unit,
    onLongClick: () -> Unit,
    includeStatusBarPadding: Boolean,
) {
    val haptics = LocalHapticFeedback.current
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val density = LocalDensity.current
    val titleStartPadding = with(density) { (72.dp.roundToPx() - 56.dp.roundToPx()).toDp() }
    Row(
        modifier = Modifier
            .zIndex(1f)
            .graphicsLayer { shadowElevation = with(density) { 4.dp.toPx() } }
            .fillMaxWidth()
            .background(MaterialTheme.theme.colors.secondaryUi01)
            .semantics {
                onLongClick {
                    currentOnLongClick()
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentOnLongClick()
                    },
                )
            }
            .then(if (includeStatusBarPadding) Modifier.windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top)) else Modifier)
            .height(56.dp),
    ) {
        ToolbarButton(
            contentDescription = stringResource(LR.string.back),
            onClick = onNavigationClick,
            modifier = Modifier
                .width(56.dp)
                .fillMaxHeight(),
        ) {
            DrawableIcon(IR.drawable.ic_arrow_back, MaterialTheme.theme.colors.secondaryIcon01, Modifier.size(24.dp).align(Alignment.Center))
        }
        ToolbarTitle(
            title = stringResource(LR.string.rearrange_actions),
            color = MaterialTheme.theme.colors.secondaryText01,
            modifier = Modifier
                .padding(start = titleStartPadding)
                .weight(1f)
                .align(TitleAlignment),
        )
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        TextViewLine(
            text = AnnotatedString(text),
            size = 15.sp,
            color = MaterialTheme.theme.colors.primaryText02,
            isMedium = true,
            maxLines = Int.MAX_VALUE,
        )
    }
}

@Preview
@Composable
private fun MultiSelectRearrangePagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        MultiSelectRearrangePage(
            items = remember {
                val actions = MultiSelectEpisodeAction.listFromIds(emptyList())
                listOf(MultiSelectRearrangeTitle(LR.string.multiselect_actions_shown)) + actions.take(4) +
                    MultiSelectRearrangeTitle(LR.string.multiselect_actions_hidden) + actions.drop(4)
            },
            onNavigationClick = {},
            onToolbarLongClick = {},
            onItemMove = { _, _ -> },
            onItemStartDrag = {},
            onItemDrop = {},
            includeStatusBarPadding = false,
        )
    }
}

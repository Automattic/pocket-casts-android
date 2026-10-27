package au.com.shiftyjelly.pocketcasts.views.compose

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.MenuRes
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.TooltipCompat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.getActivity
import au.com.shiftyjelly.pocketcasts.views.R
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectBottomSheet
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.MultiSelectViewOverflowMenuShownEvent
import kotlin.math.roundToInt
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun <T> MultiSelectTopBar(
    controller: MultiSelectController<T>,
    eventHorizon: EventHorizon,
    modifier: Modifier = Modifier,
    @MenuRes menuRes: Int? = null,
    includeStatusBarPadding: Boolean = true,
) {
    val context = LocalContext.current
    val selectedCount by controller.selectedCount.collectAsStateWithLifecycle()
    val toolbarItems by controller.toolbarItems.collectAsStateWithLifecycle()
    val staticItems = remember(menuRes, context) { menuRes?.let { staticMultiSelectToolbarItems(context.resources, it) } }
    MultiSelectTopBar(
        title = selectedCount.toString(),
        items = staticItems ?: toolbarItems,
        onNavigationClick = controller::exit,
        onItemClick = { itemId ->
            context.getActivity()?.let { activity -> controller.onMenuItemSelected(itemId, context.resources, activity) }
        },
        onBottomSheetClick = { sheet ->
            val helper = controller.helper as? MultiSelectEpisodesHelper
            if (helper != null) {
                eventHorizon.track(MultiSelectViewOverflowMenuShownEvent(source = helper.source.analyticsValue))
                context.getActivity()?.supportFragmentManager?.let { fragmentManager ->
                    MultiSelectBottomSheet.newInstance(sheet.actionIds, sheet.showRemoveListeningHistory)
                        .apply { multiSelectHelper = helper }
                        .show(fragmentManager, "multiselectbottomsheet")
                }
            }
        },
        includeStatusBarPadding = includeStatusBarPadding,
        modifier = modifier,
    )
}

@Composable
internal fun MultiSelectTopBar(
    title: String,
    items: MultiSelectToolbarItems,
    onNavigationClick: () -> Unit,
    onItemClick: (Int) -> Unit,
    onBottomSheetClick: (MultiSelectToolbarOverflow.BottomSheet) -> Unit,
    modifier: Modifier = Modifier,
    includeStatusBarPadding: Boolean = true,
) {
    val tint = MaterialTheme.theme.colors.primaryInteractive02
    val titleStartPadding = with(LocalDensity.current) { (72.dp.roundToPx() - 56.dp.roundToPx()).toDp() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.theme.colors.support01)
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
            DrawableIcon(IR.drawable.ic_arrow_back, tint, Modifier.size(24.dp).align(Alignment.Center))
        }
        ToolbarTitle(
            title = title,
            color = tint,
            modifier = Modifier
                .padding(start = titleStartPadding)
                .weight(1f)
                .align(TitleAlignment),
        )
        items.icons.forEach { item ->
            ActionButton(stringResource(item.title), item.iconRes, tint, onClick = { onItemClick(item.actionId) })
        }
        when (val overflow = items.overflow) {
            is MultiSelectToolbarOverflow.BottomSheet -> {
                ActionButton(stringResource(LR.string.more_options), IR.drawable.ic_more_vert_black_24dp, tint, onClick = { onBottomSheetClick(overflow) })
            }

            is MultiSelectToolbarOverflow.Menu -> if (overflow.items.isNotEmpty()) {
                OverflowMenuButton(overflow.items, tint, onItemClick)
            }

            MultiSelectToolbarOverflow.None -> Unit
        }
    }
}

@Composable
private fun ToolbarTitle(title: String, color: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val style = remember(context) { context.toolbarTitleStyle() }
    TextViewLine(
        text = AnnotatedString(title),
        size = with(LocalDensity.current) { style.textSizePx.toSp() },
        color = color,
        isMedium = style.isMedium,
        letterSpacing = style.letterSpacing.em,
        modifier = modifier,
    )
}

@Composable
private fun RowScope.ActionButton(
    contentDescription: String,
    @DrawableRes iconRes: Int,
    tint: Color,
    onClick: () -> Unit,
) {
    ToolbarButton(
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .align(ActionAlignment),
    ) {
        DrawableIcon(iconRes, tint, Modifier.size(24.dp).align(TruncatedCenter))
    }
}

@Composable
private fun RowScope.OverflowMenuButton(
    items: List<MultiSelectToolbarItem>,
    tint: Color,
    onItemClick: (Int) -> Unit,
) {
    val context = LocalContext.current
    val style = remember(context) { context.overflowButtonStyle() }
    val anchor = remember { mutableStateOf<View?>(null) }
    val popup = remember(anchor.value) {
        anchor.value?.let { PopupMenu(context, it, Gravity.END, androidx.appcompat.R.attr.actionOverflowMenuStyle, 0) }
    }
    val currentOnItemClick by rememberUpdatedState(onItemClick)
    DisposableEffect(popup, items) {
        popup?.menu?.apply {
            clear()
            items.forEach { item -> add(Menu.NONE, item.actionId, 0, item.title) }
        }
        popup?.setOnMenuItemClickListener { menuItem ->
            currentOnItemClick(menuItem.itemId)
            true
        }
        onDispose {}
    }
    ToolbarButton(
        contentDescription = style.contentDescription,
        onClick = { popup?.show() },
        touchListener = popup?.dragToOpenListener,
        onAnchor = { anchor.value = it },
        modifier = Modifier
            .height(48.dp)
            .align(ActionAlignment),
    ) {
        val density = LocalDensity.current
        DrawableIcon(
            drawableId = style.iconRes,
            tint = tint,
            modifier = Modifier
                .padding(start = with(density) { style.paddingStartPx.toDp() }, end = with(density) { style.paddingEndPx.toDp() })
                .size(24.dp)
                .align(Alignment.Center),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolbarButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    touchListener: View.OnTouchListener? = null,
    onAnchor: (View) -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val anchor = remember { mutableStateOf<View?>(null) }
    val currentTouchListener = rememberUpdatedState(touchListener)
    val interactionSource = remember { MutableInteractionSource() }
    val currentPress = remember { mutableStateOf<PressInteraction.Press?>(null) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            currentPress.value = interaction as? PressInteraction.Press
        }
    }
    Box(
        modifier = modifier
            .semantics { this.contentDescription = contentDescription }
            .forwardTouches(anchor, currentTouchListener) {
                currentPress.value?.let { interactionSource.tryEmit(PressInteraction.Cancel(it)) }
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false),
                role = Role.Button,
                hapticFeedbackEnabled = false,
                onLongClick = {
                    anchor.value?.let { view ->
                        val position = currentPress.value?.pressPosition
                        view.performLongClick(position?.x ?: (view.width / 2f), position?.y ?: (view.height / 2f))
                    }
                },
                onClick = onClick,
            ),
    ) {
        AndroidView(
            factory = { context ->
                PopupAnchorView(context).also {
                    it.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    anchor.value = it
                    onAnchor(it)
                }
            },
            update = { TooltipCompat.setTooltipText(it, contentDescription) },
            modifier = Modifier.matchParentSize(),
        )
        content()
    }
}

private class ToolbarTitleStyle(val textSizePx: Int, val letterSpacing: Float, val isMedium: Boolean)

private fun Context.toolbarTitleStyle(): ToolbarTitleStyle {
    val toolbar = obtainStyledAttributes(null, intArrayOf(androidx.appcompat.R.attr.titleTextAppearance), androidx.appcompat.R.attr.toolbarStyle, 0)
    val appearance = toolbar.getResourceId(0, 0)
    toolbar.recycle()
    val attrs = obtainStyledAttributes(appearance, intArrayOf(android.R.attr.textSize, android.R.attr.fontFamily, android.R.attr.letterSpacing))
    val style = ToolbarTitleStyle(
        textSizePx = attrs.getDimensionPixelSize(0, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20f, resources.displayMetrics).roundToInt()),
        letterSpacing = attrs.getFloat(2, 0f),
        isMedium = attrs.getString(1) == "sans-serif-medium",
    )
    attrs.recycle()
    return style
}

private class OverflowButtonStyle(val paddingStartPx: Int, val paddingEndPx: Int, @DrawableRes val iconRes: Int, val contentDescription: String)

private fun Context.overflowButtonStyle(): OverflowButtonStyle {
    val attrs = obtainStyledAttributes(
        null,
        intArrayOf(android.R.attr.contentDescription, android.R.attr.paddingStart, android.R.attr.paddingEnd, androidx.appcompat.R.attr.srcCompat),
        androidx.appcompat.R.attr.actionOverflowButtonStyle,
        0,
    )
    val style = OverflowButtonStyle(
        paddingStartPx = attrs.getDimensionPixelSize(1, 0),
        paddingEndPx = attrs.getDimensionPixelSize(2, 0),
        iconRes = attrs.getResourceId(3, IR.drawable.ic_more_vert_black_24dp),
        contentDescription = attrs.getString(0) ?: getString(LR.string.more_options),
    )
    attrs.recycle()
    return style
}

private val TitleAlignment = Alignment.Vertical { size, space -> (space - size) / 2 }

private val ActionAlignment = Alignment.Vertical { size, space -> space / 2 - size / 2 }

@Preview
@Composable
private fun MultiSelectTopBarPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        MultiSelectTopBar(
            title = "2",
            items = staticMultiSelectToolbarItems(LocalContext.current.resources, R.menu.menu_multiselect_upnext),
            onNavigationClick = {},
            onItemClick = {},
            onBottomSheetClick = {},
            includeStatusBarPadding = false,
        )
    }
}

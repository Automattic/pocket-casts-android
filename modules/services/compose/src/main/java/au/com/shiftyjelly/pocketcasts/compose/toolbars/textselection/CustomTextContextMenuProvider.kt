@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package au.com.shiftyjelly.pocketcasts.compose.toolbars.textselection

import android.content.Intent
import android.os.Looper
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuData
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSeparator
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastRoundToInt
import androidx.core.view.isNotEmpty
import au.com.shiftyjelly.pocketcasts.compose.extensions.getPrimaryClipText
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import kotlinx.coroutines.channels.Channel
import android.graphics.Rect as AndroidRect

/**
 * Installs a [CustomTextContextMenuProvider] for the text selection toolbar shown inside the
 * [content]'s `SelectionContainer`/`BasicText`.
 *
 * @param customMenuItems extra app-specific items to append to the toolbar (e.g. Share).
 * @param onHighlightText invoked once per active non-empty text selection.
 */
@Composable
fun ProvideTextSelectionToolbar(
    customMenuItems: List<CustomMenuItemOption>,
    onHighlightText: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val clipboard = LocalClipboard.current
    val latestOnTextHighlighted = rememberUpdatedState(onHighlightText)

    // onGloballyPositioned may fire with the same LayoutCoordinates containing different positioning
    // data, so always trigger read observation when this is set.
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null, neverEqualPolicy()) }

    val provider = remember(view, clipboard, customMenuItems) {
        CustomTextContextMenuProvider(
            view = view,
            clipboard = clipboard,
            customMenuItems = customMenuItems,
            coordinatesProvider = { coordinates },
            onTextHighlighted = { latestOnTextHighlighted.value?.invoke() },
        )
    }

    DisposableEffect(provider) {
        provider.start()
        onDispose { provider.dispose() }
    }

    CompositionLocalProvider(LocalTextContextMenuToolbarProvider provides provider) {
        Box(
            propagateMinConstraints = true,
            modifier = modifier.onGloballyPositioned { coordinates = it },
        ) {
            content()
        }
    }
}

/**
 * Custom implementation of [TextContextMenuProvider] that shows the platform floating text toolbar.
 *
 * It renders the components supplied by Compose (Copy, Select All, …) and appends [customMenuItems]
 * on top. Modeled on the internal `AndroidTextContextMenuToolbarProvider`.
 */
internal class CustomTextContextMenuProvider(
    private val view: View,
    private val clipboard: Clipboard,
    private val customMenuItems: List<CustomMenuItemOption>,
    private val coordinatesProvider: () -> LayoutCoordinates?,
    private val onTextHighlighted: (() -> Unit)?,
) : TextContextMenuProvider {
    private val textHighlightReporter = TextHighlightReporter(onTextHighlighted)

    private val snapshotStateObserver = SnapshotStateObserver(
        onChangedExecutor = { command ->
            if (view.handler?.looper === Looper.myLooper()) {
                command()
            } else {
                view.handler?.post(command)
            }
        },
    )

    private var actionMode: ActionMode? = null

    private val onDataChange: (Any) -> Unit = { actionMode?.invalidate() }
    private val onPositionChange: (Any) -> Unit = { actionMode?.invalidateContentRect() }

    fun start() {
        snapshotStateObserver.start()
    }

    fun dispose() {
        snapshotStateObserver.stop()
        snapshotStateObserver.clear()
        actionMode?.finish()
        actionMode = null
    }

    override suspend fun showTextContextMenu(dataProvider: TextContextMenuDataProvider) {
        val session = ChannelSession()
        val callback = ActionModeCallback(session, dataProvider)
        try {
            val mode = view.startActionMode(callback, ActionMode.TYPE_FLOATING)
            actionMode = mode
            if (mode == null) return
            callback.reportHighlightIfNeeded()
            session.awaitClose()
        } finally {
            textHighlightReporter.resetIfSelectionCleared(dataProvider.data())
            snapshotStateObserver.clear()
            actionMode?.finish()
            actionMode = null
        }
    }

    private fun onCustomMenuItemClicked(
        item: CustomMenuItemOption,
        data: TextContextMenuData,
        session: TextContextMenuSession,
    ) {
        when (item) {
            CustomMenuItemOption.Share -> {
                // Workaround until the selected text is exposed from the SelectionContainer: copy the
                // selection to the clipboard via Compose's own Copy action, then read it back.
                // It's fixed in the issue so it should be available soon.
                // https://issuetracker.google.com/issues/142551575
                val copyItem = data.components
                    .filterIsInstance<TextContextMenuItem>()
                    .firstOrNull { it.key === TextContextMenuKeys.CopyKey }
                copyItem?.let { with(it) { session.onClick() } }
                val text = clipboard.getPrimaryClipText().orEmpty()
                shareText(text)
            }
        }
    }

    private fun shareText(text: String) {
        try {
            val context = view.context
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share)))
        } catch (e: Exception) {
            LogBuffer.e(LogBuffer.TAG_CRASH, e, "Failed to share selected transcript text")
        }
    }

    private fun <T : Any> observeReadsAndGet(
        scope: Any,
        onValueChanged: (Any) -> Unit,
        block: () -> T,
    ): T {
        var result: T? = null
        snapshotStateObserver.observeReads(scope, onValueChanged) { result = block() }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    private inner class ActionModeCallback(
        private val session: TextContextMenuSession,
        private val dataProvider: TextContextMenuDataProvider,
    ) : ActionMode.Callback2() {
        private var previousData: TextContextMenuData? = null
        private var currentData: TextContextMenuData? = null

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            updateMenuItems(menu)
            return menu.isNotEmpty()
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return updateMenuItems(menu)
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = false

        override fun onDestroyActionMode(mode: ActionMode) {
            session.close()
        }

        override fun onGetContentRect(mode: ActionMode, view: View?, outRect: AndroidRect) {
            val rect = observeReadsAndGet("position", onPositionChange) {
                val destination = coordinatesProvider()?.takeIf(LayoutCoordinates::isAttached)
                if (destination == null) {
                    Rect.Zero
                } else {
                    dataProvider.contentBounds(destination).translate(destination.positionInRoot())
                }
            }
            outRect.set(
                rect.left.fastRoundToInt(),
                rect.top.fastRoundToInt(),
                rect.right.fastRoundToInt(),
                rect.bottom.fastRoundToInt(),
            )
        }

        fun reportHighlightIfNeeded() {
            currentData?.let(textHighlightReporter::reportIfNeeded)
        }

        /** @return whether the menu has changed. */
        private fun updateMenuItems(menu: Menu): Boolean {
            val data = observeReadsAndGet("data", onDataChange) { dataProvider.data() }
            currentData = data
            textHighlightReporter.resetIfSelectionCleared(data)
            if (actionMode != null) {
                textHighlightReporter.reportIfNeeded(data)
            }
            if (data == previousData) return false
            previousData = data

            menu.clear()
            var groupId = 1
            var order = 1
            data.components.fastForEach { component ->
                when (component) {
                    is TextContextMenuItem -> {
                        val menuItem = menu.add(groupId, order, order, component.label)
                        order++
                        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                        menuItem.setOnMenuItemClickListener {
                            with(component) { session.onClick() }
                            true
                        }
                    }

                    // Separators bump the group so custom items render in their own section.
                    is TextContextMenuSeparator -> groupId++

                    // Other component types (e.g. text classification actions) rely on internal
                    // APIs to render, so they are not shown — matching the previous toolbar.
                    else -> Unit
                }
            }
            customMenuItems.fastForEach { item ->
                val menuItem = menu.add(groupId, order, order, item.titleResource)
                order++
                menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                menuItem.setOnMenuItemClickListener {
                    onCustomMenuItemClicked(item, data, session)
                    session.close()
                    true
                }
            }
            return true
        }
    }

    private class ChannelSession : TextContextMenuSession {
        private val channel = Channel<Unit>(Channel.CONFLATED)

        override fun close() {
            channel.trySend(Unit)
        }

        suspend fun awaitClose() {
            channel.receive()
        }
    }
}

/**
 * Tracks the text selection but ignores dragging of the same text selection, or selection clear.
 */
internal class TextHighlightReporter(
    private val onTextHighlighted: (() -> Unit)?,
) {
    private var hasReportedActiveSelection = false

    fun reportIfNeeded(data: TextContextMenuData) {
        if (!data.hasCopyAction()) {
            hasReportedActiveSelection = false
            return
        }

        if (!hasReportedActiveSelection) {
            hasReportedActiveSelection = true
            onTextHighlighted?.invoke()
        }
    }

    fun resetIfSelectionCleared(data: TextContextMenuData) {
        if (!data.hasCopyAction()) {
            hasReportedActiveSelection = false
        }
    }
}

private fun TextContextMenuData.hasCopyAction(): Boolean {
    return components
        .filterIsInstance<TextContextMenuItem>()
        .any { it.key === TextContextMenuKeys.CopyKey }
}

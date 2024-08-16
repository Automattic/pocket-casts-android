package au.com.shiftyjelly.pocketcasts.compose.toolbars.textselection

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.geometry.Rect

private const val CUSTOM_MENU_ITEM_GROUP_ID = 1
private const val CUSTOM_MENU_ITEM_START_INDEX = 10 // Should be more than default max 4

internal class TextActionModeCallback(
    val onActionModeDestroy: (() -> Unit)? = null,
    var rect: Rect = Rect.Zero,
    var onCopyRequested: (() -> Unit)? = null,
    var onPasteRequested: (() -> Unit)? = null,
    var onCutRequested: (() -> Unit)? = null,
    var onSelectAllRequested: (() -> Unit)? = null,
    var customMenuItems: List<CustomMenuItemOption>? = null,
    var onCustomMenuActionRequested: ((CustomMenuItemOption) -> Unit)? = null,
) {
    fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        requireNotNull(menu) { "onCreateActionMode requires a non-null menu" }
        requireNotNull(mode) { "onCreateActionMode requires a non-null mode" }

        onCopyRequested?.let {
            addMenuItem(menu, MenuItemOption.Copy)
        }
        onPasteRequested?.let {
            addMenuItem(menu, MenuItemOption.Paste)
        }
        onCutRequested?.let {
            addMenuItem(menu, MenuItemOption.Cut)
        }
        onSelectAllRequested?.let {
            addMenuItem(menu, MenuItemOption.SelectAll)
        }
        customMenuItems?.forEachIndexed { index, item ->
            addCustomMenuItem(menu, item, index)
        }
        return true
    }

    // this method is called to populate new menu items when the actionMode was invalidated
    fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        if (mode == null || menu == null) return false
        updateMenuItems(menu)
        // should return true so that new menu items are populated
        return true
    }

    fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        item?.let {
            when (item.groupId) {
                0 -> {
                    when (item.itemId) {
                        MenuItemOption.Copy.id -> onCopyRequested?.invoke()
                        MenuItemOption.Paste.id -> onPasteRequested?.invoke()
                        MenuItemOption.Cut.id -> onCutRequested?.invoke()
                        MenuItemOption.SelectAll.id -> onSelectAllRequested?.invoke()
                        else -> return false
                    }
                }

                CUSTOM_MENU_ITEM_GROUP_ID -> {
                    customMenuItems?.get(item.order)
                        ?.let { onCustomMenuActionRequested?.invoke(it) }
                }

                else -> return false
            }
        }

        mode?.finish()
        return true
    }

    fun onDestroyActionMode() {
        onActionModeDestroy?.invoke()
    }

    @VisibleForTesting
    internal fun updateMenuItems(menu: Menu) {
        addOrRemoveMenuItem(menu, MenuItemOption.Copy, onCopyRequested)
        addOrRemoveMenuItem(menu, MenuItemOption.Paste, onPasteRequested)
        addOrRemoveMenuItem(menu, MenuItemOption.Cut, onCutRequested)
        addOrRemoveMenuItem(menu, MenuItemOption.SelectAll, onSelectAllRequested)
        customMenuItems?.forEachIndexed { index, it ->
            addOrRemoveCustomMenuItem(menu, it, index, onCustomMenuActionRequested)
        }
    }

    internal fun addMenuItem(menu: Menu, item: MenuItemOption) {
        menu.add(0, item.id, item.order, item.titleResource)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
    }

    internal fun addCustomMenuItem(menu: Menu, item: CustomMenuItemOption, index: Int) {
        menu.add(CUSTOM_MENU_ITEM_GROUP_ID, index + CUSTOM_MENU_ITEM_START_INDEX, index, item.titleResource)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
    }

    private fun addOrRemoveMenuItem(
        menu: Menu,
        item: MenuItemOption,
        callback: (() -> Unit)?,
    ) {
        when {
            callback != null && menu.findItem(item.id) == null -> addMenuItem(menu, item)
            callback == null && menu.findItem(item.id) != null -> menu.removeItem(item.id)
        }
    }

    private fun addOrRemoveCustomMenuItem(
        menu: Menu,
        item: CustomMenuItemOption,
        index: Int,
        callback: ((d: CustomMenuItemOption) -> Unit)?,
    ) {
        when {
            callback != null && menu.findItem(index + CUSTOM_MENU_ITEM_START_INDEX) == null ->
                addCustomMenuItem(menu, item, index)

            callback == null && menu.findItem(index + CUSTOM_MENU_ITEM_START_INDEX) != null ->
                menu.removeItem(index + CUSTOM_MENU_ITEM_START_INDEX)
        }
    }
}

internal enum class MenuItemOption(val id: Int) {
    Copy(0),
    Paste(1),
    Cut(2),
    SelectAll(3),
    ;

    val titleResource: Int
        get() = when (this) {
            Copy -> android.R.string.copy
            Paste -> android.R.string.paste
            Cut -> android.R.string.cut
            SelectAll -> android.R.string.selectAll
        }

    /**
     * This item will be shown before all items that have order greater than this value.
     */
    val order = id
}

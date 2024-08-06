package au.com.shiftyjelly.pocketcasts.compose.toolbars.textselection

import android.content.Intent
import android.content.Intent.createChooser
import android.view.ActionMode
import android.view.View
import androidx.annotation.DoNotInline
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import au.com.shiftyjelly.pocketcasts.localization.R

/**
 * Custom implementation for [TextToolbar].
 * Refers Compose class androidx.compose.ui.platform.AndroidTextToolbar.
 */
class CustomTextToolbar(
    private val view: View,
    private val customMenuItems: List<CustomMenuItemOption>,
    private val clipboardManager: ClipboardManager,
) : TextToolbar {
    private var actionMode: ActionMode? = null
    private val textActionModeCallback = TextActionModeCallback(
        onActionModeDestroy = {
            actionMode = null
        },
    )
    override var status: TextToolbarStatus = TextToolbarStatus.Hidden
        private set

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        textActionModeCallback.rect = rect
        textActionModeCallback.onCopyRequested = onCopyRequested
        textActionModeCallback.onCutRequested = onCutRequested
        textActionModeCallback.onPasteRequested = onPasteRequested
        textActionModeCallback.onSelectAllRequested = onSelectAllRequested
        textActionModeCallback.customMenuItems = customMenuItems
        textActionModeCallback.onCustomMenuActionRequested = { onCustomMenuItemClicked(it) }
        if (actionMode == null) {
            status = TextToolbarStatus.Shown
            actionMode =
                TextToolbarHelperMethods.startActionMode(
                    view,
                    FloatingTextActionModeCallback(textActionModeCallback),
                    ActionMode.TYPE_FLOATING,
                )
        } else {
            actionMode?.invalidate()
        }
    }

    override fun hide() {
        status = TextToolbarStatus.Hidden
        actionMode?.finish()
        actionMode = null
    }

    private fun onCustomMenuItemClicked(item: CustomMenuItemOption) {
        try {
            val text = clipboardManager.getText()
            when (item) {
                CustomMenuItemOption.Share -> {
                    val context = view.context
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, text)
                        type = "text/plain"
                    }
                    context.startActivity(createChooser(shareIntent, context.getString(R.string.share)))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * This class is here to ensure that the classes that use this API will get verified and can be
 * AOT compiled. It is expected that this class will soft-fail verification, but the classes
 * which use this method will pass.
 */
internal object TextToolbarHelperMethods {
    @DoNotInline
    fun startActionMode(
        view: View,
        actionModeCallback: ActionMode.Callback,
        type: Int,
    ): ActionMode? {
        return view.startActionMode(
            actionModeCallback,
            type,
        )
    }
}

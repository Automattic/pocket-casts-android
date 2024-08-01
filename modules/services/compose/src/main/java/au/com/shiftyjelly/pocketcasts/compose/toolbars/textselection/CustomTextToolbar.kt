package au.com.shiftyjelly.pocketcasts.compose.toolbars.textselection

import android.view.ActionMode
import android.view.View
import androidx.annotation.DoNotInline
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus

/**
 * Custom implementation for [TextToolbar].
 * Refers Compose class androidx.compose.ui.platform.AndroidTextToolbar.
 */
class CustomTextToolbar(
    private val view: View,
) : TextToolbar {
    private var actionMode: ActionMode? = null
    private val textActionModeCallback = TextActionModeCallback(
        onActionModeDestroy = {
            actionMode = null
        }
    )
    override var status: TextToolbarStatus = TextToolbarStatus.Hidden
        private set

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        textActionModeCallback.rect = rect
        textActionModeCallback.onCopyRequested = onCopyRequested
        textActionModeCallback.onCutRequested = onCutRequested
        textActionModeCallback.onPasteRequested = onPasteRequested
        textActionModeCallback.onSelectAllRequested = onSelectAllRequested
        if (actionMode == null) {
            status = TextToolbarStatus.Shown
            actionMode =
                TextToolbarHelperMethods.startActionMode(
                    view,
                    FloatingTextActionModeCallback(textActionModeCallback),
                    ActionMode.TYPE_FLOATING
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
        type: Int
    ): ActionMode? {
        return view.startActionMode(
            actionModeCallback,
            type
        )
    }
}




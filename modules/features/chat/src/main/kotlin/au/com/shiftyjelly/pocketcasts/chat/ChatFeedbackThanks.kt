package au.com.shiftyjelly.pocketcasts.chat

import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import au.com.shiftyjelly.pocketcasts.views.extensions.applyLightToastStyle
import com.google.android.material.snackbar.Snackbar
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal fun Fragment.showChatFeedbackThanks() {
    val root = (parentFragment as? DialogFragment)?.dialog?.window?.decorView?.findViewById<View>(android.R.id.content)
        ?: activity?.findViewById(android.R.id.content)
        ?: return
    Snackbar.make(root, getString(LR.string.chat_feedback_thanks), Snackbar.LENGTH_SHORT)
        .applyLightToastStyle()
        .show()
}

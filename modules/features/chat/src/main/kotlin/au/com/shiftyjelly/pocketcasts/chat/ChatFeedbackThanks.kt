package au.com.shiftyjelly.pocketcasts.chat

import android.app.Activity
import android.graphics.Color
import android.view.View
import com.google.android.material.snackbar.Snackbar
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal fun Activity.showChatFeedbackThanks() {
    val root = findViewById<View>(android.R.id.content) ?: return
    Snackbar.make(root, getString(LR.string.chat_feedback_thanks), Snackbar.LENGTH_SHORT)
        .setBackgroundTint(Color.WHITE)
        .setTextColor(Color.BLACK)
        .show()
}

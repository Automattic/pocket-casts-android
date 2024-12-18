package au.com.shiftyjelly.pocketcasts.views.extensions

import android.text.SpannableStringBuilder
import android.text.Spanned
import timber.log.Timber

/**
 * Removes top and bottom padding.
 */
fun Spanned.trimPadding(): Spanned {
    val spannable = this as? SpannableStringBuilder ?: return this

    try {
        var trimStart = 0
        var trimEnd = 0

        var text = spannable.toString()

        if (text.isEmpty()) {
            return this
        }

        while (text.startsWith("\n")) {
            text = text.substring(1)
            trimStart += 1
        }
        if (trimStart > 0) {
            spannable.delete(0, trimStart)
        }

        while (text.endsWith("\n")) {
            text = text.substring(0, text.length - 1)
            trimEnd += 1
        }
        if (trimEnd > 0) {
            spannable.delete(spannable.length - trimEnd, spannable.length)
        }
    } catch (e: Exception) {
        Timber.e(e)
    }

    return spannable
}

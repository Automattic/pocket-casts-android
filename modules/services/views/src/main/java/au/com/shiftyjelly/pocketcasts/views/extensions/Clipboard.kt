package au.com.shiftyjelly.pocketcasts.views.extensions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.core.content.getSystemService
import au.com.shiftyjelly.pocketcasts.localization.R

fun Context.copyLinkToClipboard(url: String) {
    val text = linkClipboardText(url) ?: return

    requireNotNull(getSystemService<ClipboardManager>()).setPrimaryClip(
        ClipData.newPlainText(getString(R.string.share_label_copy_link), text),
    )
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        Toast.makeText(this, R.string.share_link_copied_feedback, Toast.LENGTH_SHORT).show()
    }
}

internal fun linkClipboardText(url: String): String? {
    val text = if (url.startsWith(MAILTO_SCHEME, ignoreCase = true)) {
        url.drop(MAILTO_SCHEME.length)
    } else {
        url
    }
    return text.takeIf(String::isNotBlank)
}

private const val MAILTO_SCHEME = "mailto:"

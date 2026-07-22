package au.com.shiftyjelly.pocketcasts.views.extensions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.core.content.getSystemService
import au.com.shiftyjelly.pocketcasts.localization.R

fun Context.copyLinkToClipboard(url: String) {
    if (url.isBlank()) return

    requireNotNull(getSystemService<ClipboardManager>()).setPrimaryClip(
        ClipData.newPlainText(getString(R.string.share_label_copy_link), url),
    )
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        Toast.makeText(this, R.string.share_link_copied_feedback, Toast.LENGTH_SHORT).show()
    }
}

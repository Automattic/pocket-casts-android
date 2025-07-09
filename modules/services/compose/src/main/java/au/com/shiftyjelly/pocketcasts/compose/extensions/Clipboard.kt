package au.com.shiftyjelly.pocketcasts.compose.extensions

import android.content.ClipData
import androidx.compose.ui.platform.Clipboard
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer

fun Clipboard.getPrimaryClipText(): String? = try {
    nativeClipboard.primaryClip?.getItemAtOrNull(0)?.text?.toString()
} catch (e: Exception) {
    LogBuffer.e(LogBuffer.TAG_CRASH, e, "Failed to get primary clip text from clipboard")
    null
}

fun ClipData.getItemAtOrNull(index: Int): ClipData.Item? = if (itemCount == 0 || index < 0 || index >= itemCount) null else getItemAt(index)

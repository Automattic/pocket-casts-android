package au.com.shiftyjelly.pocketcasts.ui.extensions

import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.fragment.app.Fragment
import timber.log.Timber

fun Fragment.openUrl(url: String) {
    try {
        this.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        Timber.e(e)
    }
}

@Suppress("DEPRECATION")
fun Fragment.setupKeyboardModeResize() {
    activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
}

fun Fragment.setupKeyboardModePan() {
    activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
}

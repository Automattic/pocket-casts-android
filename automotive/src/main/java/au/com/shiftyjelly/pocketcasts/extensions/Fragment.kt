package au.com.shiftyjelly.pocketcasts.extensions

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment
import timber.log.Timber

fun Fragment.openUrl(url: String) {
    try {
        this.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        Timber.w(e)
    }
}

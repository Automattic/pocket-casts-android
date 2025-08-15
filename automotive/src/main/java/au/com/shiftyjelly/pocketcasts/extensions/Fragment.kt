package au.com.shiftyjelly.pocketcasts.extensions

import android.content.Intent
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import au.com.shiftyjelly.pocketcasts.AutomotiveLinkFragment
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import timber.log.Timber

fun Fragment.openUrl(url: String) {
    try {
        this.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (e: Exception) {
        Timber.e(e)
        // fallback to showing the URL with a QR code
        (activity as? FragmentHostListener)?.addFragment(AutomotiveLinkFragment.newInstance(url))
    }
}

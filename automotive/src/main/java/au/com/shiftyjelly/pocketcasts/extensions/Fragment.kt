package au.com.shiftyjelly.pocketcasts.extensions

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment
import au.com.shiftyjelly.pocketcasts.AutomotiveLinkFragment
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener

fun Fragment.openUrl(url: String) {
    try {
        this.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        // fallback to showing the URL with a QR code
        (activity as? FragmentHostListener)?.addFragment(AutomotiveLinkFragment.newInstance(url))
    }
}

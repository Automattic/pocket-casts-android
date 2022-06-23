package au.com.shiftyjelly.pocketcasts.ui.extensions

import android.content.res.Configuration

fun Configuration.inLandscape(): Boolean {
    return orientation == Configuration.ORIENTATION_LANDSCAPE
}

fun Configuration.inPortrait(): Boolean {
    return !inLandscape()
}

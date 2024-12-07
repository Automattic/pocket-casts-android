package au.com.shiftyjelly.pocketcasts.compose.extensions

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content

// don't consume the insets or older versions of Android such as SDK 26 it will effect the layout of other areas of the app such as the What's New bottom sheet
fun Fragment.contentWithoutConsumedInsets(contentWithoutInsets: @Composable () -> Unit): ComposeView {
    return content {
        contentWithoutInsets()
    }.apply {
        consumeWindowInsets = false
    }
}

package au.com.shiftyjelly.pocketcasts.player.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import au.com.shiftyjelly.pocketcasts.views.extensions.setSystemWindowInsetToHeight

class StatusBarSpacer @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    init {
        setSystemWindowInsetToHeight(top = true)
    }
}

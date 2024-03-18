package au.com.shiftyjelly.pocketcasts.podcasts.helper

import android.text.TextUtils
import android.view.ViewGroup
import android.widget.TextView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager

fun TextView.readMore(collapsedLines: Int) {
    maxLines = collapsedLines
    ellipsize = TextUtils.TruncateAt.END
    setOnClickListener {
        val transition = ChangeBounds().apply {
            duration = 200
            interpolator = FastOutSlowInInterpolator()
        }
        TransitionManager.beginDelayedTransition(parent as ViewGroup, transition)
        maxLines = if (maxLines > collapsedLines) collapsedLines else Int.MAX_VALUE
    }
}

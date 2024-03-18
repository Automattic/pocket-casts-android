package au.com.shiftyjelly.pocketcasts.podcasts.helper

import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButtonType

object BindingAdapters {
    
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
}

package au.com.shiftyjelly.pocketcasts.views.extensions

import androidx.annotation.ColorInt
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.graphics.drawable.DrawableCompat
import androidx.mediarouter.R
import androidx.mediarouter.app.MediaRouteButton

fun MediaRouteButton.updateColor(@ColorInt color: Int?) {
    color ?: return
    val castContext = ContextThemeWrapper(context, R.style.Theme_MediaRouter)
    val attrs = castContext.obtainStyledAttributes(null, R.styleable.MediaRouteButton, R.attr.mediaRouteButtonStyle, 0)
    val drawable = attrs.getDrawable(R.styleable.MediaRouteButton_externalRouteEnabledDrawable) ?: return
    attrs.recycle()
    DrawableCompat.setTint(drawable, color)
    drawable.state = drawableState
    setRemoteIndicatorDrawable(drawable)
}

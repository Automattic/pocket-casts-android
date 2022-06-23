package au.com.shiftyjelly.pocketcasts.views.extensions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

fun ViewGroup.inflate(@LayoutRes layoutId: Int, attachToThis: Boolean = true): View {
    return LayoutInflater.from(context).inflate(layoutId, this, attachToThis)
}

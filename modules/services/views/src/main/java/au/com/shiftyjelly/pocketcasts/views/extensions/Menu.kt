package au.com.shiftyjelly.pocketcasts.views.extensions

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.ColorInt
import androidx.core.view.children
import androidx.core.view.iterator
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import timber.log.Timber

fun Menu.hideItem(vararg ids: Int) {
    ids.forEach {
        this.findItem(it)?.isVisible = false
    }
}

fun Menu.showItemIf(id: Int, show: Boolean) {
    this.findItem(id)?.isVisible = show
}

fun Menu.tintIcons(@ColorInt color: Int, excludeMenuItems: List<Int>? = null) {
    for (item in this.iterator()) {
        // tint the chrome cast icon
        val actionView = item.actionView
        if (actionView != null && actionView is MediaRouteButton) {
            actionView.updateColor(color)
            continue
        }
        // tint the other icons
        val drawable = item.icon
        val exclude = excludeMenuItems != null && excludeMenuItems.contains(item.itemId)
        if (drawable != null && !exclude) {
            drawable.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        }
    }
}

private fun Menu.findChromeCastItem(): MenuItem? {
    return this.children.firstOrNull { it.actionView is MediaRouteButton }
}

fun Menu.setupChromeCastButton(context: Context?, onClick: () -> Unit) {
    context ?: return
    try {
        val menuItem = findChromeCastItem() ?: return
        CastButtonFactory.setUpMediaRouteButton(context, this, menuItem.itemId)
        menuItem.actionView?.setOnClickListener { onClick() }
    } catch (e: Throwable) {
        Timber.e(e)
    }
}

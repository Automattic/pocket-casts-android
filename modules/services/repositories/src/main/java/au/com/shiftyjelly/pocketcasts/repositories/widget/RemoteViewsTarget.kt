package au.com.shiftyjelly.pocketcasts.repositories.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.RemoteViews
import androidx.annotation.IdRes
import coil.target.Target

class RemoteViewsTarget(val context: Context, private val componentName: ComponentName, private val remoteViews: RemoteViews, @IdRes private val imageViewId: Int) : Target {
    override fun onStart(placeholder: Drawable?) {
        setDrawable(placeholder)
    }

    override fun onError(error: Drawable?) {
        setDrawable(error)
    }

    override fun onSuccess(result: Drawable) {
        setDrawable(result)
    }

    private fun setDrawable(drawable: Drawable?) {
        remoteViews.setImageViewBitmap(imageViewId, (drawable as? BitmapDrawable)?.bitmap)
        update()
    }

    private fun update() {
        val appWidgetManager = AppWidgetManager.getInstance(this.context)
        appWidgetManager.updateAppWidget(this.componentName, remoteViews)
    }
}

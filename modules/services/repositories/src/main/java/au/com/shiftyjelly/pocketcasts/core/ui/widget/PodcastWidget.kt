package au.com.shiftyjelly.pocketcasts.core.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.widget.WidgetManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * The podcast widget.
 * This widget can't move from the core package or it gets deleted when the new APK is installed.
 */
@AndroidEntryPoint
class PodcastWidget : AppWidgetProvider(), CoroutineScope {

    @Inject lateinit var widgetManager: WidgetManager
    @Inject lateinit var playbackManager: PlaybackManager

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    override fun onUpdate(context: Context, manager: AppWidgetManager, widgetIds: IntArray) {
        launch {
            widgetManager.updateWidgetFromProvider(context, manager, widgetIds, playbackManager)
        }

        Timber.i("Widget onUpdate called.")
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Timber.i("Widget onDisabled called.")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Timber.i("Widget onReceive called.")
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Timber.i("Widget onEnabled called.")
    }
}

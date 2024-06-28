package au.com.shiftyjelly.pocketcasts.core.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.widget.WidgetManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * The podcast widget.
 * This widget can't move from the core package or it gets deleted when the new APK is installed.
 */
@AndroidEntryPoint
class PodcastWidget : AppWidgetProvider(), CoroutineScope {

    companion object {
        const val SKIP_FORWARD_REQUEST_CODE = 0
        const val SKIP_FORWARD_ACTION = "SKIP_FORWARD_ACTION"
        const val SKIP_BACKWARD_REQUEST_CODE = 1
        const val SKIP_BACKWARD_ACTION = "SKIP_BACKWARD_ACTION"
        const val PLAY_REQUEST_CODE = 2
        const val PLAY_ACTION = "PLAY_ACTION"
        const val PAUSE_REQUEST_CODE = 3
        const val PAUSE_ACTION = "PAUSE_ACTION"
    }

    @Inject lateinit var widgetManager: WidgetManager

    @Inject lateinit var playbackManager: PlaybackManager

    @Inject lateinit var tracker: AnalyticsTrackerWrapper

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    override fun onUpdate(context: Context, manager: AppWidgetManager, widgetIds: IntArray) {
        launch {
            widgetManager.updateWidgetFromProvider(context, manager, widgetIds, playbackManager)
        }

        Timber.i("Widget onUpdate called.")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != null) {
            when (intent.action) {
                SKIP_FORWARD_ACTION -> { playbackManager.skipForward(sourceView = SourceView.WIDGET_PLAYER_OLD) }
                SKIP_BACKWARD_ACTION -> { playbackManager.skipBackward(sourceView = SourceView.WIDGET_PLAYER_OLD) }
                PLAY_ACTION -> { playbackManager.playQueue(sourceView = SourceView.WIDGET_PLAYER_OLD) }
                PAUSE_ACTION -> { playbackManager.pause(sourceView = SourceView.WIDGET_PLAYER_OLD) }
            }
        }
    }

    override fun onEnabled(context: Context) {
        tracker.track(AnalyticsEvent.WIDGET_INSTALLED, mapOf("widget_type" to "player_old"))
    }

    override fun onDisabled(context: Context) {
        tracker.track(AnalyticsEvent.WIDGET_UNINSTALLED, mapOf("widget_type" to "player_old"))
    }
}

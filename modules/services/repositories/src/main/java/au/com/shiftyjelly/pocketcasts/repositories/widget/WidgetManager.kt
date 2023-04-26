package au.com.shiftyjelly.pocketcasts.repositories.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager

interface WidgetManager {
    fun updateWidget(podcast: Podcast?, playing: Boolean, playingEpisode: BaseEpisode?)
    fun updateWidgetFromProvider(context: Context, manager: AppWidgetManager, widgetIds: IntArray, playbackManager: PlaybackManager?)
    fun updateWidgetFromPlaybackState(playbackManager: PlaybackManager?)
    fun updateWidgetNotPlaying()
}

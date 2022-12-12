package au.com.shiftyjelly.pocketcasts.repositories.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.RemoteViews
import androidx.media.session.MediaButtonReceiver
import au.com.shiftyjelly.pocketcasts.core.ui.widget.PodcastWidget
import au.com.shiftyjelly.pocketcasts.models.db.helper.UserEpisodePodcastSubstitute
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.R
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImageLoader
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.extensions.getLaunchActivityPendingIntent
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR

class WidgetManagerImpl @Inject constructor(
    private val settings: Settings,
    private val podcastManager: PodcastManager,
    @ApplicationContext private val context: Context
) : WidgetManager {

    override fun updateWidget(podcast: Podcast?, playing: Boolean, playingEpisode: Playable?) {
        if (Util.isAutomotive(context)) {
            return
        }

        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)

            val views = RemoteViews(context.packageName, R.layout.widget)
            val widgetName = ComponentName(context, PodcastWidget::class.java)
            if (playingEpisode == null) {
                showPlayingControls(false, views)
            } else {
                showPlayingControls(true, views)
                updateArtWork(podcast, playingEpisode, views, widgetName, context)
                showPlayButton(playing, views)
                updateSkipAmounts(views, settings)
            }
            updateOnClicks(views, context)
            appWidgetManager.updateAppWidget(widgetName, views)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun updateWidgetFromPlaybackState(playbackManager: PlaybackManager?) {
        val episode = playbackManager?.getCurrentEpisode() ?: return
        val podcast = findPodcastByEpisode(episode)
        updateWidget(podcast, playbackManager.isPlaying(), playbackManager.getCurrentEpisode())
    }

    private fun findPodcastByEpisode(episode: Playable): Podcast? {
        return when (episode) {
            is Episode -> podcastManager.findPodcastByUuid(episode.podcastUuid)
            is UserEpisode -> podcastManager.buildUserEpisodePodcast(episode)
            else -> null
        }
    }

    @Suppress("SENSELESS_COMPARISON")
    override fun updateWidgetFromProvider(context: Context, manager: AppWidgetManager, widgetIds: IntArray, playbackManager: PlaybackManager?) {
        if (context == null || Util.isAutomotive(context)) {
            return
        }

        val isPlaying = playbackManager != null && playbackManager.isPlaying()

        val widgetName = ComponentName(context, PodcastWidget::class.java)

        for (i in widgetIds.indices) {
            val widgetId = widgetIds[i]

            val views = RemoteViews(context.packageName, R.layout.widget)
            updateOnClicks(views, context)
            updateSkipAmounts(views, settings)
            setupView(views, isPlaying, playbackManager, widgetName, context)

            try {
                manager.updateAppWidget(widgetId, views)
            } catch (e: Exception) {
                // sometimes widgets are not able to be updated, ignore this one and move on to the next one
                Timber.e(e)
            }
        }
    }

    private fun showPlayingControls(visible: Boolean, views: RemoteViews) {
        views.setViewVisibility(R.id.widget_empty_player, if (visible) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.widget_podcast_playing, if (visible) View.VISIBLE else View.GONE)
    }

    private fun setupView(views: RemoteViews, isPlaying: Boolean, playbackManager: PlaybackManager?, widgetName: ComponentName, context: Context) {
        val episode = playbackManager?.getCurrentEpisode()
        if (episode == null) {
            showPlayingControls(false, views)
        } else {
            showPlayingControls(true, views)
            showPlayButton(isPlaying, views)
            val podcast = findPodcastByEpisode(episode)
            updateArtWork(podcast, playbackManager.getCurrentEpisode(), views, widgetName, context)
        }
    }

    private fun updateOnClicks(views: RemoteViews, context: Context) {
        with(views) {
            getSkipBackIntent()?.let { intent -> setOnClickPendingIntent(R.id.widget_skip_back, intent) }
            getSkipForwardIntent()?.let { intent -> setOnClickPendingIntent(R.id.widget_skip_forward, intent) }
            getPlayIntent()?.let { intent -> setOnClickPendingIntent(R.id.widget_play_button, intent) }
            getPauseIntent()?.let { intent -> setOnClickPendingIntent(R.id.widget_pause_button, intent) }
            val openAppIntent = getOpenAppIntent(context)
            setOnClickPendingIntent(R.id.widget_artwork, openAppIntent)
            setOnClickPendingIntent(R.id.widget_empty_player, openAppIntent)
        }
    }

    private fun updateSkipAmounts(views: RemoteViews, settings: Settings) {
        val jumpFwdAmount = settings.getSkipForwardInSecs()
        val jumpBackAmount = settings.getSkipBackwardInSecs()

        views.setTextViewText(R.id.widget_skip_back_text, "$jumpBackAmount")
        views.setContentDescription(R.id.widget_skip_back_text, "Skip back $jumpBackAmount seconds")
        views.setTextViewText(R.id.widget_skip_forward_text, "$jumpFwdAmount")
        views.setContentDescription(R.id.widget_skip_forward_text, "Skip forward $jumpFwdAmount seconds")
    }

    private fun updateArtWork(podcast: Podcast?, playingEpisode: Playable?, views: RemoteViews, widgetName: ComponentName, context: Context) {
        if (playingEpisode == null) {
            views.setImageViewResource(R.id.widget_artwork, IR.drawable.defaultartwork)
            views.setContentDescription(R.id.widget_artwork, "Open Pocket Casts")
            return
        }

        val podcastTitle = podcast?.title ?: UserEpisodePodcastSubstitute.substituteTitle
        views.setContentDescription(R.id.widget_artwork, "$podcastTitle. Open Pocket Casts")
        views.setImageViewResource(R.id.widget_artwork, IR.drawable.defaultartwork_small_dark)

        val target = RemoteViewsTarget(
            context,
            widgetName,
            views,
            R.id.widget_artwork
        )
        val imageLoader = PodcastImageLoader(context = context, isDarkTheme = true, transformations = emptyList())
        if (playingEpisode is UserEpisode) {
            imageLoader.smallPlaceholder().loadForTarget(playingEpisode, 128, target)
        } else if (podcast != null) {
            imageLoader.smallPlaceholder().loadForTarget(podcast, 128, target)
        }
    }

    private fun showPlayButton(playing: Boolean, views: RemoteViews) {
        views.setViewVisibility(R.id.widget_play_button, if (playing) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.widget_pause_button, if (playing) View.VISIBLE else View.GONE)
    }

    override fun updateWidgetNotPlaying() {
        updateWidget(null, false, playingEpisode = null)
    }

    private fun getOpenAppIntent(context: Context): PendingIntent {
        return context.getLaunchActivityPendingIntent()
    }

    private fun getPlayIntent(): PendingIntent? {
        return MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY)
    }

    private fun getPauseIntent(): PendingIntent? {
        return MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PAUSE)
    }

    private fun getSkipBackIntent(): PendingIntent? {
        return MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
    }

    private fun getSkipForwardIntent(): PendingIntent? {
        return MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
    }
}

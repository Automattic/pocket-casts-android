package au.com.shiftyjelly.pocketcasts.discover.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import au.com.shiftyjelly.pocketcasts.discover.R
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.repositories.images.into
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverEpisode
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeDrawable
import au.com.shiftyjelly.pocketcasts.ui.images.PodcastImageLoaderThemed
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.buttons.AnimatedPlayButton
import au.com.shiftyjelly.pocketcasts.views.extensions.setRippleBackground
import au.com.shiftyjelly.pocketcasts.views.extensions.showIf
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class EpisodeRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val imageLoader = PodcastImageLoaderThemed(context)
    private val podcastTitleView: TextView
    private val episodeTitleView: TextView
    private val durationView: TextView
    private val publishedView: TextView
    private val durationPublishedSeparator: TextView
    private val imageView: ImageView
    private val playButton: AnimatedPlayButton
    private val dateFormatter = RelativeDateFormatter(context)

    init {
        LayoutInflater.from(context).inflate(R.layout.row_episode, this, true)
        podcastTitleView = findViewById(R.id.podcastTitle)
        episodeTitleView = findViewById(R.id.episodeTitle)
        durationView = findViewById(R.id.duration)
        publishedView = findViewById(R.id.published)
        durationPublishedSeparator = findViewById(R.id.durationPublishedSeparator)
        imageView = findViewById(R.id.imageView)
        playButton = findViewById(R.id.playButton)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        val eightDp = 16.dpToPx(context)
        setPadding(eightDp, eightDp, eightDp, eightDp)
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        clipToPadding = false
        clipChildren = false

        setRippleBackground()
    }

    var episode: DiscoverEpisode? = null
        set(value) {
            field = value

            if (value == null) {
                clear()
            } else {
                podcastTitleView.text = value.podcast_title
                episodeTitleView.text = value.title

                val durationMs = (value.duration ?: 0) * 1000
                durationView.text = TimeHelper.getTimeDurationMediumString(durationMs, context)
                val durationVisible = durationMs > 0
                durationView.showIf(durationVisible)
                durationPublishedSeparator.showIf(durationVisible)

                val published = value.published
                publishedView.text = if (published == null) "" else dateFormatter.format(published)
                imageLoader.loadSmallImage(episode?.podcast_uuid).into(imageView)
                playButton.setPlaying(isPlaying = value.isPlaying, animate = false)
            }
        }

    var listTintColor: Int? = null
        set(value) {
            field = value

            if (value == null) {
                clear()
            } else {
                podcastTitleView.setTextColor(value)
                playButton.setCircleTintColor(value)
            }
        }

    var onPlayClicked: (() -> Unit)? = null
        set(value) {
            field = value
            playButton.setOnPlayClicked {
                value?.invoke()
            }
        }

    fun clear() {
        podcastTitleView.text = null
        episodeTitleView.text = null
        durationView.text = null
        publishedView.text = null
        imageView.setImageResource(imageView.context.getThemeDrawable(UR.attr.defaultArtworkSmall))
    }
}

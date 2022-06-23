package au.com.shiftyjelly.pocketcasts.player.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.databinding.ViewMiniPlayerBinding
import au.com.shiftyjelly.pocketcasts.repositories.images.into
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.images.PodcastImageLoaderThemed
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.model.KeyPath
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class MiniPlayer @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val binding = DataBindingUtil.inflate<ViewMiniPlayerBinding>(inflater, R.layout.view_mini_player, this, true)
    private var playing = false
    private var touchX: Float = 0f
    private var touchY: Float = 0f

    var clickListener: OnMiniPlayerClicked? = null

    init {
        // open full screen player on click
        binding.root.setOnClickListener { openPlayer() }
        binding.root.setOnTouchListener { _, event ->
            touchX = event.x
            touchY = event.y
            false
        }
        binding.root.setOnLongClickListener {
            clickListener?.onLongClick(touchX, touchY)
            true
        }
        // play / pause click
        binding.miniPlayButton.setOnClickListener { playClicked() }
        // skip clicks
        binding.skipBack.setOnClickListener { skipBackwardClicked() }
        binding.skipForward.setOnClickListener { skipForwardClicked() }
        // open Up Next
        binding.upNextButton.setOnClickListener { openUpNext() }

        updatePlayButtonFrame(false)

        contentDescription = "Mini player. Activate for more player options"
        setOnClickListener {
            if (Util.isTalkbackOn(context)) {
                openPlayer()
            }
        }
    }

    fun setPlaybackState(playbackState: PlaybackState) {
        binding.playbackState = playbackState
        // set the progress bar values as we need the max to be set before progress or the initial state doesn't work
        with(binding.progressBar) {
            max = playbackState.durationMs
            progress = playbackState.positionMs
            secondaryProgress = playbackState.bufferedMs
        }
        updatePlaying(nowPlaying = playbackState.isPlaying, animate = true)
    }

    fun getPlaybackState(): PlaybackState? {
        return binding.playbackState
    }

    private fun updateTintColor(tintColor: Int, theme: Theme) {
        binding.tintColor = ThemeColor.podcastIcon03(theme.activeTheme, tintColor)

        val colorStateList = ThemeColor.podcastUi02(theme.activeTheme, tintColor)
        binding.miniPlayerTint.setBackgroundColor(colorStateList)
        binding.countText.setTextColor(colorStateList)
        binding.miniPlayButton.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            SimpleColorFilter(colorStateList)
        }

        val progressColor = ThemeColor.playerHighlight01(theme.activeTheme, tintColor)
        val trackColor = ThemeColor.playerHighlight07(theme.activeTheme, tintColor)
        val bufferingColor = ThemeColor.playerHighlight07(theme.activeTheme, tintColor)
        binding.progressBar.apply {
            // play progress color
            progressTintList = ColorStateList.valueOf(progressColor)
            // buffering progress color
            secondaryProgressTintList = ColorStateList.valueOf(bufferingColor)
            // track color
            setBackgroundColor(trackColor)
        }
    }

    fun setUpNext(upNextState: UpNextQueue.State, theme: Theme) {
        if (upNextState is UpNextQueue.State.Loaded) {
            if (binding.episode?.uuid != upNextState.episode.uuid) {
                val imageLoader = PodcastImageLoaderThemed(context)
                imageLoader.radiusPx = 2.dpToPx(context.resources.displayMetrics)
                imageLoader.smallPlaceholder().load(upNextState.episode).into(binding.artwork)
            }

            binding.episode = upNextState.episode
            binding.podcast = upNextState.podcast

            val podcast = upNextState.podcast
            if (podcast != null) {
                updateTintColor(podcast.getMiniPlayerTintColor(theme.isDarkTheme), theme)
            } else {
                updateTintColor(context.getThemeColor(androidx.appcompat.R.attr.colorAccent), theme)
            }
        }

        val upNextCount: Int = upNextState.queueSize()
        binding.upNextCount = upNextCount

        val drawableId = when {
            upNextCount == 0 -> R.drawable.mini_player_upnext
            upNextCount < 10 -> R.drawable.mini_player_upnext_badge
            else -> R.drawable.mini_player_upnext_badge_large
        }
        val upNextDrawable: Drawable? = ContextCompat.getDrawable(context, drawableId)
        binding.upNextButton.setImageDrawable(upNextDrawable)
    }

    fun openUpNext() {
        clickListener?.onUpNextClicked()
    }

    private fun openPlayer() {
        clickListener?.onPlayerClicked()
    }

    private fun playClicked() {
        if (playing) {
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Pause clicked in mini player")
            clickListener?.onPauseClicked()
        } else {
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Play clicked in mini player")
            clickListener?.onPlayClicked()
        }
    }

    private fun skipBackwardClicked() {
        clickListener?.onSkipBackwardClicked()
    }

    private fun skipForwardClicked() {
        clickListener?.onSkipForwardClicked()
    }

    private fun updatePlaying(nowPlaying: Boolean, animate: Boolean) {
        if (playing == nowPlaying) {
            if ((binding.miniPlayButton.drawable as? LottieDrawable)?.isAnimating == false) {
                updatePlayButtonFrame(nowPlaying)
            }
            return
        }
        playing = nowPlaying
        if (animate) {
            animatePlayButton(playing)
        } else {
            updatePlayButtonFrame(nowPlaying)
        }
    }

    private fun animatePlayButton(isPlaying: Boolean) {
        val drawable = binding.miniPlayButton.drawable
        if (drawable is LottieDrawable) {
            if (isPlaying) {
                drawable.setMinAndMaxFrame(10, 20)
            } else {
                drawable.setMinAndMaxFrame(0, 10)
            }
            drawable.playAnimation()
            updatePlayButtonDescription(isPlaying)
        }
    }

    private fun updatePlayButtonDescription(isPlaying: Boolean) {
        binding.miniPlayButton.contentDescription = resources.getString(if (isPlaying) LR.string.pause else LR.string.play)
    }

    private fun updatePlayButtonFrame(isPlaying: Boolean) {
        binding.miniPlayButton.setMinAndMaxFrame(0, 20)
        binding.miniPlayButton.frame = if (isPlaying) 0 else 10
        updatePlayButtonDescription(isPlaying)
    }

    interface OnMiniPlayerClicked {
        fun onPlayerClicked()
        fun onUpNextClicked()
        fun onPlayClicked()
        fun onPauseClicked()
        fun onSkipBackwardClicked()
        fun onSkipForwardClicked()
        fun onLongClick(x: Float, y: Float)
    }
}

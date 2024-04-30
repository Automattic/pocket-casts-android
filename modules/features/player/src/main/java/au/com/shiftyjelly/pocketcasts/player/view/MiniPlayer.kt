package au.com.shiftyjelly.pocketcasts.player.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.databinding.ViewMiniPlayerBinding
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.ChromeCastAnalytics
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.loadInto
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.extensions.showIf
import au.com.shiftyjelly.pocketcasts.views.extensions.updateColor
import coil.request.Disposable
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.model.KeyPath
import com.google.android.gms.cast.framework.CastButtonFactory
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class MiniPlayer @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private val inflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val binding = ViewMiniPlayerBinding.inflate(
        inflater,
        this,
        true,
    )
    private var playing = false
    private val stringPause: String = context.resources.getString(LR.string.pause)
    private val stringPlay: String = context.resources.getString(LR.string.play)

    var clickListener: OnMiniPlayerClicked? = null

    @Inject
    lateinit var chromeCastAnalytics: ChromeCastAnalytics

    init {
        // open full screen player on click
        binding.root.setOnClickListener { openPlayer() }
        binding.root.setOnLongClickListener {
            if (binding.root.isClickable.not()) return@setOnLongClickListener false
            clickListener?.onLongClick()
            true
        }
        // play / pause click
        binding.miniPlayButton.setOnClickListener { playClicked() }
        // skip clicks
        binding.skipBack.setOnClickListener { skipBackwardClicked() }
        binding.skipForward.setOnClickListener { skipForwardClicked() }
        // open Up Next
        binding.upNextButton.setOnClickListener { openUpNext() }
        // cast button
        CastButtonFactory.setUpMediaRouteButton(context, binding.mediaRouteButton)
        binding.mediaRouteButton.setOnClickListener {
            chromeCastAnalytics.trackChromeCastViewShown()
        }
        binding.mediaRouteButton.showIf(FeatureFlag.isEnabled(Feature.UPNEXT_IN_TAB_BAR))

        setOnClickListener {
            if (Util.isTalkbackOn(context)) {
                openPlayer()
            }
        }
    }

    private val imageRequestFactory = PocketCastsImageRequestFactory(context, cornerRadius = 2).smallSize().themed()

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            updatePlayButton(isPlaying = playing, animate = false)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        return MiniPlayerState(superState = super.onSaveInstanceState(), isPlaying = playing)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val miniPlayerState = state as? MiniPlayerState
        super.onRestoreInstanceState(miniPlayerState?.superState)
        this.playing = miniPlayerState?.isPlaying ?: false
    }

    fun setPlaybackState(playbackState: PlaybackState) {
        // set the progress bar values as we need the max to be set before progress or the initial state doesn't work
        with(binding.progressBar) {
            max = playbackState.durationMs
            progress = playbackState.positionMs
            secondaryProgress = playbackState.bufferedMs
        }
        updatePlaying(isPlaying = playbackState.isPlaying)
    }

    private fun updateTintColor(tintColor: Int, theme: Theme) {
        val iconTintColor = ThemeColor.podcastIcon03(theme.activeTheme, tintColor)
        val tintColorStateList: ColorStateList =
            ColorStateList.valueOf(iconTintColor)

        binding.skipForward.imageTintList = tintColorStateList
        binding.miniPlayButton.backgroundTintList = tintColorStateList
        binding.skipBack.imageTintList = tintColorStateList
        binding.upNextButton.imageTintList = tintColorStateList
        binding.mediaRouteButton.updateColor(iconTintColor)

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

    fun setUpNext(upNextState: UpNextQueue.State, theme: Theme, useEpisodeArtwork: Boolean) {
        when (upNextState) {
            is UpNextQueue.State.Loaded -> {
                loadEpisodeArtwork(upNextState.episode, useEpisodeArtwork, binding.artwork)

                val podcast = upNextState.podcast
                if (podcast != null) {
                    updateTintColor(podcast.getPlayerTintColor(theme.isDarkTheme), theme)
                } else {
                    updateTintColor(context.getThemeColor(androidx.appcompat.R.attr.colorAccent), theme)
                }
                binding.nothingPlayingText.isVisible = false
                binding.skipBack.isVisible = true
                binding.skipForward.isVisible = true
                binding.miniPlayButton.isVisible = true
                binding.progressBar.isVisible = true
                binding.root.isClickable = true
                binding.root.isFocusable = true
            }

            is UpNextQueue.State.Empty -> {
                binding.artwork.setImageDrawable(null)
                binding.artwork.setBackgroundColor(ThemeColor.primaryUi05(theme.activeTheme))
                binding.miniPlayerTint.setBackgroundColor(ThemeColor.primaryUi01(theme.activeTheme))
                binding.nothingPlayingText.isVisible = true
                binding.skipBack.isVisible = false
                binding.skipForward.isVisible = false
                binding.miniPlayButton.isVisible = false
                binding.progressBar.isVisible = false
                binding.root.isClickable = false
                binding.root.isFocusable = false
            }
        }

        val upNextCount: Int = upNextState.queueSize()
        binding.countText.text = upNextCount.toString()
        binding.countText.isVisible = upNextCount > 0
        binding.upNextButton.showIf(
            !FeatureFlag.isEnabled(Feature.UPNEXT_IN_TAB_BAR) ||
                (upNextCount > 0),
        )

        val drawableId = when {
            upNextCount == 0 -> R.drawable.mini_player_upnext
            upNextCount < 10 -> R.drawable.mini_player_upnext_badge
            else -> R.drawable.mini_player_upnext_badge_large
        }
        val upNextDrawable: Drawable? = ContextCompat.getDrawable(context, drawableId)
        binding.upNextButton.setImageDrawable(upNextDrawable)
    }

    private fun openUpNext() {
        clickListener?.onUpNextClicked()
    }

    private fun openPlayer() {
        if (binding.root.isClickable.not()) return
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

    private fun updatePlaying(isPlaying: Boolean) {
        val drawable = binding.miniPlayButton.drawable as? LottieDrawable ?: return
        if (playing == isPlaying) {
            if (!drawable.isAnimating) {
                updatePlayButton(isPlaying = isPlaying, animate = false)
            }
        } else {
            playing = isPlaying
            updatePlayButton(isPlaying = playing, animate = true)
        }
    }

    private fun updatePlayButton(isPlaying: Boolean, animate: Boolean) {
        val button = binding.miniPlayButton
        val drawable = button.drawable as? LottieDrawable ?: return
        if (animate) {
            if (isPlaying) {
                drawable.setMinAndMaxFrame(10, 20)
            } else {
                drawable.setMinAndMaxFrame(0, 10)
            }
            drawable.playAnimation()
        } else {
            drawable.setMinAndMaxFrame(0, 20)
            drawable.frame = if (isPlaying) 0 else 10
        }
        button.contentDescription = if (isPlaying) stringPause else stringPlay
    }

    private var lastLoadedBaseEpisodeId: String? = null
    private var lastUseEpisodeArtwork: Boolean? = null

    private fun loadEpisodeArtwork(
        baseEpisode: BaseEpisode,
        useEpisodeArtwork: Boolean,
        imageView: ImageView,
    ): Disposable? {
        if (lastLoadedBaseEpisodeId == baseEpisode.uuid && lastUseEpisodeArtwork == useEpisodeArtwork && imageView.drawable != null) {
            return null
        }

        lastLoadedBaseEpisodeId = baseEpisode.uuid
        lastUseEpisodeArtwork = useEpisodeArtwork
        return imageRequestFactory.create(baseEpisode, useEpisodeArtwork).loadInto(imageView)
    }

    interface OnMiniPlayerClicked {
        fun onPlayerClicked()
        fun onUpNextClicked()
        fun onPlayClicked()
        fun onPauseClicked()
        fun onSkipBackwardClicked()
        fun onSkipForwardClicked()
        fun onLongClick()
    }

    @Parcelize
    private class MiniPlayerState(val superState: Parcelable?, val isPlaying: Boolean) : Parcelable
}

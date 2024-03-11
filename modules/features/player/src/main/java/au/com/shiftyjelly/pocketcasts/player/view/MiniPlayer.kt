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
import androidx.databinding.DataBindingUtil
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.databinding.ViewMiniPlayerBinding
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getUrlForArtwork
import au.com.shiftyjelly.pocketcasts.repositories.images.into
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.images.PodcastImageLoaderThemed
import au.com.shiftyjelly.pocketcasts.ui.images.ThemedImageTintTransformation
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import coil.load
import coil.request.Disposable
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.size.Scale
import coil.transform.RoundedCornersTransformation
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.model.KeyPath
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class MiniPlayer @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private val inflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val binding = DataBindingUtil.inflate<ViewMiniPlayerBinding>(
        inflater,
        R.layout.view_mini_player,
        this,
        true,
    )
    private var playing = false
    private val stringPause: String = context.resources.getString(LR.string.pause)
    private val stringPlay: String = context.resources.getString(LR.string.play)

    var clickListener: OnMiniPlayerClicked? = null

    init {
        // open full screen player on click
        binding.root.setOnClickListener { openPlayer() }
        binding.root.setOnLongClickListener {
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

        setOnClickListener {
            if (Util.isTalkbackOn(context)) {
                openPlayer()
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        updatePlayButton(isPlaying = playing, animate = false)
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
        val tintColorStateList: ColorStateList =
            ColorStateList.valueOf(ThemeColor.podcastIcon03(theme.activeTheme, tintColor))

        binding.skipForward.imageTintList = tintColorStateList
        binding.miniPlayButton.backgroundTintList = tintColorStateList
        binding.skipBack.imageTintList = tintColorStateList
        binding.upNextButton.imageTintList = tintColorStateList

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

    fun setUpNext(upNextState: UpNextQueue.State, theme: Theme, useRssArtwork: Boolean) {
        if (upNextState is UpNextQueue.State.Loaded) {
            loadArtwork(upNextState.podcast, upNextState.episode, useRssArtwork)

            val podcast = upNextState.podcast
            if (podcast != null) {
                updateTintColor(podcast.getPlayerTintColor(theme.isDarkTheme), theme)
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

    private fun openUpNext() {
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

    private fun loadArtwork(podcast: Podcast?, episode: BaseEpisode, useRssArtwork: Boolean) {
        val imageLoader = PodcastImageLoaderThemed(context)
        val imageView = binding.artwork
        imageLoader.radiusPx = 2.dpToPx(context.resources.displayMetrics)

        val artwork = getEpisodeArtwork(episode, useRssArtwork)
        if (artwork == Artwork.None && podcast?.uuid != null) {
            loadPodcastArtwork(imageLoader, podcast)
        } else {
            loadEpisodeArtwork(artwork, imageView, imageLoader)?.let { disposable ->
                launch {
                    // If episode artwork fails to load, then load podcast artwork
                    val result = disposable.job.await()
                    if (result is ErrorResult && podcast?.uuid != null) {
                        loadPodcastArtwork(imageLoader, podcast)
                    }
                }
            }
        }
    }

    private var loadedPodcastUuid: String? = null
    private fun loadPodcastArtwork(
        imageLoader: PodcastImageLoaderThemed,
        podcast: Podcast,
    ) {
        if (loadedPodcastUuid == podcast.uuid) return
        val imageView = binding.artwork
        imageLoader.smallPlaceholder().loadPodcastUuid(podcast.uuid).into(imageView)
        loadedPodcastUuid = podcast.uuid
        loadedEpisodeArtwork = null
    }

    private var loadedEpisodeArtwork: Artwork? = null
    private fun loadEpisodeArtwork(
        artwork: Artwork,
        imageView: ImageView,
        imageLoader: PodcastImageLoaderThemed,
    ): Disposable? {
        if (artwork is Artwork.None || loadedEpisodeArtwork == artwork) return null
        imageView.imageTintList = null

        val imageBuilder: ImageRequest.Builder.() -> Unit = {
            error(au.com.shiftyjelly.pocketcasts.images.R.drawable.defaultartwork_dark)
            scale(Scale.FIT)
            transformations(
                RoundedCornersTransformation(imageLoader.radiusPx.toFloat()),
                ThemedImageTintTransformation(imageView.context),
            )
        }
        loadedEpisodeArtwork = artwork
        loadedPodcastUuid = null
        return when (artwork) {
            is Artwork.Path -> {
                imageView.load(data = File(artwork.path), builder = imageBuilder)
            }
            is Artwork.Url -> {
                imageView.load(data = artwork.url, builder = imageBuilder)
            }
            else -> {
                null
            }
        }
    }

    companion object {
        private fun getEpisodeArtwork(episode: BaseEpisode, useRssArtwork: Boolean): Artwork {
            val showNotesImageUrl = (episode as? PodcastEpisode)?.imageUrl
            return if (showNotesImageUrl != null && useRssArtwork) {
                Artwork.Url(showNotesImageUrl)
            } else if (episode is UserEpisode) {
                val artworkUrl = episode.getUrlForArtwork(themeIsDark = true)
                if (artworkUrl.startsWith("/")) {
                    Artwork.Path(artworkUrl)
                } else {
                    Artwork.Url(artworkUrl)
                }
            } else {
                Artwork.None
            }
        }
    }

    sealed class Artwork {
        data class Url(val url: String) : Artwork()
        data class Path(val path: String) : Artwork()
        object None : Artwork()
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

package au.com.shiftyjelly.pocketcasts.player.view.video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.player.binding.setPlaybackState
import au.com.shiftyjelly.pocketcasts.player.databinding.FragmentVideoBinding
import au.com.shiftyjelly.pocketcasts.player.view.PlayerSeekBar
import au.com.shiftyjelly.pocketcasts.player.viewmodel.VideoViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.SimplePlayer
import au.com.shiftyjelly.pocketcasts.views.extensions.hide
import au.com.shiftyjelly.pocketcasts.views.extensions.show
import au.com.shiftyjelly.pocketcasts.views.helper.toCircle
import com.airbnb.lottie.LottieAnimationView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.time.Duration
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class VideoFragment :
    Fragment(),
    PlayerSeekBar.OnUserSeekListener {

    @Inject lateinit var playbackManager: PlaybackManager

    @Inject lateinit var settings: Settings

    private val viewModel: VideoViewModel by viewModels()
    private var binding: FragmentVideoBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentVideoBinding.inflate(inflater, container, false)
        this.binding = binding

        // video under the status and navigation bar
        setupSystemUi()

        // listen for taps causing the system ui to come back
        listenForSystemUiChanges()

        val context = binding.videoView.context

        binding.touchView.setOnClickListener { viewModel.toggleControls() }
        binding.skipBack.setOnClickListener { view ->
            val button = view as LottieAnimationView
            viewModel.skipBackward()
            button.playAnimation()
        }
        binding.skipForward.setOnClickListener { view ->
            val button = view as LottieAnimationView
            viewModel.skipForward()
            button.playAnimation()
        }
        binding.playButton.setPlaying(viewModel.isPlaying(), false)
        binding.playButton.setOnPlayClicked {
            val isPlaying = viewModel.isPlaying()
            viewModel.playPause()
            binding.playButton.setPlaying(!isPlaying, animate = true)
        }
        binding.seekBar.changeListener = this
        binding.toolbar.setNavigationOnClickListener { activity?.finish() }

        binding.skipBackText.text = "${settings.skipBackInSecs.value}"
        binding.jumpForwardText.text = "${settings.skipForwardInSecs.value}"

        binding.playButton.setCircleTintColor(ContextCompat.getColor(context, UR.color.transparent))

        binding.btnPip.setOnClickListener {
            activity?.let {
                binding.controlsPanel.hide()
                val intent = VideoActivity.buildIntent(enterPictureInPicture = true, context = it)
                it.startActivity(intent)
            }
        }
        binding.skipBack.toCircle(true)
        binding.skipForward.toCircle(true)

        viewModel.playbackState.observe(viewLifecycleOwner) { playbackState ->
            val newPlayer = (playbackManager.player as? SimplePlayer)?.exoPlayer

            // setPlayer returns straight away if the player is the same so calling this too much doesn't matter.
            // This ensures while the full screen player is visible, the surface isn't set from somewhere else causing
            // this player to appear blank.
            binding.videoView.player = newPlayer
            binding.seekBar.setPlaybackState(playbackState)
        }

        viewModel.controlsVisible.observe(viewLifecycleOwner) { visible ->
            if (visible) {
                binding.controlsPanel.show()
                showSystemUi()
            } else {
                binding.controlsPanel.hide()
                hideSystemUi()
            }
        }

        return binding.root
    }

    override fun onDetach() {
        super.onDetach()
        binding?.let {
            it.root.setOnApplyWindowInsetsListener(null)
            it.touchView.setOnClickListener(null)
            it.seekBar.changeListener = null
            it.videoView.player = null
        }
        binding = null
    }

    private fun listenForSystemUiChanges() {
        val view = binding?.root ?: return

        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            if (insets.isVisible(WindowInsetsCompat.Type.statusBars())) {
                viewModel.showControls()
            } else {
                viewModel.hideControls()
            }
            insets
        }
    }

    private fun setupSystemUi() {
        val window = activity?.window ?: return
        WindowCompat.setDecorFitsSystemWindows(window, false)
        getInsetsController()?.hide(WindowInsetsCompat.Type.navigationBars())
    }

    private fun hideSystemUi() {
        getInsetsController()?.hide(WindowInsetsCompat.Type.statusBars())
        getInsetsController()?.hide(WindowInsetsCompat.Type.navigationBars())
    }

    private fun showSystemUi() {
        getInsetsController()?.show(WindowInsetsCompat.Type.statusBars())
        getInsetsController()?.show(WindowInsetsCompat.Type.navigationBars())
    }

    private fun getInsetsController(): WindowInsetsControllerCompat? {
        val window = activity?.window ?: return null
        val view = binding?.root ?: return null
        return WindowCompat.getInsetsController(window, view)
    }

    override fun onSeekPositionChangeStart() {
        viewModel.seekStarted()
    }

    override fun onSeekPositionChanging(progress: Duration) {
    }

    override fun onSeekPositionChangeStop(progress: Duration, seekComplete: () -> Unit) {
        val progressMs = progress.inWholeMilliseconds.toInt()
        viewModel.seekToMs(progressMs)
        playbackManager.trackPlaybackSeek(progressMs, SourceView.FULL_SCREEN_VIDEO)
        seekComplete()
    }
}

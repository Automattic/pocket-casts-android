package au.com.shiftyjelly.pocketcasts.player.view.video

import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Rational
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media.session.MediaButtonReceiver
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager.PlaybackSource
import au.com.shiftyjelly.pocketcasts.repositories.playback.SimplePlayer
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class VideoActivity : AppCompatActivity() {

    @Inject lateinit var settings: Settings
    @Inject lateinit var playbackManager: PlaybackManager
    @Inject lateinit var theme: Theme

    private var pipActionsPlaying: List<RemoteAction>? = null
    private var pipActionsPaused: List<RemoteAction>? = null
    private var wasInPiP: Boolean = false

    companion object {
        const val EXTRA_PIP = "EXTRA_PIP"

        fun buildIntent(enterPictureInPicture: Boolean = false, context: Context): Intent {
            return Intent(context, VideoActivity::class.java).apply {
                putExtra(EXTRA_PIP, enterPictureInPicture)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(Theme.ThemeType.EXTRA_DARK.resourceId)

        val color = ContextCompat.getColor(this, R.color.videoButtonBackground)

        window.statusBarColor = color
        window.navigationBarColor = color

        setContentView(R.layout.activity_video)

        playbackManager.playbackSource = PlaybackSource.FULL_SCREEN_VIDEO

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, VideoFragment())
                .commitNow()

            // Enter Picture-in-Picture
            if (intent.getBooleanExtra(EXTRA_PIP, false)) {
                enterPictureInPicture()
            } else {
                playbackManager.player?.isPip = false
            }
        }

        playbackManager.playbackStateLive.observe(this) { playbackState ->
            val currentEpisode = playbackManager.upNextQueue.currentEpisode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                updatePictureInPictureActions(playbackState.isPlaying)
            }
            if (currentEpisode == null || !currentEpisode.isVideo) {
                finish()
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        playbackManager.player?.isPip = isInPictureInPictureMode
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        // Enter Picture-in-Picture
        if (intent?.getBooleanExtra(EXTRA_PIP, false) == true) {
            enterPictureInPicture()
        } else {
            playbackManager.player?.isPip = false
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (newConfig.orientation != Configuration.ORIENTATION_LANDSCAPE && !wasInPiP) {
            finish()
        }
    }

    private fun enterPictureInPicture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        wasInPiP = true
        playbackManager.player?.isPip = true

        // create PiP actions
        if (pipActionsPlaying == null) {
            val skipBackward = createRemoteAction(IR.drawable.notification_skipbackwards, LR.string.skip_back, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
            val play = createRemoteAction(IR.drawable.notification_play, LR.string.play, PlaybackStateCompat.ACTION_PLAY)
            val pause = createRemoteAction(IR.drawable.notification_pause, LR.string.pause, PlaybackStateCompat.ACTION_PAUSE)
            val skipForward = createRemoteAction(IR.drawable.notification_skipforward, LR.string.skip_forward, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)

            pipActionsPlaying = listOf(skipBackward, pause, skipForward)
            pipActionsPaused = listOf(skipBackward, play, skipForward)
        }

        val player = playbackManager.player as? SimplePlayer ?: return
        val width = player.videoWidth
        val height = player.videoHeight
        val aspectRatio = if (width == 0 || height == 0) Rational(16, 9) else Rational(width, height)
        val params = PictureInPictureParams.Builder().setAspectRatio(aspectRatio)
        enterPictureInPictureMode(params.build())
        updatePictureInPictureActions(isPlaying = playbackManager.isPlaying())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updatePictureInPictureActions(isPlaying: Boolean) {
        val actions = if (isPlaying) pipActionsPlaying else pipActionsPaused
        val params = PictureInPictureParams.Builder().setActions(actions)
        setPictureInPictureParams(params.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createRemoteAction(drawableId: Int, titleId: Int, playbackState: Long): RemoteAction {
        val title = this.resources.getString(titleId)
        val intent = MediaButtonReceiver.buildMediaButtonPendingIntent(application, playbackState)
        return RemoteAction(Icon.createWithResource(this, drawableId), title, title, intent)
    }
}

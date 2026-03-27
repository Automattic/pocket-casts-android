package au.com.shiftyjelly.pocketcasts.player.view.video

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
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
        private const val ACTION_PIP_SKIP_BACK = "au.com.shiftyjelly.pocketcasts.PIP_SKIP_BACK"
        private const val ACTION_PIP_PLAY = "au.com.shiftyjelly.pocketcasts.PIP_PLAY"
        private const val ACTION_PIP_PAUSE = "au.com.shiftyjelly.pocketcasts.PIP_PAUSE"
        private const val ACTION_PIP_SKIP_FORWARD = "au.com.shiftyjelly.pocketcasts.PIP_SKIP_FORWARD"

        fun buildIntent(enterPictureInPicture: Boolean = false, context: Context): Intent {
            return Intent(context, VideoActivity::class.java).apply {
                putExtra(EXTRA_PIP, enterPictureInPicture)
            }
        }
    }

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_PIP_SKIP_BACK -> playbackManager.skipBackward(sourceView = SourceView.MEDIA_BUTTON_BROADCAST_ACTION)
                ACTION_PIP_PLAY -> playbackManager.playQueue(sourceView = SourceView.MEDIA_BUTTON_BROADCAST_ACTION)
                ACTION_PIP_PAUSE -> playbackManager.pause(sourceView = SourceView.MEDIA_BUTTON_BROADCAST_ACTION)
                ACTION_PIP_SKIP_FORWARD -> playbackManager.skipForward(sourceView = SourceView.MEDIA_BUTTON_BROADCAST_ACTION)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter().apply {
            addAction(ACTION_PIP_SKIP_BACK)
            addAction(ACTION_PIP_PLAY)
            addAction(ACTION_PIP_PAUSE)
            addAction(ACTION_PIP_SKIP_FORWARD)
        }
        ContextCompat.registerReceiver(this, pipReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        setTheme(Theme.ThemeType.EXTRA_DARK.resourceId)
        setContentView(R.layout.activity_video)

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

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(pipReceiver)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        playbackManager.player?.isPip = isInPictureInPictureMode
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Enter Picture-in-Picture
        if (intent.getBooleanExtra(EXTRA_PIP, false)) {
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
            val skipBackward = createRemoteAction(IR.drawable.notification_skipbackwards, LR.string.skip_back, ACTION_PIP_SKIP_BACK, 0)
            val play = createRemoteAction(IR.drawable.notification_play, LR.string.play, ACTION_PIP_PLAY, 1)
            val pause = createRemoteAction(IR.drawable.notification_pause, LR.string.pause, ACTION_PIP_PAUSE, 2)
            val skipForward = createRemoteAction(IR.drawable.notification_skipforward, LR.string.skip_forward, ACTION_PIP_SKIP_FORWARD, 3)

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
    private fun createRemoteAction(drawableId: Int, titleId: Int, action: String, requestCode: Int): RemoteAction {
        val title = resources.getString(titleId)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            Intent(action).setPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return RemoteAction(Icon.createWithResource(this, drawableId), title, title, pendingIntent)
    }
}

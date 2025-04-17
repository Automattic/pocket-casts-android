package au.com.shiftyjelly.pocketcasts.player.view.video

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.SimplePlayer
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer

class VideoView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr), SurfaceHolder.Callback, SimplePlayer.VideoChangedListener {
    companion object {
        private const val DELAY_MS = 300L
        private const val TAG = "VideoView"
    }

    var playbackManager: PlaybackManager? = null
    var show: Boolean = false
        set(value) {
            field = value
            connectWithDelay()
        }

    private val view = LayoutInflater.from(context).inflate(R.layout.video_view, this, true)

    @UnstableApi
    private val aspectRatioLayout = view.findViewById<AspectRatioFrameLayout>(R.id.aspectRatioLayout).apply {
        setAspectRatio(1.78f)
    }
    private val surfaceView = view.findViewById<SurfaceView>(R.id.surfaceView)
    private var isSurfaceCreated: Boolean = false
    private var isSurfaceSet: Boolean = false
    private var pendingConnection = false

    init {
        surfaceView.holder.addCallback(this)
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (visibility == GONE) {
            isSurfaceSet = false
            (playbackManager?.player as? SimplePlayer)?.setDisplay(null)
        }
    }

    fun connect() {
        if (!isSurfaceCreated || isSurfaceSet) {
            return
        }

        if (!show) {
            return
        }

        val player = playbackManager?.player
        if (player == null || !player.supportsVideo() || player.isRemote || player.isPip) {
            return
        }

        if (player is SimplePlayer) {
            player.setVideoSizeChangedListener(this)
            if (player.setDisplay(surfaceView)) {
                isSurfaceSet = true
            }
        }
    }

    @UnstableApi
    override fun videoSizeChanged(width: Int, height: Int, pixelWidthHeightRatio: Float) {
        val videoAspectRatio = if (height == 0 || width == 0) 1f else width * pixelWidthHeightRatio / height
        aspectRatioLayout.setAspectRatio(videoAspectRatio)
    }

    override fun videoNeedsReset() {
        isSurfaceSet = false
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isSurfaceCreated = true
        isSurfaceSet = false
        connectWithDelay()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isSurfaceCreated = false
        isSurfaceSet = false
        pendingConnection = false // Cancel any pending connection
    }

    fun updatePlayerPrepared(prepared: Boolean) {
        if (prepared && !isSurfaceSet) {
            connectWithDelay()
        }
    }

    private fun connectWithDelay() {
        pendingConnection = true
        // Temporary fix for https://github.com/Automattic/pocket-casts-android/issues/3807
        // The delay gives time for the Activity/Fragment transition to complete and
        // prevents the race condition where surface gets destroyed immediately after creation

        postDelayed({
            if (pendingConnection) {
                try {
                    connect()
                } catch (e: Exception) {
                    LogBuffer.e(TAG, "Failed to connect video surface", e)
                }
            }
            pendingConnection = false
        }, DELAY_MS)
    }
}

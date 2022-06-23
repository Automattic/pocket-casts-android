package au.com.shiftyjelly.pocketcasts.player.view.video

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.SimplePlayer
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout

class VideoView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr), SurfaceHolder.Callback, SimplePlayer.VideoChangedListener {

    var playbackManager: PlaybackManager? = null
    var show: Boolean = false
        set(value) {
            field = value
            connect()
        }

    private val view = LayoutInflater.from(context).inflate(R.layout.video_view, this, true)
    private val aspectRatioLayout = view.findViewById<AspectRatioFrameLayout>(R.id.aspectRatioLayout).apply {
        setAspectRatio(1.78f)
    }
    private val surfaceView = view.findViewById<SurfaceView>(R.id.surfaceView)
    private var isSurfaceCreated: Boolean = false
    private var isSurfaceSet: Boolean = false

    init {
        surfaceView.holder.addCallback(this)
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (visibility == View.GONE) {
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
        connect()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isSurfaceCreated = false
        isSurfaceSet = false
    }

    fun updatePlayerPrepared(prepared: Boolean) {
        if (prepared && !isSurfaceSet) {
            connect()
        }
    }
}

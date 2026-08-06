package au.com.shiftyjelly.pocketcasts.nowplaying

import android.content.Context
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import au.com.shiftyjelly.pocketcasts.repositories.playback.Player
import au.com.shiftyjelly.pocketcasts.repositories.playback.SimplePlayer
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer

@Composable
fun TvVideoSurface(
    player: Player?,
    modifier: Modifier = Modifier,
) {
    var aspectRatio by remember { mutableFloatStateOf(DEFAULT_ASPECT_RATIO) }
    AndroidView(
        factory = { context -> TvVideoView(context) },
        update = { view ->
            view.onAspectRatioChanged = { ratio -> aspectRatio = ratio }
            view.player = player
            view.connectWithDelay()
        },
        onRelease = { view -> view.releaseSurface() },
        modifier = modifier.aspectRatio(aspectRatio),
    )
}

private const val DEFAULT_ASPECT_RATIO = 1.78f

// The surface handling mirrors the mobile player's VideoView. That view lives in the player
// feature module whose Hilt graph cannot be linked into the TV app, hence this local copy.
private class TvVideoView(
    context: Context,
) : FrameLayout(context),
    SurfaceHolder.Callback,
    SimplePlayer.VideoChangedListener {

    var player: Player? = null
    var onAspectRatioChanged: ((Float) -> Unit)? = null

    private var isSurfaceCreated = false
    private var isSurfaceConnectionPending = false
    private var isSurfaceConnected = false

    private val surfaceView = SurfaceView(context).also { view ->
        addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        view.holder.addCallback(this)
    }

    override fun onDetachedFromWindow() {
        releaseSurface()
        super.onDetachedFromWindow()
    }

    fun releaseSurface() {
        (player as? SimplePlayer)?.setDisplay(null)
        isSurfaceConnected = false
        isSurfaceConnectionPending = false
    }

    fun connectWithDelay() {
        if (isSurfaceConnected || isSurfaceConnectionPending) {
            return
        }
        isSurfaceConnectionPending = true

        // The delay prevents the race condition where the surface gets destroyed immediately
        // after creation while a screen transition is still running.
        postDelayed({
            if (isSurfaceConnectionPending) {
                try {
                    connect()
                } catch (e: Exception) {
                    LogBuffer.e(TAG, "Failed to connect video surface", e)
                }
            }
            isSurfaceConnectionPending = false
        }, SURFACE_CONNECT_DELAY_MS)
    }

    private fun connect() {
        if (!isSurfaceCreated || isSurfaceConnected || !isSurfaceConnectionPending) {
            return
        }

        val player = this.player as? SimplePlayer
        if (player == null || !player.supportsVideo() || player.isRemote || player.isPip) {
            return
        }

        player.setVideoSizeChangedListener(this)
        if (player.setDisplay(surfaceView)) {
            isSurfaceConnected = true
        }
    }

    override fun videoSizeChanged(width: Int, height: Int, pixelWidthHeightRatio: Float) {
        val videoAspectRatio = if (height == 0 || width == 0) 1f else width * pixelWidthHeightRatio / height
        onAspectRatioChanged?.invoke(videoAspectRatio)
    }

    override fun videoNeedsReset() {
        isSurfaceConnected = false
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isSurfaceCreated = true
        isSurfaceConnected = false
        connectWithDelay()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isSurfaceCreated = false
        isSurfaceConnected = false
        isSurfaceConnectionPending = false
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    companion object {
        private const val SURFACE_CONNECT_DELAY_MS = 300L
        private const val TAG = "TvVideoView"
    }
}

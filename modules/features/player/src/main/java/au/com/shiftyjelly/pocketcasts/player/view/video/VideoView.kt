@file:Suppress("OPT_IN_USAGE")

package au.com.shiftyjelly.pocketcasts.player.view.video

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import au.com.shiftyjelly.pocketcasts.player.databinding.VideoViewBinding
import au.com.shiftyjelly.pocketcasts.repositories.playback.Player
import au.com.shiftyjelly.pocketcasts.repositories.playback.SimplePlayer
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer

@OptIn(UnstableApi::class)
class VideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr),
    SurfaceHolder.Callback,
    SimplePlayer.VideoChangedListener {
    var player: Player? = null
    private var isSurfaceCreated = false
    private var isSurfaceConnectionPending = false
    private var isSurfaceConnected = false

    private val binding = VideoViewBinding.inflate(LayoutInflater.from(context), this, true).apply {
        aspectRatioLayout.setAspectRatio(1.78f)
        surfaceView.holder.addCallback(this@VideoView)
    }

    fun addOnAspectRatioListener(listener: (Float) -> Unit) {
        binding.aspectRatioLayout.setAspectRatioListener { targetAspectRatio, _, _ ->
            listener(targetAspectRatio)
        }
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (visibility == GONE) {
            isSurfaceConnected = false
            (player as? SimplePlayer)?.setDisplay(null)
        }
    }

    fun connectWithDelay() {
        if (isSurfaceConnected) {
            return
        }
        isSurfaceConnectionPending = true

        // Temporary fix for https://github.com/Automattic/pocket-casts-android/issues/3807
        // The delay gives time for the Activity/Fragment transition to complete and
        // prevents the race condition where surface gets destroyed immediately after creation
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
        if (player.setDisplay(binding.surfaceView)) {
            isSurfaceConnected = true
        }
    }

    override fun videoSizeChanged(width: Int, height: Int, pixelWidthHeightRatio: Float) {
        val videoAspectRatio = if (height == 0 || width == 0) 1f else width * pixelWidthHeightRatio / height
        binding.aspectRatioLayout.setAspectRatio(videoAspectRatio)
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
        private const val TAG = "VideoView"
    }
}

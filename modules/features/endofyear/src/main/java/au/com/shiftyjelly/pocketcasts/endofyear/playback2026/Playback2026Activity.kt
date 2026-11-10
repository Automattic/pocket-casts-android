package au.com.shiftyjelly.pocketcasts.endofyear.playback2026

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import com.automattic.pocketcasts.playback.PlaybackController
import com.automattic.pocketcasts.playback.PlaybackEvent
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Hosts the Playback experience full screen and turns its events into app behavior.
 *
 * The story opens before its data is there: [Playback2026ViewModel] syncs and reads the listening
 * history while the intro plays, and the payload is handed to the page when it is ready. If the
 * user moves on from the intro first, the page shows a spinner until then.
 *
 * The manifest declares `configChanges` for this activity: a rotation, font size or dark mode change
 * would otherwise recreate it, and with it the web view and the user's place in the story.
 */
@AndroidEntryPoint
class Playback2026Activity : ComponentActivity() {
    private val viewModel by viewModels<Playback2026ViewModel>()

    private lateinit var container: FrameLayout
    private var controller: PlaybackController? = null
    private var insets: Insets? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The story is laid out for a portrait frame without scrolling, so phones stay in portrait.
        // Tablets and foldables are tall enough either way.
        if (resources.configuration.smallestScreenWidthDp < 600) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        // The story is drawn edge to edge on a dark ground, and lays out around the insets it is given.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        container = FrameLayout(this).apply { setBackgroundColor(BACKGROUND_COLOR) }
        setContentView(container)

        // Android web views report env(safe-area-inset-*) as zero, so measure and push.
        ViewCompat.setOnApplyWindowInsetsListener(container) { _, windowInsets ->
            insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            applyInsets()
            windowInsets
        }

        // The system back gesture steps a page back, and only leaves when nothing is behind it.
        onBackPressedDispatcher.addCallback(this) {
            val controller = controller
            if (controller == null) {
                finish()
            } else {
                controller.handleBack { consumed -> if (!consumed) finish() }
            }
        }

        show()
        lifecycleScope.launch {
            val payload = viewModel.payload.filterNotNull().first()
            controller?.setPayload(payload)
        }
    }

    /** Opens the story at once, without its payload: the intro plays while the view model reads it. */
    private fun show() {
        val controller = try {
            PlaybackController(this)
        } catch (e: Exception) {
            // The web view can fail to start, for example while the system WebView is being updated.
            Timber.e(e, "Playback 2026: failed to start")
            finish()
            return
        }
        controller.onEvent = ::handle
        controller.onProcessGone = {
            Timber.w("Playback 2026: web view process gone")
            finish()
        }
        this.controller = controller

        container.addView(
            controller.webView,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        applyInsets()
        controller.load()
    }

    private fun applyInsets() {
        val insets = insets ?: return
        controller?.setInsets(insets.top, insets.right, insets.bottom, insets.left)
    }

    private fun handle(event: PlaybackEvent) {
        when (event) {
            is PlaybackEvent.Close -> finish()
            is PlaybackEvent.Share -> share()
            is PlaybackEvent.Error -> Timber.e("Playback 2026: payload rejected: ${event.messages}")
            is PlaybackEvent.Warning -> Timber.w("Playback 2026: ${event.message}")
            else -> Unit
        }
    }

    /**
     * The page asks to share: take the 1080 × 1920 image the page lays out and present the system share sheet.
     */
    private fun share() {
        val controller = controller ?: return
        controller.snapshotPng { png ->
            if (png == null) {
                Timber.w("Playback 2026: share failed, no snapshot")
                return@snapshotPng
            }
            lifecycleScope.launch {
                val uri = withContext(Dispatchers.IO) {
                    val file = File(File(cacheDir, SHARE_DIRECTORY).apply { mkdirs() }, SHARE_FILE_NAME)
                    file.writeBytes(png)
                    FileUtil.getUriForFile(this@Playback2026Activity, file)
                }
                val intent = Intent(Intent.ACTION_SEND)
                    .setType("image/png")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .apply { clipData = ClipData.newRawUri(null, uri) }
                startActivity(Intent.createChooser(intent, null))
            }
        }
    }

    // `fontScale` and `uiMode` are in the manifest's configChanges: the page is told, and takes
    // what it supports of the new text size, bold text and contrast.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        controller?.updateAccessibility()
    }

    override fun onDestroy() {
        controller?.destroy()
        controller = null
        super.onDestroy()
    }

    companion object {
        // The design's ground, so nothing flashes before the page's first frame.
        private val BACKGROUND_COLOR = Color.rgb(0x2D, 0x00, 0x04)
        private const val SHARE_DIRECTORY = "playback_2026"
        private const val SHARE_FILE_NAME = "playback-2026.png"

        fun newIntent(context: Context) = Intent(context, Playback2026Activity::class.java)
    }
}

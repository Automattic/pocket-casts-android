package au.com.shiftyjelly.pocketcasts.views.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.preferences.BuildConfig
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

private const val EXTRA_TITLE = "EXTRA_TITLE"
private const val EXTRA_URL = "EXTRA_URL"

@AndroidEntryPoint
class WebViewActivity :
    AppCompatActivity(),
    CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    @Inject lateinit var settings: Settings

    private var isLoading by mutableStateOf(false)

    private val extraUrl: String?
        get() = intent.extras?.getString(EXTRA_URL)

    companion object {
        fun newInstance(context: Context, title: String, url: String): Intent {
            val intent = Intent(context, WebViewActivity::class.java)
            intent.putExtra(EXTRA_TITLE, title)
            intent.putExtra(EXTRA_URL, url)
            return intent
        }

        fun show(context: Context?, title: String, url: String) {
            if (context == null) return
            val intent = newInstance(context, title, url)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialUrl = if (savedInstanceState == null) extraUrl else null
        val composeView = ComposeView(this).apply {
            setContent {
                AppTheme(Theme.ThemeType.DARK) {
                    WebViewPage(
                        title = intent.extras?.getString(EXTRA_TITLE).orEmpty(),
                        isLoading = isLoading,
                        onNavigationClick = ::finish,
                        onCreateWebView = { context -> createWebView(context, initialUrl) },
                    )
                }
            }
        }
        setContentView(composeView)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(context: Context, initialUrl: String?): WebView {
        return WebView(context).apply {
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    isLoading = true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    isLoading = false
                }

                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    return when {
                        uri.host == BuildConfig.WEB_BASE_HOST -> false
                        uri.toString() == extraUrl -> false
                        else -> runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }.isSuccess
                    }
                }
            }

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

            initialUrl?.let(::loadUrl)
        }
    }
}

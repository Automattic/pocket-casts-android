package au.com.shiftyjelly.pocketcasts.views.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import au.com.shiftyjelly.pocketcasts.preferences.BuildConfig
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.views.databinding.ActivityWebViewBinding
import au.com.shiftyjelly.pocketcasts.views.extensions.includeStatusBarPadding
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

    private lateinit var binding: ActivityWebViewBinding

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

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWebViewBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.toolbar.title = intent.extras?.getString(EXTRA_TITLE)
        binding.toolbar.includeStatusBarPadding()
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(false) {
                override fun handleOnBackPressed() {
                    if (binding.webview.canGoBack()) {
                        binding.webview.goBack()
                    } else {
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            },
        )

        binding.webview.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.loading.isVisible = true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.loading.isVisible = false
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

        binding.webview.settings.javaScriptEnabled = true
        binding.webview.settings.domStorageEnabled = true

        if (savedInstanceState == null) {
            extraUrl?.let { url ->
                binding.webview.loadUrl(url)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

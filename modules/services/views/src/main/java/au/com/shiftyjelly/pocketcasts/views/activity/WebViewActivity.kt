package au.com.shiftyjelly.pocketcasts.views.activity

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import au.com.shiftyjelly.pocketcasts.localization.BuildConfig
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.views.databinding.ActivityWebViewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

private const val EXTRA_TITLE = "EXTRA_TITLE"
private const val EXTRA_URL = "EXTRA_URL"

@AndroidEntryPoint
class WebViewActivity : AppCompatActivity(), CoroutineScope {
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

        val INTERNAL_HOSTS = listOf(BuildConfig.WEB_BASE_HOST)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWebViewBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.toolbar.title = intent.extras?.getString(EXTRA_TITLE)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.webview.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.loading.isVisible = true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.loading.isVisible = false
            }

            // Any link you tap on we will open in the external browser
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                if (extraUrl == url) {
                    return false
                }
                val parsedUri = Uri.parse(url)
                return if (parsedUri != null && !uriIsInternal(parsedUri)) {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(url)
                    try {
                        startActivity(intent)
                        true
                    } catch (e: ActivityNotFoundException) {
                        false
                    }
                } else {
                    false
                }
            }
        }

        binding.webview.settings.javaScriptEnabled = true

        if (savedInstanceState == null) {
            extraUrl?.let { url ->
                binding.webview.loadUrl(url)
            }
        }
    }

    override fun onBackPressed() {
        if (binding.webview.canGoBack()) {
            binding.webview.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private fun uriIsInternal(url: Uri): Boolean {
        val host = url.host ?: return false
        return INTERNAL_HOSTS.count { host.endsWith(it) } > 0
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

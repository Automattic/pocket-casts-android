package au.com.shiftyjelly.pocketcasts.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionInflater
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.support.Support
import au.com.shiftyjelly.pocketcasts.settings.status.StatusFragment
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.HelpViewModel
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.extensions.findToolbar
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.launch
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.views.R as VR

@AndroidEntryPoint
class HelpFragment : BaseFragment(), HasBackstack, Toolbar.OnMenuItemClickListener {

    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper

    @Inject lateinit var settings: Settings

    @Inject lateinit var subscriptionManager: SubscriptionManager

    @Inject lateinit var support: Support

    val viewModel by viewModels<HelpViewModel>()

    private var webView: WebView? = null
    private var loadedUrl: String? = null
    private var loadingView: View? = null
    private var layoutError: View? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findToolbar()
        toolbar.setup(
            title = getString(LR.string.settings_title_help),
            navigationIcon = BackArrow,
            activity = activity,
            theme = theme,
            menu = R.menu.menu_help,
        )
        toolbar.setOnMenuItemClickListener(this)

        FirebaseAnalyticsTracker.userGuideOpened()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            loadedUrl = savedInstanceState.getString("url")
        }
        if (FeatureFlag.isEnabled(Feature.UPNEXT_IN_TAB_BAR)) {
            val inflater = TransitionInflater.from(requireContext())
            enterTransition = inflater.inflateTransition(au.com.shiftyjelly.pocketcasts.ui.R.transition.slide_in)
        }
        viewModel.onShown()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        loadedUrl?.let {
            outState.putString("url", loadedUrl)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(VR.layout.fragment_webview, container, false)

        webView = (view.findViewById<View>(VR.id.webview) as WebView).apply {
            webViewClient = SupportWebViewClient()
            loadUrl(loadedUrl ?: Settings.INFO_FAQ_URL)
            settings.javaScriptEnabled = true
            settings.textZoom = 100
            settings.domStorageEnabled = true
        }
        loadingView = view.findViewById(VR.id.progress_circle)
        layoutError = view.findViewById(VR.id.layoutLoadingError)

        view.findViewById<Button>(VR.id.btnContactSupport).setOnClickListener {
            contactSupport()
        }

        return view
    }

    override fun onMenuItemClick(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.menu_logs -> {
                val fragment = LogsFragment()
                (activity as? FragmentHostListener)?.addFragment(fragment, overBottomSheet = FeatureFlag.isEnabled(Feature.UPNEXT_IN_TAB_BAR))
                true
            }

            R.id.menu_status_page -> {
                val fragment = StatusFragment()
                (activity as? FragmentHostListener)?.addFragment(fragment, overBottomSheet = FeatureFlag.isEnabled(Feature.UPNEXT_IN_TAB_BAR))
                true
            }

            else -> false
        }

    override fun onBackPressed(): Boolean {
        webView?.let {
            if (it.canGoBack()) {
                it.goBack()
                return true
            }
        }
        return false
    }

    override fun onPause() {
        super.onPause()
        viewModel.onFragmentPause(activity?.isChangingConfigurations)
    }

    override fun getBackstackCount(): Int {
        return if (webView?.canGoBack() == true) 1 else 0
    }

    private inner class SupportWebViewClient : WebViewClient() {
        @Suppress("NAME_SHADOWING")
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            var url = request.url.toString()
            Timber.i("Webview loading url $url")

            when {
                url.lowercase(Locale.ROOT).contains("feedback") -> sendFeedbackEmail()
                url.startsWith("mailto:support@shiftyjelly.com") || url.startsWith("mailto:support@pocketcasts.com") -> {
                    contactSupport()
                }
                url.startsWith("https://support.pocketcasts.com") -> {
                    if (!url.contains("device=android")) {
                        url += (if (url.contains("?")) "&" else "?") + "device=android"
                    }
                    loadedUrl = url
                    view.loadUrl(url)
                }
                else -> {
                    val intent = Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(url) }
                    startActivity(intent)
                }
            }

            return true
        }

        override fun onLoadResource(view: WebView, url: String) {
            loadingView?.let {
                if (it.visibility != View.GONE && url.contains(".css")) {
                    it.visibility = View.GONE
                }
            }
            super.onLoadResource(view, url)
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            // only show the error message if the whole page fails, not just an asset
            if (request == null || view == null || request.url.toString() != view.url) {
                return
            }
            webView?.isVisible = false
            loadingView?.isVisible = false
            layoutError?.isVisible = true
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            loadingView?.isVisible = false
        }
    }

    private fun sendFeedbackEmail() {
        val context = context ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val intent = support.shareLogs(
                    subject = "Android feedback.",
                    intro = "It's a great app, but it really needs...",
                    emailSupport = true,
                    context = context,
                )
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                UiUtil.displayDialogNoEmailApp(context)
            }
        }

        analyticsTracker.track(AnalyticsEvent.SETTINGS_LEAVE_FEEDBACK)
        FirebaseAnalyticsTracker.userGuideEmailFeedback()
    }

    private fun contactSupport() {
        when (subscriptionManager.getCachedStatus()) {
            null, is SubscriptionStatus.Free -> useForumPopup()
            is SubscriptionStatus.Paid -> sendSupportEmail()
        }

        analyticsTracker.track(AnalyticsEvent.SETTINGS_GET_SUPPORT)
        FirebaseAnalyticsTracker.userGuideEmailSupport()
    }

    private fun useForumPopup() {
        val context = context ?: return
        val forumUrl = "https://forums.pocketcasts.com/"
        AlertDialog.Builder(context)
            .setTitle(LR.string.settings_forums)
            .setMessage(context.getString(LR.string.settings_forums_description, forumUrl))
            .setPositiveButton(LR.string.settings_take_me_there) { _, _ ->
                val intent =
                    Intent(Intent.ACTION_VIEW, Uri.parse(forumUrl))
                startActivity(intent)
            }
            .show()
    }

    private fun sendSupportEmail() {
        val context = context ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val intent = support.shareLogs(
                    subject = "Android support.",
                    intro = "Hi there, just needed help with something...",
                    emailSupport = true,
                    context = context,
                )
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                UiUtil.displayDialogNoEmailApp(context)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        enterTransition = null
    }
}

package au.com.shiftyjelly.pocketcasts.player.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.compose.bottomsheet.Pill
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.kevinnzou.web.LoadingState
import com.kevinnzou.web.WebView
import com.kevinnzou.web.WebViewState
import com.kevinnzou.web.rememberWebViewState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class PlaybackIssuesBottomSheetFragment : BaseDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        DialogBox(themeType = Theme.ThemeType.LIGHT) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(6.dp))
                Pill()
                Spacer(modifier = Modifier.height(6.dp))
                WebViewContainer(modifier = Modifier.weight(1f))
            }
        }
    }

    companion object {
        private const val TAG = "PlaybackIssuesBottomSheetFragment"

        fun show(fragmentManager: FragmentManager) {
            if (!fragmentManager.isStateSaved && fragmentManager.findFragmentByTag(TAG) == null) {
                PlaybackIssuesBottomSheetFragment().show(fragmentManager, TAG)
            }
        }
    }
}

private enum class ContainerState {
    Initializing,
    Loading,
    Loaded,
    Error,
}

@Composable
private fun WebViewContainer(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        val webViewState = rememberWebViewState(url = Settings.INFO_DOWNLOAD_AND_PLAYBACK_URL)
        var containerState by remember { mutableStateOf(ContainerState.Initializing) }

        LaunchedEffect(webViewState) {
            var loadedBefore = false
            combine(
                snapshotFlow { webViewState.loadingState },
                snapshotFlow { webViewState.errorsForCurrentRequest.toList() },
                ::Pair,
            ).collect { (loadingState, errors) ->
                containerState = when {
                    errors.any { it.request?.url?.toString() == webViewState.lastLoadedUrl } -> {
                        loadedBefore = false
                        ContainerState.Error
                    }

                    loadingState !is LoadingState.Finished && !loadedBefore -> ContainerState.Loading

                    else -> {
                        loadedBefore = true
                        ContainerState.Loaded
                    }
                }
            }
        }

        val animatedAlpha by animateFloatAsState(
            targetValue = if (containerState == ContainerState.Loaded) 1f else 0f,
        )
        val alpha = if (containerState == ContainerState.Error) 0f else animatedAlpha

        WebViewContent(
            state = webViewState,
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha),
        )

        AnimatedContent(
            targetState = containerState,
        ) { state ->
            when (state) {
                ContainerState.Loading -> ProgressIndicator(modifier = Modifier.fillMaxSize())
                ContainerState.Error -> ErrorDisplay(modifier = Modifier.fillMaxSize())
                ContainerState.Loaded, ContainerState.Initializing -> Unit
            }
        }
    }
}

@Composable
private fun ProgressIndicator(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.theme.colors.secondaryIcon01,
            modifier = Modifier.size(36.dp),
        )
    }
}

@Composable
private fun ErrorDisplay(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        TextP40(text = stringResource(LR.string.settings_help_cant_load))
    }
}

@Composable
private fun WebViewContent(
    state: WebViewState,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = MaterialTheme.theme.colors.primaryUi01
    WebView(
        state = state,
        modifier = modifier,
        onCreated = { webView ->
            webView.setBackgroundColor(backgroundColor.toArgb())
            webView.settings.apply {
                @SuppressLint("SetJavaScriptEnabled")
                javaScriptEnabled = true
                domStorageEnabled = true
                textZoom = 100
            }
            @SuppressLint("ClickableViewAccessibility")
            webView.setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        v.parent.requestDisallowInterceptTouchEvent(v.canScrollVertically(-1))
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.parent.requestDisallowInterceptTouchEvent(false)
                    }
                }
                false
            }
        },
    )
}

package au.com.shiftyjelly.pocketcasts.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.HelpViewModel
import au.com.shiftyjelly.pocketcasts.ui.helper.modifyHsv
import au.com.shiftyjelly.pocketcasts.views.helper.IntentUtil
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import com.kevinnzou.web.AccompanistWebViewClient
import com.kevinnzou.web.LoadingState
import com.kevinnzou.web.WebView
import com.kevinnzou.web.WebViewState
import com.kevinnzou.web.rememberWebViewState
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun HelpPage(
    activity: Activity,
    onShowLogs: () -> Unit,
    onShowStatusPage: () -> Unit,
    onGoBack: () -> Unit,
    modifier: Modifier = Modifier,
    appBarInsets: WindowInsets = AppBarDefaults.topAppBarWindowInsets,
    viewModel: HelpViewModel = hiltViewModel(),
    onWebViewCreate: (WebView) -> Unit = {},
    onWebViewDispose: (WebView) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var isExportingDatabase by remember { mutableStateOf(false) }

    Box(
        modifier = modifier,
    ) {
        HelpPage(
            appBarInsets = appBarInsets,
            onSendFeedbackEmail = {
                scope.launch {
                    val intent = viewModel.getFeedbackIntent(activity)
                    try {
                        activity.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        UiUtil.displayDialogNoEmailApp(activity)
                    }
                }
            },
            onContactSupport = {
                scope.launch {
                    val intent = viewModel.getSupportIntent(activity)
                    try {
                        activity.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        UiUtil.displayDialogNoEmailApp(activity)
                    }
                }
            },
            onTapUri = { uri ->
                activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
            },
            onShowLogs = onShowLogs,
            onShowStatusPage = onShowStatusPage,
            onExportDatabase = {
                if (!isExportingDatabase) {
                    isExportingDatabase = true
                    scope.launch {
                        viewModel.exportDatabase()?.let { file ->
                            IntentUtil.sendIntent(
                                context = activity,
                                file = file,
                                intentType = "application/zip",
                                errorMessage = activity.getString(LR.string.settings_export_database_failed),
                            )
                        }
                        isExportingDatabase = false
                    }
                }
            },
            onGoBack = onGoBack,
            onWebViewCreate = onWebViewCreate,
            onWebViewDispose = onWebViewDispose,
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = isExportingDatabase,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            WebViewProgressIndicator(
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun HelpPage(
    appBarInsets: WindowInsets,
    onSendFeedbackEmail: () -> Unit,
    onContactSupport: () -> Unit,
    onTapUri: (Uri) -> Unit,
    onShowLogs: () -> Unit,
    onShowStatusPage: () -> Unit,
    onExportDatabase: () -> Unit,
    onGoBack: () -> Unit,
    onWebViewCreate: (WebView) -> Unit,
    onWebViewDispose: (WebView) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
    ) {
        var showActionPopup by remember { mutableStateOf(false) }
        Column {
            AppBar(
                onGoBack = onGoBack,
                onShowActions = { showActionPopup = true },
                windowInsets = appBarInsets,
                modifier = Modifier
                    .fillMaxWidth()
                    // We use z-index to control the drawing order.
                    // Otherwise there is an ugly flicker when web view is loaded
                    .zIndex(2f),
            )
            HelpWebViewContainer(
                onSendFeedbackEmail = onSendFeedbackEmail,
                onContactSupport = onContactSupport,
                onTapUri = onTapUri,
                onWebViewCreate = onWebViewCreate,
                onWebViewDispose = onWebViewDispose,
                modifier = Modifier
                    .weight(1f)
                    .zIndex(1f),
            )
        }
        AppBarActionsEffect(
            show = showActionPopup,
            appBarInsets = appBarInsets,
            onShowLogs = onShowLogs,
            onShowStatusPage = onShowStatusPage,
            onExportDatabase = onExportDatabase,
            onDismiss = { showActionPopup = false },
        )
    }
}

@Composable
private fun AppBar(
    windowInsets: WindowInsets,
    onGoBack: () -> Unit,
    onShowActions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ThemedTopAppBar(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(LR.string.settings_title_help),
        windowInsets = windowInsets,
        onNavigationClick = onGoBack,
        actions = { iconColor ->
            IconButton(onClick = onShowActions) {
                Icon(
                    painter = painterResource(IR.drawable.ic_overflow),
                    contentDescription = stringResource(LR.string.more_options),
                    tint = iconColor,
                )
            }
        },
    )
}

@Composable
private fun BoxScope.AppBarActionsEffect(
    show: Boolean,
    appBarInsets: WindowInsets,
    onShowLogs: () -> Unit,
    onShowStatusPage: () -> Unit,
    onExportDatabase: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Outer box to interceptr all touches and dismiss the popup
    if (show) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = null,
                    onClick = onDismiss,
                ),
        )
    }
    // Animated actions
    AnimatedVisibility(
        visible = show,
        enter = popupEnterTransition,
        exit = popupExitTranstion,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .windowInsetsPadding(appBarInsets)
            .offset(x = -16.dp, y = 16.dp)
            .shadow(elevation = 8.dp),
    ) {
        val baseColor = MaterialTheme.theme.colors.secondaryUi01
        // Brigthen or darken popup background a bit to make it stand out more above the app bar
        val popupColor = remember(baseColor) {
            baseColor.modifyHsv { h, s, v ->
                val newValue = if (v < 0.5f) {
                    v.coerceAtLeast(0.1f) * 1.2f
                } else {
                    v * 0.95f
                }
                Color.hsv(h, s, newValue)
            }
        }
        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .widthIn(min = 200.dp)
                .background(
                    color = popupColor,
                    shape = MaterialTheme.shapes.small,
                ),
        ) {
            ActionRow(
                text = stringResource(LR.string.settings_logs),
                onClick = {
                    onShowLogs()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            ActionRow(
                text = stringResource(LR.string.settings_status_page),
                onClick = {
                    onShowStatusPage()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            ActionRow(
                text = stringResource(LR.string.settings_export_database),
                onClick = {
                    onExportDatabase()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ActionRow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextP40(
        text = text,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.theme.colors.secondaryText01,
        modifier = modifier
            .clickable(
                indication = ripple(color = MaterialTheme.theme.colors.secondaryText01),
                interactionSource = null,
                onClick = onClick,
                role = Role.Button,
            )
            .padding(16.dp),
    )
}

@Composable
private fun HelpWebViewContainer(
    onSendFeedbackEmail: () -> Unit,
    onContactSupport: () -> Unit,
    onTapUri: (Uri) -> Unit,
    onWebViewCreate: (WebView) -> Unit,
    onWebViewDispose: (WebView) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
    ) {
        var initialUrl by rememberSaveable { mutableStateOf(Settings.INFO_FAQ_URL) }
        val webViewState = rememberWebViewState(url = initialUrl)
        var containerState by remember { mutableStateOf(ContainerState.Initalizing) }
        LaunchedEffect(webViewState) {
            var loadedBefore = false
            combine(
                snapshotFlow { webViewState.loadingState },
                snapshotFlow { webViewState.errorsForCurrentRequest.toList() },
                ::Pair,
            ).collect { (loadingState, errors) ->
                containerState = when {
                    // only show the error message if the whole page fails, not just an asset
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
        // Do not animate alpha if there is an error state to avoid showing web error content
        val alpha = if (containerState == ContainerState.Error) 0f else animatedAlpha
        HelpWebView(
            state = webViewState,
            onSendFeedbackEmail = onSendFeedbackEmail,
            onContactSupport = onContactSupport,
            onTapUri = onTapUri,
            onLoadUrl = { initialUrl = it },
            onWebViewCreate = onWebViewCreate,
            onWebViewDispose = onWebViewDispose,
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha),
        )
        AnimatedContent(
            targetState = containerState,
        ) { state ->
            when (state) {
                ContainerState.Loading -> WebViewProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                )

                ContainerState.Error -> WebViewError(
                    onContactSupport = onContactSupport,
                    modifier = Modifier.fillMaxSize(),
                )
                // Web View is not part of AnimatedContent because we want to keep it active
                // to load pages regardless of the container state
                ContainerState.Loaded, ContainerState.Initalizing -> Unit
            }
        }
    }
}

private enum class ContainerState {
    Initalizing,
    Loading,
    Loaded,
    Error,
}

@Composable
private fun WebViewProgressIndicator(
    modifier: Modifier = Modifier,
) {
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
private fun WebViewError(
    onContactSupport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp),
    ) {
        TextP40(
            text = stringResource(LR.string.settings_help_cant_load),
            textAlign = TextAlign.Center,
        )
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        RowButton(
            text = stringResource(LR.string.settings_help_contact_support),
            onClick = onContactSupport,
        )
    }
}

@Composable
private fun HelpWebView(
    state: WebViewState,
    onSendFeedbackEmail: () -> Unit,
    onContactSupport: () -> Unit,
    onTapUri: (Uri) -> Unit,
    onLoadUrl: (String) -> Unit,
    onWebViewCreate: (WebView) -> Unit,
    onWebViewDispose: (WebView) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = MaterialTheme.theme.colors.primaryUi01
    WebView(
        state = state,
        modifier = modifier,
        client = remember {
            HelpWebViewClient(
                onSendFeedbackEmail = onSendFeedbackEmail,
                onContactSupport = onContactSupport,
                onTapUri = onTapUri,
                onLoadUrl = onLoadUrl,
            )
        },
        onCreated = { webView ->
            webView.setBackgroundColor(backgroundColor.toArgb())
            webView.settings.apply {
                @SuppressLint("SetJavaScriptEnabled")
                javaScriptEnabled = true
                domStorageEnabled = true
                textZoom = 100
            }
            onWebViewCreate(webView)
        },
        onDispose = onWebViewDispose,
    )
}

private class HelpWebViewClient(
    private val onSendFeedbackEmail: () -> Unit,
    private val onContactSupport: () -> Unit,
    private val onTapUri: (Uri) -> Unit,
    private val onLoadUrl: (String) -> Unit,
) : AccompanistWebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()

        when {
            url.contains("feedback", ignoreCase = true) -> onSendFeedbackEmail()
            contactSupportAction.any { url.startsWith(it) } -> onContactSupport()
            url.startsWith("https://support.pocketcasts.com") -> {
                val androidUrl = if ("device=android" !in url) {
                    buildString {
                        append(url)
                        append(if ('?' in url) '&' else '?')
                        append("device=android")
                    }
                } else {
                    url
                }
                onLoadUrl(androidUrl)
                view.loadUrl(androidUrl)
            }

            else -> onTapUri(request.url)
        }

        return true
    }
}

private val contactSupportAction = listOf("mailto:support@shiftyjelly.com", "mailto:support@pocketcasts.com")
private val fadeAnimationSpec = spring<Float>(stiffness = Spring.StiffnessMedium, visibilityThreshold = 0.001f)
private val expandAnimationSpec = spring<IntSize>(stiffness = Spring.StiffnessMedium, visibilityThreshold = IntSize.VisibilityThreshold)
private val popupEnterTransition = fadeIn(fadeAnimationSpec) + expandIn(expandAnimationSpec, expandFrom = Alignment.TopEnd)
private val popupExitTranstion = fadeOut(fadeAnimationSpec) + shrinkOut(expandAnimationSpec, shrinkTowards = Alignment.TopEnd)

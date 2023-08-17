package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.LogsViewModel
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class LogsFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setContent {
                AppThemeWithBackground(theme.activeTheme) {
                    LogsPage(
                        onBackPressed = ::closeFragment
                    )
                }
            }
        }

    private fun closeFragment() {
        (activity as? FragmentHostListener)?.closeModal(this)
    }
}

@Composable
private fun LogsPage(
    onBackPressed: () -> Unit,
) {
    val viewModel = hiltViewModel<LogsViewModel>()
    val state by viewModel.state.collectAsState()
    val logs = state.logs
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LogsContent(
        onBackPressed = onBackPressed,
        onCopyToClipboard = { logs?.let { clipboardManager.setText(AnnotatedString(it)) } },
        onShareLogs = { viewModel.shareLogs(context) },
        includeAppBar = !Util.isAutomotive(context),
        logs = logs
    )
}

@Composable
private fun LogsContent(
    onBackPressed: () -> Unit,
    onCopyToClipboard: () -> Unit,
    onShareLogs: () -> Unit,
    logs: String?,
    includeAppBar: Boolean
) {
    val logScrollState = rememberScrollState(0)
    Column {
        if (includeAppBar) {
            val coroutineScope = rememberCoroutineScope()
            AppBarWithShare(
                onBackPressed = onBackPressed,
                onCopyToClipboard = onCopyToClipboard,
                onShareLogs = onShareLogs,
                onScrollToTop = {
                    coroutineScope.launch {
                        logScrollState.animateScrollTo(0)
                    }
                },
                onScrollToBottom = {
                    coroutineScope.launch {
                        logScrollState.animateScrollTo(Int.MAX_VALUE)
                    }
                },
                logsAvailable = logs != null,
            )
        }
        Column(
            modifier = Modifier
                .verticalScroll(logScrollState)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            if (logs == null) {
                LoadingView()
            } else {
                TextP60(logs)
            }
        }
    }
    // scroll to the end to show the latest logs
    LaunchedEffect(logs) {
        logScrollState.scrollTo(logScrollState.maxValue)
    }
}

@Composable
private fun AppBarWithShare(
    onBackPressed: () -> Unit,
    onCopyToClipboard: () -> Unit,
    onShareLogs: () -> Unit,
    onScrollToTop: () -> Unit,
    onScrollToBottom: () -> Unit,
    logsAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    ThemedTopAppBar(
        title = stringResource(LR.string.settings_logs),
        onNavigationClick = onBackPressed,
        actions = {
            IconButton(
                onClick = onScrollToTop,
                enabled = logsAvailable,
            ) {
                Icon(
                    imageVector = Icons.Default.VerticalAlignTop,
                    contentDescription = stringResource(LR.string.go_to_top)
                )
            }
            IconButton(
                onClick = onScrollToBottom,
                enabled = logsAvailable,
            ) {
                Icon(
                    imageVector = Icons.Default.VerticalAlignBottom,
                    contentDescription = stringResource(LR.string.go_to_bottom)
                )
            }
            IconButton(
                onClick = onCopyToClipboard,
                enabled = logsAvailable
            ) {
                Icon(
                    imageVector = Icons.Default.CopyAll,
                    contentDescription = stringResource(LR.string.share)
                )
            }
            IconButton(
                onClick = onShareLogs,
                enabled = logsAvailable
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = stringResource(LR.string.share)
                )
            }
        },
        modifier = modifier
    )
}

@Composable
@Preview
private fun LogsContentPreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        LogsContent(
            onBackPressed = {},
            onCopyToClipboard = {},
            onShareLogs = {},
            logs = "This is a preview",
            includeAppBar = true
        )
    }
}
@Composable
@Preview
private fun LogsContentLoadingPreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        LogsContent(
            onBackPressed = {},
            onCopyToClipboard = {},
            onShareLogs = {},
            logs = null,
            includeAppBar = true
        )
    }
}

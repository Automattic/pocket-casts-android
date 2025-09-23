package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.AppBarDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.LogsViewModel
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class LogsFragment : BaseFragment() {
    @Inject lateinit var settings: Settings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        UiUtil.hideKeyboard(LocalView.current)
        AppThemeWithBackground(theme.activeTheme) {
            val bottomInset = settings.bottomInset.collectAsStateWithLifecycle(initialValue = 0)
            LogsPage(
                bottomInset = bottomInset.value.pxToDp(LocalContext.current).dp,
                onBackPress = ::closeFragment,
            )
        }
    }

    private fun closeFragment() {
        (activity as? FragmentHostListener)?.closeModal(this)
    }
}

@Composable
fun LogsPage(
    bottomInset: Dp,
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier,
    appBarInsets: WindowInsets = AppBarDefaults.topAppBarWindowInsets,
    viewModel: LogsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val logs = state.logs
    val logLines = state.logLines
    val context = LocalContext.current

    LogsContent(
        onBackPress = onBackPress,
        onCopyToClipboard = { viewModel.copyToClipboard(context, logs) },
        onShareLogs = { viewModel.shareLogs(context) },
        includeAppBar = !Util.isAutomotive(context),
        logLines = logLines,
        bottomInset = bottomInset,
        appBarInsets = appBarInsets,
        modifier = modifier,
    )
}

@Composable
private fun LogsContent(
    onBackPress: () -> Unit,
    onCopyToClipboard: () -> Unit,
    onShareLogs: () -> Unit,
    logLines: List<String>,
    includeAppBar: Boolean,
    bottomInset: Dp,
    appBarInsets: WindowInsets,
    modifier: Modifier = Modifier,
) {
    val logScrollState = rememberLazyListState()
    Column(
        modifier = modifier,
    ) {
        if (includeAppBar) {
            val coroutineScope = rememberCoroutineScope()
            AppBarWithShare(
                appBarInsets = appBarInsets,
                logsAvailable = logLines.isNotEmpty(),
                onBackPress = onBackPress,
                onCopyToClipboard = onCopyToClipboard,
                onShareLogs = onShareLogs,
                onScrollToTop = {
                    if (logLines.isNotEmpty()) {
                        coroutineScope.launch {
                            logScrollState.animateScrollToItem(0)
                        }
                    }
                },
                onScrollToBottom = {
                    if (logLines.isNotEmpty()) {
                        coroutineScope.launch {
                            logScrollState.animateScrollToItem(Int.MAX_VALUE)
                        }
                    }
                },
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
        ) {
            if (logLines.isEmpty()) {
                LoadingView(
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                SelectionContainer {
                    LazyColumn(
                        state = logScrollState,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        item {
                            Spacer(modifier = Modifier.size(16.dp))
                        }
                        items(logLines) { log ->
                            TextP60(log)
                        }
                        item {
                            Spacer(modifier = Modifier.size(bottomInset))
                        }
                    }
                }
            }
        }
    }
    // scroll to the end to show the latest logs
    LaunchedEffect(logLines) {
        if (logLines.isNotEmpty()) {
            logScrollState.scrollToItem(Int.MAX_VALUE)
        }
    }
}

@Composable
private fun AppBarWithShare(
    onBackPress: () -> Unit,
    onCopyToClipboard: () -> Unit,
    onShareLogs: () -> Unit,
    onScrollToTop: () -> Unit,
    onScrollToBottom: () -> Unit,
    logsAvailable: Boolean,
    appBarInsets: WindowInsets,
    modifier: Modifier = Modifier,
) {
    ThemedTopAppBar(
        title = stringResource(LR.string.settings_logs),
        windowInsets = appBarInsets,
        onNavigationClick = onBackPress,
        actions = {
            IconButton(
                onClick = onScrollToTop,
                enabled = logsAvailable,
            ) {
                Icon(
                    imageVector = Icons.Default.VerticalAlignTop,
                    contentDescription = stringResource(LR.string.go_to_top),
                )
            }
            IconButton(
                onClick = onScrollToBottom,
                enabled = logsAvailable,
            ) {
                Icon(
                    imageVector = Icons.Default.VerticalAlignBottom,
                    contentDescription = stringResource(LR.string.go_to_bottom),
                )
            }
            IconButton(
                onClick = onCopyToClipboard,
                enabled = logsAvailable,
            ) {
                Icon(
                    imageVector = Icons.Default.CopyAll,
                    contentDescription = stringResource(LR.string.share),
                )
            }
            IconButton(
                onClick = onShareLogs,
                enabled = logsAvailable,
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = stringResource(LR.string.share),
                )
            }
        },
        modifier = modifier,
    )
}

@Composable
@Preview
private fun LogsContentPreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        LogsContent(
            onBackPress = {},
            onCopyToClipboard = {},
            onShareLogs = {},
            logLines = listOf(
                "This is a preview",
                "Of some logs",
                "In our app",
            ),
            includeAppBar = true,
            bottomInset = 0.dp,
            appBarInsets = AppBarDefaults.topAppBarWindowInsets,
        )
    }
}

@Composable
@Preview
private fun LogsContentLoadingPreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        LogsContent(
            onBackPress = {},
            onCopyToClipboard = {},
            onShareLogs = {},
            logLines = emptyList(),
            includeAppBar = true,
            bottomInset = 0.dp,
            appBarInsets = AppBarDefaults.topAppBarWindowInsets,
        )
    }
}

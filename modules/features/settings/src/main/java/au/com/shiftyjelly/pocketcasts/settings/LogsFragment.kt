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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.LogsViewModel
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
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

    Column {

        ThemedTopAppBar(
            title = stringResource(LR.string.settings_logs),
            onNavigationClick = onBackPressed,
            actions = {
                IconButton(
                    onClick = {
                        logs?.let {
                            clipboardManager.setText(AnnotatedString(it))
                        }
                    },
                    enabled = logs != null
                ) {
                    Icon(
                        imageVector = Icons.Default.CopyAll,
                        contentDescription = stringResource(LR.string.share)
                    )
                }

                IconButton(
                    onClick = { viewModel.shareLogs(context) },
                    enabled = logs != null
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(LR.string.share)
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .fillMaxWidth()
        ) {

            if (logs == null) {
                TextH30(
                    text = stringResource(LR.string.loading),
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                TextP60(logs)
            }
        }
    }
}

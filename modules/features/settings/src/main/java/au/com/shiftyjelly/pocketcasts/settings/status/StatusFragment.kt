package au.com.shiftyjelly.pocketcasts.settings.status

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class StatusFragment : BaseFragment() {
    @Inject
    lateinit var settings: Settings
    private val viewModel: StatusViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        UiUtil.hideKeyboard(LocalView.current)
        val bottomInset = settings.bottomInset.collectAsStateWithLifecycle(initialValue = 0)
        AppThemeWithBackground(theme.activeTheme) {
            StatusPage(
                viewModel = viewModel,
                bottomInset = bottomInset.value.pxToDp(LocalContext.current).dp,
                onBackPressed = { closeFragment() },
            )
        }
    }

    private fun closeFragment() {
        (activity as? FragmentHostListener)?.closeModal(this)
    }
}

@Composable
fun StatusPage(
    bottomInset: Dp,
    onBackPressed: () -> Unit,
    viewModel: StatusViewModel = hiltViewModel(),
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = bottomInset),

    ) {
        item {
            ThemedTopAppBar(
                title = stringResource(LR.string.settings_status_page),
                onNavigationClick = onBackPressed,
            )
        }
        item {
            StatusPageContent(viewModel = viewModel)
        }
    }
}

@Composable
fun StatusPageContent(viewModel: StatusViewModel) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
    ) {
        Text(
            text = stringResource(LR.string.settings_status_description),
            color = MaterialTheme.theme.colors.primaryText01,
            style = MaterialTheme.typography.body1.copy(lineHeight = 20.sp),
            modifier = Modifier.padding(bottom = 16.dp),
        )
        when (val state = viewModel.uiState.collectAsState().value) {
            is StatusUiState.Welcome -> StatusWelcomePage(viewModel = viewModel)
            is StatusUiState.ListServices -> StatusServicesPage(state = state, viewModel = viewModel)
        }
    }
}

@Composable
fun StatusWelcomePage(viewModel: StatusViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Button(
            onClick = { viewModel.run() },
        ) {
            Text(stringResource(LR.string.settings_status_run))
        }
    }
}

@Composable
fun StatusServicesPage(state: StatusUiState.ListServices, viewModel: StatusViewModel) {
    val context = LocalContext.current
    Column {
        state.services.forEach { service ->
            ServiceStatusRow(
                title = stringResource(service.title),
                summary = stringResource(service.summary),
                help = service.helpString(context),
                status = service.status,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Button(
                onClick = { viewModel.run() },
                enabled = !state.running,
            ) {
                Text(stringResource(LR.string.settings_status_retry))
            }
            Spacer(modifier = Modifier.width(24.dp))
            Button(
                onClick = { viewModel.sendReport(context) },
                enabled = !state.running,
            ) {
                Text(stringResource(LR.string.settings_status_send_report))
            }
        }
    }
}

@Composable
fun ServiceStatusRow(title: String, summary: String, help: String, status: ServiceStatus) {
    Row(
        modifier = Modifier
            .padding(vertical = 16.dp)
            .padding(end = 16.dp),
    ) {
        Box(modifier = Modifier.padding(top = 3.dp)) {
            when (status) {
                is ServiceStatus.Success -> {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = null,
                        tint = MaterialTheme.theme.colors.support02,
                    )
                }
                is ServiceStatus.Failed -> {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.theme.colors.support05,
                    )
                }
                is ServiceStatus.Running -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .width(24.dp)
                            .height(24.dp),
                        strokeWidth = 2.dp,
                    )
                }
                else -> Spacer(
                    modifier = Modifier
                        .width(24.dp)
                        .height(24.dp),
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.theme.colors.primaryText01,
            )
            if (status is ServiceStatus.Failed) {
                if (status.userMessage != null) {
                    Text(
                        text = status.userMessage,
                        color = MaterialTheme.theme.colors.support05,
                        style = MaterialTheme.typography.body1.copy(lineHeight = 20.sp),
                    )
                }
                Text(
                    text = help,
                    color = MaterialTheme.theme.colors.primaryText02,
                    style = MaterialTheme.typography.body1.copy(lineHeight = 20.sp),
                )
            } else {
                Text(
                    text = summary,
                    color = MaterialTheme.theme.colors.primaryText02,
                    style = MaterialTheme.typography.body1.copy(lineHeight = 20.sp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewServiceStatusRow() {
    MaterialTheme {
        ServiceStatusRow(
            title = stringResource(LR.string.settings_status_service_internet),
            summary = "test",
            help = "Ouch",
            status = ServiceStatus.Running,
        )
    }
}

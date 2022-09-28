package au.com.shiftyjelly.pocketcasts.settings.privacy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.text.LinkText
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.extensions.startActivityViewUrl
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class PrivacyFragment : BaseFragment() {

    private val viewModel: PrivacyViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppThemeWithBackground(theme.activeTheme) {
                val state: PrivacyViewModel.UiState by viewModel.uiState.collectAsState()
                PrivacySettings(
                    state = state,
                    onAnalyticsClick = {
                        viewModel.updateAnalyticsSetting(it)
                    },
                    onCrashReportsClick = {
                        viewModel.updateCrashReportsSetting(it)
                    },
                    onLinkAccountClick = {
                        viewModel.updateLinkAccountSetting(it)
                    },
                    onPrivacyPolicyClick = {
                        context.startActivityViewUrl(Settings.INFO_PRIVACY_URL)
                    },
                    onBackClick = {
                        @Suppress("DEPRECATION")
                        activity?.onBackPressed()
                    }
                )
            }
        }
    }

    @Composable
    private fun PrivacySettings(
        state: PrivacyViewModel.UiState,
        onAnalyticsClick: (Boolean) -> Unit,
        onCrashReportsClick: (Boolean) -> Unit,
        onLinkAccountClick: (Boolean) -> Unit,
        onPrivacyPolicyClick: () -> Unit,
        onBackClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier
                .background(MaterialTheme.theme.colors.primaryUi02)
                .fillMaxHeight()
        ) {
            ThemedTopAppBar(
                title = stringResource(LR.string.settings_title_privacy),
                onNavigationClick = onBackClick
            )
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
            ) {
                TextP50(
                    text = stringResource(LR.string.settings_privacy_summary),
                    color = MaterialTheme.theme.colors.primaryText02,
                    modifier = Modifier.padding(16.dp)
                )
                if (state is PrivacyViewModel.UiState.Loaded) {
                    SettingRow(
                        primaryText = stringResource(LR.string.settings_privacy_analytics),
                        secondaryText = stringResource(LR.string.settings_privacy_analytics_summary),
                        toggle = SettingRowToggle.Switch(checked = state.analytics),
                        modifier = Modifier.toggleable(
                            value = state.analytics,
                            role = Role.Switch
                        ) { onAnalyticsClick(!state.analytics) },
                        indent = false
                    )
                    SettingRow(
                        primaryText = stringResource(LR.string.settings_privacy_crash),
                        secondaryText = stringResource(LR.string.settings_privacy_crash_summary),
                        toggle = SettingRowToggle.Switch(checked = state.crashReports),
                        modifier = Modifier.toggleable(
                            value = state.crashReports,
                            role = Role.Switch
                        ) { onCrashReportsClick(!state.crashReports) },
                        indent = false
                    )
                    if (state.crashReports) {
                        SettingRow(
                            primaryText = stringResource(LR.string.settings_privacy_crash_link),
                            secondaryText = stringResource(LR.string.settings_privacy_crash_link_summary),
                            toggle = SettingRowToggle.Switch(checked = state.linkAccount),
                            modifier = Modifier.toggleable(
                                value = state.linkAccount,
                                role = Role.Switch
                            ) { onLinkAccountClick(!state.linkAccount) },
                            indent = false
                        )
                    }
                    LinkText(
                        text = stringResource(LR.string.profile_privacy_policy_read),
                        textAlign = TextAlign.Start,
                        onClick = onPrivacyPolicyClick
                    )
                }
            }
        }
    }
}

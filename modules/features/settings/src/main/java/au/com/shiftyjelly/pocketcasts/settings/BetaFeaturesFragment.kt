package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.BetaFeaturesViewModel
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BetaFeaturesFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppThemeWithBackground(theme.activeTheme) {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    BetaFeaturesPage(
                        onBackPressed = {
                            @Suppress("DEPRECATION")
                            activity?.onBackPressed()
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun BetaFeaturesPage(
        onBackPressed: () -> Unit,
    ) {
        val viewModel = hiltViewModel<BetaFeaturesViewModel>()
        val state by viewModel.state.collectAsState()
        Column {
            ThemedTopAppBar(
                title = stringResource(R.string.settings_beta_features),
                bottomShadow = true,
                onNavigationClick = { onBackPressed() }
            )

            for (feature in state.featureFlags) {
                SettingRow(
                    primaryText = feature.featureFlag.title,
                    toggle = SettingRowToggle.Switch(checked = feature.isEnabled),
                    modifier = Modifier.toggleable(
                        value = feature.isEnabled,
                        role = Role.Switch
                    ) {
                        viewModel.setFeatureEnabled(feature.featureFlag, it)
                    },
                    indent = false,
                )
            }
        }
    }
}

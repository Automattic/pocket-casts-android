package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.BetaFeaturesViewModel
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BetaFeaturesFragment : BaseFragment() {

    @Inject
    lateinit var settings: Settings

    private val viewModel: BetaFeaturesViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        AppThemeWithBackground(theme.activeTheme) {
            val state by viewModel.state.collectAsState()
            val bottomInset = settings.bottomInset.collectAsStateWithLifecycle(0)
            BetaFeaturesPage(
                state = state,
                onFeatureEnabled = viewModel::setFeatureEnabled,
                onBackClick = {
                    @Suppress("DEPRECATION")
                    activity?.onBackPressed()
                },
                bottomInset = bottomInset.value.pxToDp(LocalContext.current).dp,
            )
        }
    }
}

@Composable
private fun BetaFeaturesPage(
    state: BetaFeaturesViewModel.State,
    onFeatureEnabled: (Feature, Boolean) -> Unit,
    onBackClick: () -> Unit,
    bottomInset: Dp,
) {
    LazyColumn(
        contentPadding = PaddingValues(top = 16.dp, bottom = bottomInset + 16.dp),
    ) {
        item {
            ThemedTopAppBar(
                title = stringResource(R.string.settings_beta_features),
                onNavigationClick = { onBackClick() },
            )
        }

        for (feature in state.featureFlags) {
            item {
                SettingRow(
                    primaryText = feature.featureFlag.title,
                    toggle = SettingRowToggle.Switch(checked = feature.isEnabled),
                    modifier = Modifier.toggleable(
                        value = feature.isEnabled,
                        role = Role.Switch,
                    ) {
                        onFeatureEnabled(feature.featureFlag, it)
                    },
                    indent = false,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BetaFeaturesPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        BetaFeaturesPage(
            state = BetaFeaturesViewModel.State(featureFlags = Feature.entries.map { BetaFeaturesViewModel.FeatureFlagWrapper(featureFlag = it, isEnabled = true) }),
            onFeatureEnabled = { _, _ -> },
            onBackClick = {},
            bottomInset = 0.dp,
        )
    }
}

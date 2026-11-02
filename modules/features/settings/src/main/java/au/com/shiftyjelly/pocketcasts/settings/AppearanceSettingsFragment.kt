package au.com.shiftyjelly.pocketcasts.settings

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.setContentWithViewCompositionStrategy
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.settings.databinding.FragmentSettingsAppearanceBinding
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.SettingsAppearanceState
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.SettingsAppearanceViewModel
import au.com.shiftyjelly.pocketcasts.ui.helper.AppIcon
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.worker.RefreshArtworkWorker
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class AppearanceSettingsFragment : BaseFragment() {
    companion object {
        fun newInstance(): AppearanceSettingsFragment {
            return AppearanceSettingsFragment()
        }
    }

    @Inject lateinit var settings: Settings

    @Inject lateinit var subscriptionManager: SubscriptionManager

    private val viewModel: SettingsAppearanceViewModel by viewModels()
    private var binding: FragmentSettingsAppearanceBinding? = null
    private var themes by mutableStateOf(emptyList<Theme.ThemeType>())
    private var selectedTheme by mutableStateOf<Theme.ThemeType?>(null)
    private var appIcons by mutableStateOf(emptyList<AppIcon.AppIconType>())
    private var selectedAppIcon by mutableStateOf<AppIcon.AppIconType?>(null)
    private var signInState by mutableStateOf<SignInState?>(null)
    private var themeScrollRequest by mutableIntStateOf(0)
    private var appIconScrollRequest by mutableIntStateOf(0)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSettingsAppearanceBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        binding.toolbar.setup(title = getString(LR.string.settings_title_appearance), navigationIcon = BackArrow, activity = activity, theme = theme)

        binding.themeRecyclerView.setContentWithViewCompositionStrategy {
            AppTheme(theme.activeTheme) { ThemePicker() }
        }
        binding.appIconRecyclerView.setContentWithViewCompositionStrategy {
            AppTheme(theme.activeTheme) { AppIconPicker() }
        }

        viewModel.createAccountState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SettingsAppearanceState.ThemesAndIconsLoading -> {}

                is SettingsAppearanceState.ThemesAndIconsLoaded -> {
                    themes = state.themeList
                    selectedTheme = state.currentThemeType
                    appIcons = state.iconList
                    selectedAppIcon = state.currentAppIcon
                    scrollToCurrentTheme()
                    scrollToCurrentAppIcon()
                }

                else -> {}
            }
        }

        viewModel.signInState.observe(viewLifecycleOwner) { signInState ->

            viewModel.changeThemeType.let { changeThemeType ->
                val beforeThemeType = changeThemeType.first
                val afterThemeType = changeThemeType.second

                if (beforeThemeType != null && afterThemeType != null) {
                    if (signInState.isSignedInAsPlusOrPatron) {
                        (activity as? AppCompatActivity)?.let {
                            theme.updateTheme(it, afterThemeType)
                            binding.swtSystemTheme.isChecked = theme.getUseSystemTheme() // Update switch if changing the theme updated the setting
                            viewModel.updateChangeThemeType(Pair(null, null))
                        }
                    } else {
                        viewModel.updateChangeThemeType(Pair(null, null))
                        selectedTheme = beforeThemeType
                        scrollToCurrentTheme()
                    }
                }
            }

            viewModel.changeAppIconType.let { changeAppIconType ->
                val beforeAppIconType = changeAppIconType.first
                val afterAppIconType = changeAppIconType.second

                if (beforeAppIconType != null && afterAppIconType != null) {
                    if (signInState.isSignedInAsPlusOrPatron) {
                        viewModel.updateGlobalIcon(afterAppIconType)
                        viewModel.updateChangeAppIconType(Pair(null, null))

                        AlertDialog.Builder(binding.appIconRecyclerView.context)
                            .setTitle(LR.string.settings_app_icon_updated)
                            .setMessage(LR.string.settings_app_icon_updated_message_v2)
                            .setPositiveButton(LR.string.settings_app_icon_ok, null)
                            .show()
                    } else {
                        selectedAppIcon = beforeAppIconType
                        scrollToCurrentAppIcon()
                    }
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    settings.bottomInset.collect {
                        binding.nestedScrollView.updatePadding(bottom = it)
                    }
                }
            }

            this.signInState = signInState
            binding.upgradeGroup.isVisible = !signInState.isSignedInAsPlusOrPatron && !settings.getUpgradeClosedAppearSettings()
        }

        viewModel.loadThemesAndIcons()

        binding.swtSystemTheme.isChecked = theme.getUseSystemTheme()
        binding.swtSystemTheme.setOnCheckedChangeListener { _, isChecked ->
            viewModel.useAndroidLightDarkMode(isChecked, activity as? AppCompatActivity)
        }
        binding.btnSystemTheme.setOnClickListener {
            binding.swtSystemTheme.isChecked = !binding.swtSystemTheme.isChecked
        }

        binding.swtDarkUpNext.isChecked = settings.useDarkUpNextTheme.value
        binding.swtDarkUpNext.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateUpNextDarkTheme(isChecked)
        }
        binding.btnUseDarkUpNext.setOnClickListener {
            binding.swtDarkUpNext.isChecked = !binding.swtDarkUpNext.isChecked
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.dividerViewWidget.isVisible = true
            binding.lblWidget.isVisible = true
            binding.lblUseDynamicColorsForWidget.isVisible = true
            binding.lblDynamicColorsForWidgetDetails.isVisible = true
            binding.swtDynamicColorsForWidget.isVisible = true
            binding.btnUseDynamicColorsForWidget.isVisible = true
        }
        binding.swtDynamicColorsForWidget.isChecked = settings.useDynamicColorsForWidget.value
        binding.swtDynamicColorsForWidget.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateWidgetForDynamicColors(isChecked)
        }
        binding.btnUseDynamicColorsForWidget.setOnClickListener {
            binding.swtDynamicColorsForWidget.isChecked = !binding.swtDynamicColorsForWidget.isChecked
        }

        binding.swtShowArtwork.isChecked = viewModel.showArtworkOnLockScreen.value
        binding.swtShowArtwork.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateShowArtworkOnLockScreen(isChecked)
        }
        binding.btnShowArtwork.setOnClickListener {
            binding.swtShowArtwork.isChecked = !binding.swtShowArtwork.isChecked
        }

        binding.swtUseEpisodeArtwork.isChecked = viewModel.artworkConfiguration.value.useEpisodeArtwork
        binding.swtUseEpisodeArtwork.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateUseEpisodeArtwork(isChecked)
        }
        binding.btnUseEpisodeArtwork.setOnClickListener {
            binding.swtUseEpisodeArtwork.isChecked = !binding.swtUseEpisodeArtwork.isChecked
        }

        binding.lblEpisodeArtworkConfiguration.setOnClickListener {
            showEpisodeArtworkConfigurationFragment()
        }

        binding.lblRefreshAllPodcastArtwork.setOnClickListener {
            refreshArtwork()
        }

        binding.upgradeBannerBackground.setOnClickListener {
            openOnboardingFlow()
        }

        binding.btnCloseUpgrade.setOnClickListener {
            viewModel.onUpgradeBannerDismissed(SourceView.APPEARANCE)
            settings.setUpgradeClosedAppearSettings(true)
            binding.upgradeGroup.isVisible = false
        }

        viewModel.onShown()
    }

    private fun openOnboardingFlow(tier: SubscriptionTier? = null, source: OnboardingUpgradeSource = OnboardingUpgradeSource.APPEARANCE) {
        val onboardingFlow = tier?.takeIf { tier == SubscriptionTier.Patron }?.let {
            OnboardingFlow.Upsell(source = source)
        } ?: OnboardingFlow.Upsell(source)
        OnboardingLauncher.openOnboardingFlow(requireActivity(), onboardingFlow)
    }

    private fun scrollToCurrentTheme() {
        themeScrollRequest++
    }

    private fun scrollToCurrentAppIcon() {
        appIconScrollRequest++
    }

    @Composable
    private fun ThemePicker() {
        val isPlusOrPatron = signInState?.isSignedInAsPlusOrPatron == true
        AppearancePicker(
            options = themes.map { themeType ->
                val isLocked = themeType.isPlus && !isPlusOrPatron
                val isSelected = themeType == selectedTheme
                AppearanceOption(
                    imageResId = themeType.iconResourceId,
                    label = stringResource(themeType.labelId),
                    imageContentDescription = stringResource(LR.string.settings_theme),
                    badgeResId = when {
                        isLocked -> R.drawable.ic_locked_plus
                        isSelected -> R.drawable.ic_circle_tick
                        else -> null
                    },
                    isSelected = isSelected,
                    isLocked = isLocked,
                )
            },
            isTall = true,
            endPadding = 8.dp,
            scrollRequest = themeScrollRequest,
            onClick = { index -> onThemeClick(themes[index]) },
        )
    }

    @Composable
    private fun AppIconPicker() {
        val signInState = signInState
        AppearancePicker(
            options = appIcons.map { appIcon ->
                val isLocked = !appIcon.isValid(signInState)
                val isSelected = appIcon == selectedAppIcon
                AppearanceOption(
                    imageResId = appIcon.settingsIcon,
                    label = stringResource(appIcon.labelId),
                    imageContentDescription = stringResource(LR.string.settings_app_icon),
                    badgeResId = when {
                        isLocked -> when (appIcon.tier) {
                            SubscriptionTier.Plus -> R.drawable.ic_locked_plus
                            SubscriptionTier.Patron -> R.drawable.ic_locked_patron
                            null -> null
                        }

                        isSelected -> R.drawable.ic_circle_tick

                        else -> null
                    },
                    isSelected = isSelected,
                    isLocked = isLocked,
                )
            },
            isTall = false,
            endPadding = 16.dp,
            scrollRequest = appIconScrollRequest,
            onClick = { index -> onAppIconClick(appIcons[index]) },
        )
    }

    private fun AppIcon.AppIconType.isValid(signInState: SignInState?) = when (tier) {
        SubscriptionTier.Plus -> signInState?.isSignedInAsPlusOrPatron == true
        SubscriptionTier.Patron -> signInState?.isSignedInAsPatron == true
        null -> true
    }

    private fun onThemeClick(afterThemeType: Theme.ThemeType) {
        val beforeThemeType = selectedTheme ?: return
        val validTheme = !afterThemeType.isPlus || signInState?.isSignedInAsPlusOrPatron == true
        if (validTheme) {
            selectedTheme = afterThemeType
            (activity as? AppCompatActivity)?.let {
                theme.updateTheme(it, afterThemeType)
                binding?.swtSystemTheme?.isChecked = theme.getUseSystemTheme() // Update switch if changing the theme updated the setting
                viewModel.onThemeChanged(afterThemeType)
            }
        } else {
            viewModel.updateChangeThemeType(Pair(beforeThemeType, afterThemeType))
            openOnboardingFlow(source = OnboardingUpgradeSource.THEMES)
            scrollToCurrentTheme()
        }
    }

    private fun onAppIconClick(afterAppIconType: AppIcon.AppIconType) {
        val beforeAppIconType = selectedAppIcon ?: return
        selectedAppIcon = afterAppIconType
        if (afterAppIconType.isValid(signInState)) {
            viewModel.updateGlobalIcon(afterAppIconType)

            AlertDialog.Builder(requireContext())
                .setTitle(LR.string.settings_app_icon_updated)
                .setMessage(LR.string.settings_app_icon_updated_message_v2)
                .setPositiveButton(LR.string.settings_app_icon_ok, null)
                .show()
        } else {
            viewModel.updateChangeAppIconType(Pair(beforeAppIconType, afterAppIconType))
            openOnboardingFlow(afterAppIconType.tier, source = OnboardingUpgradeSource.ICONS)
        }
    }

    private fun showEpisodeArtworkConfigurationFragment() {
        (activity as? FragmentHostListener)?.addFragment(EpisodeArtworkConfigurationFragment())
    }

    private fun refreshArtwork() {
        viewModel.onRefreshArtwork()
        val activity = activity ?: return
        AlertDialog.Builder(activity)
            .setTitle(LR.string.settings_refresh_artwork_title)
            .setMessage(LR.string.settings_refresh_artwork_message)
            .setPositiveButton(LR.string.settings_refresh_artwork_ok, null)
            .show()
        RefreshArtworkWorker.start(activity)
    }
}

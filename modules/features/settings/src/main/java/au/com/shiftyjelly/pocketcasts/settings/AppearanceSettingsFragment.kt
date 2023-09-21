package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.settings.databinding.FragmentSettingsAppearanceBinding
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.SettingsAppearanceState
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.SettingsAppearanceViewModel
import au.com.shiftyjelly.pocketcasts.ui.worker.RefreshArtworkWorker
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
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

        binding.themeRecyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        binding.appIconRecyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)

        binding.themeRecyclerView.doOnLayout { scrollToCurrentTheme() }
        binding.appIconRecyclerView.doOnLayout { scrollToCurrentAppIcon() }

        viewModel.createAccountState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SettingsAppearanceState.ThemesAndIconsLoading -> {}
                is SettingsAppearanceState.ThemesAndIconsLoaded -> {
                    val mainWidth = activity?.resources?.displayMetrics?.widthPixels // Display metrics gives the app size not the display, it's badly named. Works in chromebooks and split screen
                    val isSignedInAsPlusOrPatron = viewModel.signInState.value?.isSignedInAsPlusOrPatron ?: false
                    binding.themeRecyclerView.adapter = AppearanceThemeSettingsAdapter(mainWidth, isSignedInAsPlusOrPatron, state.currentThemeType, state.themeList) { beforeThemeType, afterThemeType, validTheme ->
                        if (validTheme) {
                            (activity as? AppCompatActivity)?.let {
                                theme.updateTheme(it, afterThemeType)
                                binding.swtSystemTheme.isChecked = theme.getUseSystemTheme() // Update switch if changing the theme updated the setting
                                viewModel.onThemeChanged(afterThemeType)
                            }
                        } else {
                            viewModel.updateChangeThemeType(Pair(beforeThemeType, afterThemeType))
                            openOnboardingFlow()
                            scrollToCurrentTheme()
                        }
                    }
                    binding.themeRecyclerView.setHasFixedSize(true)
                    scrollToCurrentTheme()

                    binding.appIconRecyclerView.adapter = AppearanceIconSettingsAdapter(mainWidth, viewModel.signInState.value, state.currentAppIcon, state.iconList) { beforeAppIconType, afterAppIconType, validIcon ->
                        if (validIcon) {
                            viewModel.updateGlobalIcon(afterAppIconType)

                            AlertDialog.Builder(binding.appIconRecyclerView.context)
                                .setTitle(LR.string.settings_app_icon_updated)
                                .setMessage(LR.string.settings_app_icon_updated_message)
                                .setPositiveButton(LR.string.settings_app_icon_ok, null)
                                .show()
                        } else {
                            viewModel.updateChangeAppIconType(Pair(beforeAppIconType, afterAppIconType))
                            openOnboardingFlow(afterAppIconType.tier)
                        }
                    }
                    binding.appIconRecyclerView.setHasFixedSize(true)
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
                        (binding.themeRecyclerView.adapter as? AppearanceThemeSettingsAdapter)?.updateTheme(beforeThemeType)
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
                            .setMessage(LR.string.settings_app_icon_updated_message)
                            .setPositiveButton(LR.string.settings_app_icon_ok, null)
                            .show()
                    } else {
                        (binding.appIconRecyclerView.adapter as? AppearanceIconSettingsAdapter)?.updateAppIcon(beforeAppIconType)
                        scrollToCurrentAppIcon()
                    }
                }
            }

            (binding.themeRecyclerView.adapter as? AppearanceThemeSettingsAdapter)?.updatePlusSignedIn(signInState.isSignedInAsPlusOrPatron)
            (binding.appIconRecyclerView.adapter as? AppearanceIconSettingsAdapter)?.updatePlusSignedIn(signInState)
            binding.upgradeGroup.isVisible = !signInState.isSignedInAsPlusOrPatron && !settings.getUpgradeClosedAppearSettings()
        }

        viewModel.loadThemesAndIcons()

        binding.swtSystemTheme.isChecked = theme.getUseSystemTheme()
        binding.swtSystemTheme.setOnCheckedChangeListener { _, isChecked ->
            viewModel.useAndroidLightDarkMode(isChecked, activity as? AppCompatActivity)
        }

        binding.swtShowArtwork.isChecked = viewModel.showArtworkOnLockScreen.value
        binding.swtShowArtwork.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateShowArtworkOnLockScreen(isChecked)
        }
        binding.btnShowArtwork.setOnClickListener {
            binding.swtShowArtwork.isChecked = !binding.swtShowArtwork.isChecked
        }

        binding.swtUseEmbeddedArtwork.isChecked = viewModel.useEmbeddedArtwork.value
        binding.swtUseEmbeddedArtwork.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateUseEmbeddedArtwork(isChecked)
        }
        binding.btnUseEmbeddedArtwork.setOnClickListener {
            binding.swtUseEmbeddedArtwork.isChecked = !binding.swtUseEmbeddedArtwork.isChecked
        }

        binding.lblRefreshAllPodcastArtwork.setOnClickListener {
            refreshArtwork()
        }

        binding.upgradeBannerBackground.setOnClickListener {
            openOnboardingFlow()
        }

        binding.btnCloseUpgrade.setOnClickListener {
            settings.setUpgradeClosedAppearSettings(true)
            binding.upgradeGroup.isVisible = false
        }

        viewModel.onShown()
    }

    private fun openOnboardingFlow(tier: SubscriptionTier? = null) {
        val onboardingFlow = tier?.takeIf { tier == SubscriptionTier.PATRON }?.let {
            OnboardingFlow.Upsell(source = OnboardingUpgradeSource.APPEARANCE, showPatronOnly = true)
        } ?: OnboardingFlow.Upsell(OnboardingUpgradeSource.APPEARANCE)
        OnboardingLauncher.openOnboardingFlow(activity, onboardingFlow)
    }

    private fun scrollToCurrentTheme() {
        (binding?.themeRecyclerView?.adapter as? AppearanceThemeSettingsAdapter)?.let { adapter ->
            val selectedIndex = adapter.selectedThemeIndex() ?: 0
            binding?.themeRecyclerView?.scrollToPosition(selectedIndex)
        }
    }

    private fun scrollToCurrentAppIcon() {
        (binding?.appIconRecyclerView?.adapter as? AppearanceIconSettingsAdapter)?.let { adapter ->
            val selectedIndex = adapter.selectedIconIndex() ?: 0
            binding?.appIconRecyclerView?.scrollToPosition(selectedIndex)
        }
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

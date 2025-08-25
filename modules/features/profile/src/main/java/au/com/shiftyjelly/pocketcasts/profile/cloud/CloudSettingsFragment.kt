package au.com.shiftyjelly.pocketcasts.profile.cloud

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.profile.R
import au.com.shiftyjelly.pocketcasts.profile.databinding.FragmentCloudSettingsBinding
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class CloudSettingsFragment : BaseFragment() {
    companion object {
        fun newInstance(): CloudSettingsFragment {
            return CloudSettingsFragment()
        }
    }

    @Inject lateinit var settings: Settings

    private val viewModel by viewModels<CloudSettingsViewModel>()
    private var binding: FragmentCloudSettingsBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.onShown()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCloudSettingsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.clear()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        binding.toolbar.setup(title = getString(LR.string.profile_cloud_settings_title), navigationIcon = BackArrow, activity = activity, theme = theme)

        context?.let { context ->
            AppCompatResources.getDrawable(context, R.drawable.ic_lock)?.let {
                val color0 = ContextCompat.getColor(context, UR.color.plus_gold_dark)
                val color1 = ContextCompat.getColor(context, UR.color.plus_gold_light)
                binding.imgLock.setup(it, color0, color1)
            }
        }

        val lblDeleteCloudFileAfterPlaying = binding.lblDeleteCloudFileAfterPlaying
        val swtDeleteCloudFileAfterPlaying = binding.swtDeleteCloudFileAfterPlaying
        viewModel.signInState.observe(viewLifecycleOwner) { signInState ->
            lblDeleteCloudFileAfterPlaying.isVisible = signInState.isSignedInAsPlusOrPatron
            swtDeleteCloudFileAfterPlaying.isVisible = signInState.isSignedInAsPlusOrPatron

            binding.plusLayout.isEnabled = signInState.isSignedInAsPlusOrPatron
            binding.plusLayout.alpha = if (signInState.isSignedInAsPlusOrPatron) 1.0f else 0.5f
            binding.imgLock.isVisible = !signInState.isSignedInAsPlusOrPatron
            binding.btnLock.isVisible = !signInState.isSignedInAsPlusOrPatron

            binding.swtAutoUploadToCloud.isEnabled = signInState.isSignedInAsPlusOrPatron
            binding.swtAutoDownloadFromCloud.isEnabled = signInState.isSignedInAsPlusOrPatron
            binding.swtCloudOnlyOnWiFi.isEnabled = signInState.isSignedInAsPlusOrPatron

            binding.upgradeLayout.isVisible = !signInState.isSignedInAsPlusOrPatron && !settings.getUpgradeClosedCloudSettings()
        }

        with(binding.swtAutoAddToUpNext) {
            isChecked = settings.cloudAddToUpNext.value
            setOnCheckedChangeListener { _, isChecked ->
                viewModel.setAddToUpNext(isChecked)
            }
        }
        with(binding.swtDeleteLocalFileAfterPlaying) {
            isChecked = settings.deleteLocalFileAfterPlaying.value
            setOnCheckedChangeListener { _, isChecked ->
                viewModel.setDeleteLocalFileAfterPlaying(isChecked)
            }
        }
        with(binding.swtDeleteCloudFileAfterPlaying) {
            isChecked = settings.deleteCloudFileAfterPlaying.value
            setOnCheckedChangeListener { _, isChecked ->
                viewModel.setDeleteCloudFileAfterPlaying(isChecked)
            }
        }
        with(binding.swtAutoUploadToCloud) {
            isChecked = settings.cloudAutoUpload.value
            setOnCheckedChangeListener { _, isChecked ->
                viewModel.setCloudAutoUpload(isChecked)
            }
        }
        with(binding.swtAutoDownloadFromCloud) {
            isChecked = settings.cloudAutoDownload.value
            setOnCheckedChangeListener { _, isChecked ->
                viewModel.setCloudAutoDownload(isChecked)
            }
        }
        with(binding.swtCloudOnlyOnWiFi) {
            isChecked = settings.cloudDownloadOnlyOnWifi.value
            setOnCheckedChangeListener { _, isChecked ->
                viewModel.setCloudOnlyWifi(isChecked)
            }
        }

        binding.btnClose.setOnClickListener {
            viewModel.onUpgradeBannerDismissed(SourceView.FILES_SETTINGS)
            settings.setUpgradeClosedCloudSettings(true)
            binding.upgradeLayout.isVisible = false
        }

        listOf(
            binding.btnLock,
            binding.imgLogo,
            binding.lblGetMore,
            binding.lblFindMore,
        ).forEach {
            it.setOnClickListener { openUpgradeSheet() }
        }

        val scrollContainer = binding.scrollContainer
        val initialPadding = scrollContainer.paddingBottom
        viewLifecycleOwner.lifecycleScope.launch {
            settings.bottomInset
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { inset -> scrollContainer.updatePadding(bottom = initialPadding + inset) }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.onFragmentPause(activity?.isChangingConfigurations)
    }

    private fun openUpgradeSheet() {
        OnboardingLauncher.openOnboardingFlow(
            requireActivity(),
            OnboardingFlow.Upsell(OnboardingUpgradeSource.FILES),
        )
    }
}

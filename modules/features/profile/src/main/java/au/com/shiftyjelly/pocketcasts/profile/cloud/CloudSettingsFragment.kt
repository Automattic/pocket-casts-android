package au.com.shiftyjelly.pocketcasts.profile.cloud

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.profile.R
import au.com.shiftyjelly.pocketcasts.profile.databinding.FragmentCloudSettingsBinding
import au.com.shiftyjelly.pocketcasts.settings.plus.PlusUpgradeFragment
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class CloudSettingsFragment : BaseFragment() {
    companion object {
        fun newInstance(): CloudSettingsFragment {
            return CloudSettingsFragment()
        }
    }

    @Inject lateinit var settings: Settings

    private val viewModel: AddFileViewModel by viewModels()
    private var binding: FragmentCloudSettingsBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCloudSettingsBinding.inflate(inflater, container, false)
        return binding?.root
    }

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
                val color0 = ContextCompat.getColor(context, R.color.plus_gold_dark)
                val color1 = ContextCompat.getColor(context, R.color.plus_gold_light)
                binding.imgLock.setup(it, color0, color1)
            }
        }

        val lblDeleteCloudFileAfterPlaying = binding.lblDeleteCloudFileAfterPlaying
        val swtDeleteCloudFileAfterPlaying = binding.swtDeleteCloudFileAfterPlaying
        viewModel.signInState.observe(viewLifecycleOwner) { signInState ->
            lblDeleteCloudFileAfterPlaying.isVisible = signInState.isSignedInAsPlus
            swtDeleteCloudFileAfterPlaying.isVisible = signInState.isSignedInAsPlus

            binding.plusLayout.isEnabled = signInState.isSignedInAsPlus
            binding.plusLayout.alpha = if (signInState.isSignedInAsPlus) 1.0f else 0.5f
            binding.imgLock.isVisible = !signInState.isSignedInAsPlus
            binding.btnLock.isVisible = !signInState.isSignedInAsPlus

            binding.swtAutoUploadToCloud.isEnabled = signInState.isSignedInAsPlus
            binding.swtAutoDownloadFromCloud.isEnabled = signInState.isSignedInAsPlus
            binding.swtCloudOnlyOnWiFi.isEnabled = signInState.isSignedInAsPlus

            binding.upgradeLayout.isVisible = !signInState.isSignedInAsPlus && !settings.getUpgradeClosedCloudSettings()
        }

        with(binding.swtAutoAddToUpNext) {
            isChecked = settings.getCloudAddToUpNext()
            setOnCheckedChangeListener { _, isChecked ->
                settings.setCloudAddToUpNext(isChecked)
            }
        }
        with(binding.swtDeleteLocalFileAfterPlaying) {
            isChecked = settings.getCloudDeleteAfterPlaying()
            setOnCheckedChangeListener { _, isChecked ->
                settings.setCloudDeleteAfterPlaying(isChecked)
            }
        }
        with(binding.swtDeleteCloudFileAfterPlaying) {
            isChecked = settings.getCloudDeleteCloudAfterPlaying()
            setOnCheckedChangeListener { _, isChecked ->
                settings.setCloudDeleteCloudAfterPlaying(isChecked)
            }
        }
        with(binding.swtAutoUploadToCloud) {
            isChecked = settings.getCloudAutoUpload()
            setOnCheckedChangeListener { _, isChecked ->
                settings.setCloudAutoUpload(isChecked)
            }
        }
        with(binding.swtAutoDownloadFromCloud) {
            isChecked = settings.getCloudAutoDownload()
            setOnCheckedChangeListener { _, isChecked ->
                settings.setCloudAutoDownload(isChecked)
            }
        }
        with(binding.swtCloudOnlyOnWiFi) {
            isChecked = settings.getCloudOnlyWifi()
            setOnCheckedChangeListener { _, isChecked ->
                settings.setCloudOnlyWifi(isChecked)
            }
        }

        binding.btnClose.setOnClickListener {
            settings.setUpgradeClosedCloudSettings(true)
            binding.upgradeLayout.isVisible = false
        }

        binding.btnLock.setOnClickListener {
            openUpgradeBottomSheet()
        }

        binding.lblFindMore.setOnClickListener {
            openUpgradeBottomSheet()
        }
    }

    private fun openUpgradeBottomSheet() {
        val bottomSheet = PlusUpgradeFragment.newInstance(featureBlocked = true)
        activity?.supportFragmentManager?.let {
            bottomSheet.show(it, "upgrade_bottom_sheet")
        }
    }
}

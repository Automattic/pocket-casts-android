package au.com.shiftyjelly.pocketcasts.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.repositories.file.StorageOptions
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.StorageSettingsViewModel
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StorageSettingsFragment : BaseFragment() {
    private val viewModel: StorageSettingsViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result ->
        if (result) viewModel.onPermissionGrantedStorage()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme(theme.activeTheme) {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    StorageSettingsPage(
                        viewModel = viewModel,
                        onBackPressed = { activity?.onBackPressed() }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.permissionRequest.collect { permissionRequest ->
                if (permissionRequest == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                    requestPermissionLauncher.launch(permissionRequest)
                }
            }
        }

        viewModel.start(
            folderLocations = ::getFileLocations,
            permissionGranted = ::permissionGranted
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.onFragmentResume()
    }

    private fun getFileLocations() = StorageOptions().getFolderLocations(activity)

    private fun permissionGranted() =
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

    /*
    HasBackstack {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_storage, rootKey)
        findPreference<Preference>("manualCleanup")?.setOnPreferenceClickListener { _ ->
            showDownloadedFiles()
            true
        }
    }

    private fun showDownloadedFiles() {
        val fragment = ManualCleanupFragment.newInstance()
        childFragmentManager.beginTransaction()
            .replace(UR.id.frameChildFragment, fragment)
            .addToBackStack("podcastSelect")
            .commit()
    }

    override fun onBackPressed(): Boolean {
        if (childFragmentManager.backStackEntryCount > 0) {
            childFragmentManager.popBackStack()
            return true
        }

        return false
    }

    override fun getBackstackCount(): Int {
        return childFragmentManager.backStackEntryCount
    }
    */
}

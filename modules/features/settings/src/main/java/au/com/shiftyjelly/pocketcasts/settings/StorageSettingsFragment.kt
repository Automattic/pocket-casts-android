package au.com.shiftyjelly.pocketcasts.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.file.StorageOptions
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.StorageSettingsViewModel
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.utils.isDeviceRunningOnLowStorage
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.lowstorage.LowStorageBottomSheetListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StorageSettingsFragment : BaseFragment() {
    private val viewModel: StorageSettingsViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { result ->
        if (result) viewModel.onPermissionGrantedStorage()
    }

    @Inject
    lateinit var settings: Settings

    private var lowStorageListener: LowStorageBottomSheetListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        lowStorageListener = context as? LowStorageBottomSheetListener
    }

    override fun onDetach() {
        super.onDetach()
        lowStorageListener = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        AppThemeWithBackground(theme.activeTheme) {
            val bottomInset = settings.bottomInset.collectAsStateWithLifecycle(0)
            StorageSettingsPage(
                viewModel = viewModel,
                onBackPressed = {
                    @Suppress("DEPRECATION")
                    activity?.onBackPressed()
                },
                onManageDownloadedFilesClick = { (activity as? FragmentHostListener)?.addFragment(ManualCleanupFragment.newInstance()) },
                bottomInset = bottomInset.value.pxToDp(LocalContext.current).dp,
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (isDeviceRunningOnLowStorage()) {
                    lowStorageListener?.showModal(SourceView.STORAGE_AND_DATA_USAGE)
                }
                viewModel.permissionRequest.collect { permissionRequest ->
                    if (permissionRequest == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                        requestPermissionLauncher.launch(permissionRequest)
                    }
                }
            }
        }

        viewModel.start(
            folderLocations = ::getFileLocations,
            permissionGranted = ::permissionGranted,
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.onFragmentResume()
    }

    private fun getFileLocations() = StorageOptions().getFolderLocations(requireActivity())

    private fun permissionGranted() =
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
}

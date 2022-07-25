package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.repositories.file.StorageOptions
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.StorageSettingsViewModel
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StorageSettingsFragment : BaseFragment() {
    private val viewModel: StorageSettingsViewModel by viewModels()

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
        viewModel.start(
            folderLocations = ::getFileLocations
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.onFragmentResume()
    }

    private fun getFileLocations() = StorageOptions().getFolderLocations(activity)

    /*
    PreferenceFragmentCompat(),
    Preference.OnPreferenceChangeListener,
    CoroutineScope,
    HasBackstack {

    @Inject lateinit var settings: Settings
    @Inject lateinit var fileStorage: FileStorage

    private var storageFolderPreference: EditTextPreference? = null

    private var foldersAvailable: List<FolderLocation>? = null
    private var permissionRequestedForPath: String? = null

    companion object {
        const val PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 241
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_storage, rootKey)
        storageFolderPreference = preferenceManager.findPreference(Settings.PREFERENCE_STORAGE_CUSTOM_FOLDER)

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

    private fun changeStorageLabels() {
        val storageFolderPreference = storageFolderPreference ?: return

        if (settings.usingCustomFolderStorage()) {
            storageFolderPreference.summary = settings.getStorageCustomFolder()
        } else {
            storageFolderPreference.summary = getString(LR.string.settings_storage_using, settings.getStorageChoiceName())
        }

        // Custom Folder Location shown?
        findPreference<PreferenceCategory>("storageTitle")?.let {
            if (settings.usingCustomFolderStorage()) {
                it.addPreference(storageFolderPreference)
            } else {
                it.removePreference(storageFolderPreference)
            }
        }
    }

    fun onPermissionGrantedStorage() {
        val storageFolderPreference = storageFolderPreference ?: return
        val path = permissionRequestedForPath
        if (path != null && path.isNotBlank()) {
            if (onPreferenceChange(storageFolderPreference, path)) {
                storageFolderPreference.text = path
                settings.setStorageCustomFolder(path)
            }
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        if (preference.key == storageFolderPreference?.key) {
            val newPath = newValue as String

            if (StringUtil.isBlank(newPath)) {
                return false
            }
            var oldDirectory: File? = null
            try {
                oldDirectory = fileStorage.baseStorageDirectory
            } catch (e: StorageException) {
                // ignore error
            }

            // validate the path
            if (StringUtil.isBlank(newPath)) {
                UiUtil.displayAlertError(activity, getString(LR.string.settings_storage_folder_blank), null)
                return false
            }

            val activity = activity
            if (activity != null && ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)
                permissionRequestedForPath = newPath
                return false
            }

            val newDirectory = File(newPath)
            if (!newDirectory.exists()) {
                val success = newDirectory.mkdirs()
                if (!success && !newDirectory.exists()) {
                    UiUtil.displayAlertError(activity, getString(LR.string.settings_storage_folder_not_found), null)
                    return false
                }
            }

            if (!newDirectory.canWrite()) {
                UiUtil.displayAlertError(activity, getString(LR.string.settings_storage_folder_write_failed), null)
                return false
            }

            // move the podcasts if the user wants
            if (oldDirectory != null) {
                movePodcastStorage(oldDirectory.absolutePath, newDirectory.absolutePath)
            }

            return true
        }

        return false
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

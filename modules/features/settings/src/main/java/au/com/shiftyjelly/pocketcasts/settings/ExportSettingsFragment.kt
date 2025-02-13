package au.com.shiftyjelly.pocketcasts.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.opml.OpmlImportTask
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.ServiceManager
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.ExportSettingsViewModel
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.extensions.findToolbar
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import au.com.shiftyjelly.pocketcasts.views.helper.OpmlExporter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class ExportSettingsFragment : PreferenceFragmentCompat() {

    @Inject lateinit var serviceManager: ServiceManager

    @Inject lateinit var settings: Settings

    @Inject lateinit var podcastManager: PodcastManager

    @Inject lateinit var theme: Theme

    @Inject lateinit var syncManager: SyncManager

    @Inject @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    private val viewModel by viewModels<ExportSettingsViewModel>()
    private var exporter: OpmlExporter? = null

    private val importOpmlFilePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val activity = activity ?: return@registerForActivityResult
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data?.data ?: return@registerForActivityResult
            OpmlImportTask.run(data, activity)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.onCreate()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findToolbar().setup(title = getString(LR.string.settings_title_import_export), navigationIcon = BackArrow, activity = activity, theme = theme)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                settings.bottomInset.collect {
                    view.updatePadding(bottom = it)
                }
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_export, rootKey)

        val activity = activity ?: return

        findPreference<Preference>("importPodcasts")?.setOnPreferenceClickListener {
            showOpmlFilePicker()
            viewModel.onImportSelectFile()
            true
        }

        findPreference<EditTextPreference>("importPodcastsByUrl")?.apply {
            setOnPreferenceClickListener {
                viewModel.onImportByUrlClicked()
                false
            }
            setOnPreferenceChangeListener { _, newValue ->
                val url = newValue.toString().toHttpUrlOrNull()
                if (url != null) {
                    OpmlImportTask.run(url, activity)
                }
                false
            }
        }

        findPreference<Preference>("exportSendEmail")?.setOnPreferenceClickListener {
            viewModel.onExportByEmail()
            exporter = OpmlExporter(
                fragment = this@ExportSettingsFragment,
                serviceManager = serviceManager,
                podcastManager = podcastManager,
                syncManager = syncManager,
                context = activity,
                analyticsTracker = viewModel.analyticsTracker,
                applicationScope = applicationScope,
            ).apply {
                sendEmail()
            }
            true
        }

        findPreference<Preference>("exportSaveFile")?.setOnPreferenceClickListener {
            viewModel.onExportFile()
            exporter = OpmlExporter(
                fragment = this@ExportSettingsFragment,
                serviceManager = serviceManager,
                podcastManager = podcastManager,
                syncManager = syncManager,
                context = activity,
                analyticsTracker = viewModel.analyticsTracker,
                applicationScope = applicationScope,
            ).apply {
                saveFile()
            }
            true
        }
    }

    private fun showOpmlFilePicker() {
        val intent = Intent().apply {
            type = "*/*"
            action = Intent.ACTION_GET_CONTENT
        }
        importOpmlFilePickerLauncher.launch(Intent.createChooser(intent, getString(LR.string.settings_import_choose_file)))
    }

    override fun onPause() {
        super.onPause()
        viewModel.onFragmentPause(activity?.isChangingConfigurations)
    }
}

package au.com.shiftyjelly.pocketcasts.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.opml.OpmlImportTask
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.ServerManager
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.ExportSettingsViewModel
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.extensions.findToolbar
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import au.com.shiftyjelly.pocketcasts.views.helper.OpmlExporter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class ExportSettingsFragment : PreferenceFragmentCompat() {

    @Inject lateinit var serverManager: ServerManager
    @Inject lateinit var settings: Settings
    @Inject lateinit var podcastManager: PodcastManager
    @Inject lateinit var theme: Theme

    private val viewModel by viewModels<ExportSettingsViewModel>()
    private var exporter: OpmlExporter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.onCreate()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findToolbar().setup(title = getString(LR.string.settings_title_import_export), navigationIcon = BackArrow, activity = activity, theme = theme)
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
                val url = newValue.toString()
                if (url.isNotBlank()) {
                    OpmlImportTask.run(url, activity)
                }
                false
            }
        }

        findPreference<Preference>("exportSendEmail")?.setOnPreferenceClickListener {
            exporter = OpmlExporter(this@ExportSettingsFragment, serverManager, podcastManager, settings, activity).apply {
                sendEmail()
            }
            viewModel.onExportByEmail()
            true
        }

        findPreference<Preference>("exportSaveFile")?.setOnPreferenceClickListener {
            exporter = OpmlExporter(this@ExportSettingsFragment, serverManager, podcastManager, settings, activity).apply {
                saveFile()
            }
            viewModel.onExportFile()
            true
        }
    }

    @Suppress("DEPRECATION")
    private fun showOpmlFilePicker() {
        val intent = Intent().apply {
            type = "*/*"
            action = Intent.ACTION_GET_CONTENT
        }
        startActivityForResult(Intent.createChooser(intent, getString(LR.string.settings_import_choose_file)), IMPORT_PICKER_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        val activity = activity
        if (activity == null || resultCode != Activity.RESULT_OK || resultData == null) {
            return
        }

        if (requestCode == IMPORT_PICKER_REQUEST_CODE) {
            val data = resultData.data ?: return
            OpmlImportTask.run(data, activity)
        } else if (requestCode == OpmlExporter.EXPORT_PICKER_REQUEST_CODE) {
            val data = resultData.data ?: return
            exporter?.exportToUri(data)
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.onFragmentPause(activity?.isChangingConfigurations)
    }

    companion object {
        private const val IMPORT_PICKER_REQUEST_CODE = 42
    }
}

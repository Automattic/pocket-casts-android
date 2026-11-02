package au.com.shiftyjelly.pocketcasts.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.opml.OpmlImportTask
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.ServiceManager
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.ExportSettingsViewModel
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.helper.OpmlExporter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val IMPORT_URL_DIALOG_TAG = "import_url_dialog"

@AndroidEntryPoint
class ExportSettingsFragment : Fragment() {

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = contentWithoutConsumedInsets {
        val bottomInsetPx by settings.bottomInset.collectAsStateWithLifecycle(0)
        AppTheme(theme.activeTheme) {
            ExportSettingsPage(
                bottomInset = with(LocalDensity.current) { bottomInsetPx.toDp() },
                onNavigationClick = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                onImportFileClick = {
                    showOpmlFilePicker()
                    viewModel.onImportSelectFile()
                },
                onImportUrlClick = {
                    viewModel.onImportByUrlClicked()
                    ImportOpmlUrlDialogFragment().show(childFragmentManager, IMPORT_URL_DIALOG_TAG)
                },
                onExportEmailClick = {
                    viewModel.onExportByEmail()
                    exporter = createExporter().apply { sendEmail() }
                },
                onExportFileClick = {
                    viewModel.onExportFile()
                    exporter = createExporter().apply { saveFile() }
                },
                toolbarModifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onLongPress = { theme.toggleDarkLightThemeActivity(requireActivity() as AppCompatActivity) })
                },
            )
        }
    }

    private fun createExporter() = OpmlExporter(
        fragment = this,
        serviceManager = serviceManager,
        podcastManager = podcastManager,
        syncManager = syncManager,
        context = requireActivity(),
        eventHorizon = viewModel.eventHorizon,
        applicationScope = applicationScope,
    )

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

package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.compose.components.SelectedOptionColumn
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveInactive
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastSettingsViewModel
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal class ArchiveAfterInactiveFragment : BaseDialogFragment() {
    private val viewModel by viewModels<PodcastSettingsViewModel>({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        DialogBox(
            fillMaxHeight = false,
            modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection()),
        ) {
            val uiState by viewModel.uiState.collectAsState()
            val podcast = uiState?.podcast

            SelectedOptionColumn(
                title = getString(LR.string.settings_auto_archive_inactive),
                options = AutoArchiveInactive.All,
                selectedOption = podcast?.autoArchiveInactive,
                optionLabel = { option -> stringResource(option.stringRes) },
                onClickOption = { option ->
                    viewModel.changeAutoArchiveAfterInactive(option)
                    dismiss()
                },
                modifier = Modifier.navigationBarsPadding(),
            )
        }
    }
}

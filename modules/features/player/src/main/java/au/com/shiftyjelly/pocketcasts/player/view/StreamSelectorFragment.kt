package au.com.shiftyjelly.pocketcasts.player.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.bottomsheet.Pill
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.player.viewmodel.StreamSelectorViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.StreamSelectorViewModel.StreamKind
import au.com.shiftyjelly.pocketcasts.player.viewmodel.StreamSelectorViewModel.StreamOption
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class StreamSelectorFragment : BaseDialogFragment() {

    private val viewModel by viewModels<StreamSelectorViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        DialogBox(fillMaxHeight = false) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            StreamSelectorContent(
                options = uiState.options,
                onSelect = { option ->
                    viewModel.selectStream(option)
                    dismiss()
                },
            )
        }
    }

    companion object {
        private const val TAG = "stream_selector"

        fun show(fragmentManager: FragmentManager) {
            if (!fragmentManager.isStateSaved && fragmentManager.findFragmentByTag(TAG) == null) {
                StreamSelectorFragment().show(fragmentManager, TAG)
            }
        }
    }
}

@Composable
private fun StreamSelectorContent(
    options: List<StreamOption>,
    onSelect: (StreamOption) -> Unit,
) {
    Column {
        Spacer(modifier = Modifier.height(6.dp))
        Pill()
        TextH30(
            text = stringResource(LR.string.stream_selector_title),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        options.forEach { option ->
            StreamRow(option = option, onClick = { onSelect(option) })
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StreamRow(
    option: StreamOption,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        TextP40(
            text = option.label(),
            color = MaterialTheme.theme.colors.primaryText01,
            modifier = Modifier.weight(1f),
        )
        if (option.isSelected) {
            Icon(
                painter = painterResource(IR.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.theme.colors.primaryInteractive01,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun StreamOption.label(): String = when (kind) {
    StreamKind.Hls -> stringResource(LR.string.stream)
    StreamKind.Audio -> stringResource(LR.string.audio)
    StreamKind.Video -> height?.let { "${stringResource(LR.string.video)} · ${it}p" } ?: stringResource(LR.string.video)
    StreamKind.Other -> stringResource(LR.string.stream)
}

package au.com.shiftyjelly.pocketcasts.player.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.compose.components.SelectedOptionColumn
import au.com.shiftyjelly.pocketcasts.models.type.UpNextSortType
import au.com.shiftyjelly.pocketcasts.player.viewmodel.UpNextViewModel
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal class UpNextSortFragment : BaseDialogFragment() {
    private val viewModel by viewModels<UpNextViewModel>({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        DialogBox(
            fillMaxHeight = false,
            modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection()),
        ) {
            val options = if (FeatureFlag.isEnabled(Feature.UP_NEXT_DURATION)) {
                UpNextSortType.entries
            } else {
                UpNextSortType.entries.filterNot {
                    it == UpNextSortType.ShortestToLongest || it == UpNextSortType.LongestToShortest
                }
            }
            SelectedOptionColumn(
                title = getString(LR.string.player_up_next_sort),
                options = options,
                selectedOption = null,
                optionLabel = { option -> stringResource(option.descriptionId) },
                onClickOption = { option ->
                    viewModel.sortUpNext(option)
                    dismiss()
                },
                modifier = Modifier.navigationBarsPadding(),
            )
        }
    }
}

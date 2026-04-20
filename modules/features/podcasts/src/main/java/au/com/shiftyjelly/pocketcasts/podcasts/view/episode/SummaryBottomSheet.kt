package au.com.shiftyjelly.pocketcasts.podcasts.view.episode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class SummaryBottomSheet : BottomSheetDialogFragment() {

    @Inject
    lateinit var theme: Theme

    companion object {
        private const val ARG_SUMMARY_TEXT = "summary_text"

        fun newInstance(summaryText: String): SummaryBottomSheet {
            return SummaryBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_SUMMARY_TEXT, summaryText)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val summaryText = arguments?.getString(ARG_SUMMARY_TEXT).orEmpty()
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme(theme.activeTheme) {
                    SummaryContent(text = summaryText)
                }
            }
        }
    }
}

@Composable
private fun SummaryContent(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Text(
            text = stringResource(LR.string.episode_summary),
            color = MaterialTheme.theme.colors.primaryText01,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Text(
            text = text,
            color = MaterialTheme.theme.colors.primaryText02,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        )
    }
}

package au.com.shiftyjelly.pocketcasts.profile.champion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.doOnLayout
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.rateUs
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.PocketCastsChampionDialogRateButtonTappedEvent
import com.automattic.eventhorizon.PocketCastsChampionDialogShownEvent
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PocketCastsChampionBottomSheetDialog : BottomSheetDialogFragment() {

    @Inject lateinit var theme: Theme

    @Inject lateinit var eventHorizon: EventHorizon

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppTheme(theme.activeTheme) {
            CallOnce {
                eventHorizon.track(PocketCastsChampionDialogShownEvent)
            }
            val context = LocalContext.current
            ChampionDialog(
                onRateClick = {
                    eventHorizon.track(PocketCastsChampionDialogRateButtonTappedEvent)
                    rateUs(context)
                },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.doOnLayout {
            val dialog = dialog as BottomSheetDialog
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
            if (bottomSheet != null) {
                BottomSheetBehavior.from(bottomSheet).run {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    peekHeight = 0
                    skipCollapsed = true
                }
            }
        }
    }
}

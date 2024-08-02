package au.com.shiftyjelly.pocketcasts.profile.champion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.doOnLayout
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.rateUs
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PocketCastsChampionBottomSheetDialog : BottomSheetDialogFragment() {

    @Inject lateinit var theme: Theme

    @Inject lateinit var analyticsTracker: AnalyticsTracker

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = context ?: throw Exception("Context not found")
        return ComposeView(context).apply {
            setContent {
                AppTheme(theme.activeTheme) {
                    CallOnce {
                        analyticsTracker.track(AnalyticsEvent.POCKET_CASTS_CHAMPION_DIALOG_SHOWN)
                    }
                    ChampionDialog(
                        onRateClick = {
                            analyticsTracker.track(AnalyticsEvent.POCKET_CASTS_CHAMPION_DIALOG_RATE_BUTTON_TAPPED)
                            rateUs(context)
                        },
                    )
                }
            }
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

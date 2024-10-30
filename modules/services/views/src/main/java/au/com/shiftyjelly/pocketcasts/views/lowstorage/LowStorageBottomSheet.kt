package au.com.shiftyjelly.pocketcasts.views.lowstorage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.doOnLayout
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import jakarta.inject.Inject

class LowStorageBottomSheet : BottomSheetDialogFragment() {

    @Inject
    lateinit var theme: Theme

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: android.os.Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme(theme.activeTheme) {
                    LowStorageDialog(
                        totalDownloadSize = 1000000000,
                        onManageDownloadsClick = {
                            // todo: handle this
                        },
                        onMaybeLaterClick = {
                            // todo: snooze the warning for seven days
                            dismiss()
                        },
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: android.os.Bundle?) {
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

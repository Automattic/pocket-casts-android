package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.collectAsState
import androidx.core.os.BundleCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SuggestedFoldersPaywallBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val FOLDERS_KEY = "folders_key"

        fun newInstance(folders: List<Folder>): SuggestedFoldersPaywallBottomSheet {
            return SuggestedFoldersPaywallBottomSheet().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(FOLDERS_KEY, ArrayList(folders))
                }
            }
        }
    }

    @Inject
    lateinit var theme: Theme

    private val viewModel: SuggestedFoldersPaywallViewModel by viewModels<SuggestedFoldersPaywallViewModel>()

    val suggestedFolders: ArrayList<Folder>
        get() = arguments?.let { (BundleCompat.getParcelableArrayList(it, FOLDERS_KEY, Folder::class.java)) } ?: ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        AppTheme(theme.activeTheme) {
            val signInState = viewModel.signInState.collectAsState(null)

            SuggestedFoldersPaywall(
                folders = suggestedFolders,
                onShown = {
                    viewModel.onShown()
                },
                onUseTheseFolders = {
                    if (signInState.value?.isSignedInAsPlusOrPatron == true) {
                        dismiss()
                        SuggestedFolders().show(parentFragmentManager, "suggested_folders")
                    } else {
                        OnboardingLauncher.openOnboardingFlow(activity, OnboardingFlow.Upsell(OnboardingUpgradeSource.SUGGESTED_FOLDERS))
                    }
                },
                onMaybeLater = {
                    viewModel.onMaybeLater()
                    dismiss()
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

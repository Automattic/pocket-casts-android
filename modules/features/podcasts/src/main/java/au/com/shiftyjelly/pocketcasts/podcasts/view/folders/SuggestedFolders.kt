package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SuggestedFolders : BaseDialogFragment() {

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppThemeWithBackground(theme.activeTheme) {
            SuggestedFoldersPage(
                onShown = {
                    analyticsTracker.track(AnalyticsEvent.SUGGESTED_FOLDERS_MODAL_SHOWN)
                },
                onDismiss = {
                    analyticsTracker.track(AnalyticsEvent.SUGGESTED_FOLDERS_MODAL_DISMISSED)
                    dismiss()
                },
                onUseTheseFolders = {
                    analyticsTracker.track(AnalyticsEvent.SUGGESTED_FOLDERS_MODAL_USE_THESE_FOLDERS_TAPPED)
                },
                onCreateCustomFolders = {
                    analyticsTracker.track(AnalyticsEvent.SUGGESTED_FOLDERS_MODAL_CREATE_CUSTOM_FOLDERS_TAPPED)
                    FolderCreateFragment.newInstance(source = "suggested_folders").show(parentFragmentManager, "create_folder_card")
                    dismiss()
                },
            )
        }
    }
}

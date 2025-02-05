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

    @Inject lateinit var analyticsTracker: AnalyticsTracker

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppThemeWithBackground(theme.activeTheme) {
            SuggestedFoldersPage(
                onDismiss = {
                    dismiss()
                },
                onUseTheseFolders = {},
                onCreateCustomFolders = {
                    analyticsTracker.track(AnalyticsEvent.FOLDER_CREATE_SHOWN, mapOf("source" to "suggested_folders"))
                    FolderCreateFragment().show(parentFragmentManager, "create_folder_card")
                },
            )
        }
    }
}

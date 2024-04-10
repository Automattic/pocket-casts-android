package au.com.shiftyjelly.pocketcasts.ui.helper

import android.view.View
import androidx.fragment.app.Fragment
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import com.google.android.material.bottomsheet.BottomSheetBehavior

interface FragmentHostListener {
    fun addFragment(fragment: Fragment, onTop: Boolean = false)
    fun replaceFragment(fragment: Fragment)
    fun showBottomSheet(fragment: Fragment)
    fun bottomSheetClosePressed(fragment: Fragment)
    fun openPlayer(source: String? = null)
    fun closePlayer()
    fun showModal(fragment: Fragment)
    fun closeModal(fragment: Fragment)
    fun openTab(tabId: Int)
    fun closeToRoot()
    fun closePodcastsToRoot()
    fun openPodcastPage(uuid: String)
    fun openCloudFiles()
    fun snackBarView(): View
    fun showAccountUpgradeNow(autoSelectPlus: Boolean)
    fun updateStatusBar()
    fun getPlayerBottomSheetState(): Int
    fun addPlayerBottomSheetCallback(callback: BottomSheetBehavior.BottomSheetCallback)
    fun removePlayerBottomSheetCallback(callback: BottomSheetBehavior.BottomSheetCallback)
    fun openEpisodeDialog(
        episodeUuid: String?,
        source: EpisodeViewSource,
        podcastUuid: String?,
        forceDark: Boolean,
        timestampInSecs: Int? = null,
    )
    fun lockPlayerBottomSheet(locked: Boolean)
    fun updateSystemColors()
    fun overrideNextRefreshTimer()
    fun isUpNextShowing(): Boolean
    fun showStoriesOrAccount(source: String)
    fun whatsNewDismissed(fromConfirmAction: Boolean = false)
}

package au.com.shiftyjelly.pocketcasts

import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import au.com.shiftyjelly.pocketcasts.account.CarHeaderView
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import kotlin.time.Duration
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class AutomotiveSettingsActivity :
    AppCompatActivity(),
    FragmentHostListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val header = CarHeaderView(this)
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(getThemeColor(UR.attr.primary_ui_04))
                addView(header, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                addView(FragmentContainerView(context).apply { id = R.id.frameMain }, LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            },
        )

        if (savedInstanceState == null) {
            val settingsFragment = AutomotiveSettingsFragment()
            supportFragmentManager.beginTransaction().replace(R.id.frameMain, settingsFragment).commitNowAllowingStateLoss()
        }

        header.closeButton.setImageResource(IR.drawable.ic_arrow_back)
        header.closeButton.setOnClickListener { handleBackPressed() }
    }

    override fun setSupportActionBar(toolbar: Toolbar?) {
        super.setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        handleBackPressed()
        return true
    }

    fun addFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameMain, fragment)
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }

    private fun handleBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            if (!supportFragmentManager.isStateSaved) {
                supportFragmentManager.popBackStack()
            }
            return
        }
        onBackPressedDispatcher.onBackPressed()
    }

    // TODO: Refactor FragmentHostListener in to something more generic so it can be used
    // cleaner between automotive and main
    override fun addFragment(fragment: Fragment, onTop: Boolean) {
        addFragment(fragment)
    }

    override fun replaceFragment(fragment: Fragment) {
        addFragment(fragment)
    }

    override fun showBottomSheet(fragment: Fragment) {
        addFragment(fragment)
    }

    override fun closeBottomSheet() {
        handleBackPressed()
    }

    override fun openPlayer() {
    }

    override fun closeDialogs() {
    }

    override fun closePlayer() {
    }

    override fun showModal(fragment: Fragment) {
        addFragment(fragment)
    }

    override fun closeModal(fragment: Fragment) {
        handleBackPressed()
    }

    override fun openTab(tabId: Int) {
    }

    override fun closeToRoot() {
    }

    override fun closePodcastsToRoot() {
    }

    override fun closeFiltersToRoot() {
    }

    override fun closeProfileToRoot() {
    }

    override fun openCreatePlaylist() {
    }

    override fun openNetworks() {
    }

    override fun openInAppDeepLink(url: String) = false

    override fun openPodcastPage(uuid: String, sourceView: String?) {
    }

    override fun openNetworkPage(listId: String, title: String?, sourceView: SourceView?) {
    }

    override fun openCloudFiles() {
    }

    override fun snackBarView(): View {
        return findViewById(android.R.id.content)
    }

    override fun setFullScreenDarkOverlayViewVisibility(visible: Boolean) {
    }

    override fun showAccountUpgradeNow(autoSelectPlus: Boolean) {
    }

    override fun updateStatusBar() {
    }

    override fun getPlayerBottomSheetState(): Int {
        return 0
    }

    override fun addPlayerBottomSheetCallback(callback: BottomSheetBehavior.BottomSheetCallback) = Unit

    override fun removePlayerBottomSheetCallback(callback: BottomSheetBehavior.BottomSheetCallback) = Unit

    override fun openEpisodeDialog(
        episodeUuid: String?,
        source: EpisodeViewSource,
        podcastUuid: String?,
        forceDark: Boolean,
        autoPlay: Boolean,
        startTimestamp: Duration?,
        endTimestamp: Duration?,
    ) {
    }

    override fun lockPlayerBottomSheet(locked: Boolean) {
    }

    override fun updateSystemColors() {
    }

    override fun overrideNextRefreshTimer() {
    }

    override fun isUpNextShowing(): Boolean {
        return false
    }

    override fun showStoriesOrAccount(source: String) {
    }

    override fun showPlayback2026OrAccount(source: String) {
    }

    override fun whatsNewDismissed(fromConfirmAction: Boolean) {
    }

    override fun onPlayerBackstackChanged() = Unit
}

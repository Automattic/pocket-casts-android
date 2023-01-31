package au.com.shiftyjelly.pocketcasts.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class AutoArchiveFragment : PreferenceFragmentCompat(), HasBackstack, SharedPreferences.OnSharedPreferenceChangeListener {
    @Inject lateinit var settings: Settings
    @Inject lateinit var theme: Theme
    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper

    val toolbar: Toolbar?
        get() = view?.findViewById(R.id.toolbar)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_auto_archive)
        updateStarredSummary()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar?.setup(title = getString(LR.string.settings_title_auto_archive), navigationIcon = BackArrow, activity = activity, theme = theme)
        analyticsTracker.track(AnalyticsEvent.SETTINGS_AUTO_ARCHIVE_SHOWN)
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun updateStarredSummary() {
        val starredSummary = getString(if (settings.getAutoArchiveIncludeStarred()) LR.string.settings_auto_archive_starred_summary else LR.string.settings_auto_archive_no_starred_summary)
        val preference = preferenceManager.findPreference<SwitchPreference>(Settings.AUTO_ARCHIVE_INCLUDE_STARRED)
        preference?.summary = starredSummary
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            Settings.AUTO_ARCHIVE_INCLUDE_STARRED -> {
                updateStarredSummary()
                analyticsTracker.track(
                    AnalyticsEvent.SETTINGS_AUTO_ARCHIVE_INCLUDE_STARRED_TOGGLED,
                    mapOf("enabled" to settings.getAutoArchiveIncludeStarred())
                )
            }
            Settings.AUTO_ARCHIVE_PLAYED_EPISODES_AFTER -> {
                analyticsTracker.track(
                    AnalyticsEvent.SETTINGS_AUTO_ARCHIVE_PLAYED_CHANGED,
                    mapOf(
                        "value" to when (settings.getAutoArchiveAfterPlaying()) {
                            Settings.AutoArchiveAfterPlaying.Never -> "never"
                            Settings.AutoArchiveAfterPlaying.AfterPlaying -> "after_playing"
                            Settings.AutoArchiveAfterPlaying.Hours24 -> "after_24_hours"
                            Settings.AutoArchiveAfterPlaying.Days2 -> "after_2_days"
                            Settings.AutoArchiveAfterPlaying.Weeks1 -> "after_1_week"
                        }
                    )
                )
            }
            Settings.AUTO_ARCHIVE_INACTIVE -> {
                analyticsTracker.track(
                    AnalyticsEvent.SETTINGS_AUTO_ARCHIVE_INACTIVE_CHANGED,
                    mapOf(
                        "value" to when (settings.getAutoArchiveInactive()) {
                            Settings.AutoArchiveInactive.Never -> "never"
                            Settings.AutoArchiveInactive.Hours24 -> "after_24_hours"
                            Settings.AutoArchiveInactive.Days2 -> "after_2_days"
                            Settings.AutoArchiveInactive.Weeks1 -> "after_1_week"
                            Settings.AutoArchiveInactive.Weeks2 -> "after_2_weeks"
                            Settings.AutoArchiveInactive.Days30 -> "after_30_days"
                            Settings.AutoArchiveInactive.Days90 -> "after 3 months"
                        }
                    )
                )
            }
            else -> Timber.d("Unknown preference changed: $key")
        }
    }

    override fun onBackPressed(): Boolean {
        if (childFragmentManager.backStackEntryCount > 0) {
            childFragmentManager.popBackStack()
            toolbar?.title = getString(LR.string.settings_title_auto_archive)
            return true
        }

        return false
    }

    override fun getBackstackCount(): Int {
        return childFragmentManager.backStackEntryCount
    }
}

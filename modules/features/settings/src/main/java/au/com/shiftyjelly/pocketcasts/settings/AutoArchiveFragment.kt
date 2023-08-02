package au.com.shiftyjelly.pocketcasts.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.AutoArchiveFragmentViewModel
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

    private val viewModel: AutoArchiveFragmentViewModel by viewModels()

    private lateinit var autoArchivePlayedEpisodes: ListPreference

    val toolbar: Toolbar?
        get() = view?.findViewById(R.id.toolbar)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_auto_archive)
        autoArchivePlayedEpisodes = preferenceManager.findPreference<ListPreference>("autoArchivePlayedEpisodes")!!
            .apply {
                setOnPreferenceChangeListener { _, newValue ->
                    viewModel.onPlayedEpisodesAfterChanged(newValue as String)
                    true
                }
            }
        updateStarredSummary()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar?.setup(title = getString(LR.string.settings_title_auto_archive), navigationIcon = BackArrow, activity = activity, theme = theme)
        viewModel.onViewCreated()
    }

    override fun onResume() {
        super.onResume()
        setupAutoArchiveAfterPlaying()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        viewModel.onFragmentPause(activity?.isChangingConfigurations)
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
                viewModel.onStarredChanged()
            }
            Settings.AUTO_ARCHIVE_INACTIVE -> {
                viewModel.onInactiveChanged()
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

    private fun setupAutoArchiveAfterPlaying() {
        val stringArray = resources.getStringArray(LR.array.settings_auto_archive_played_values)
        autoArchivePlayedEpisodes.value = stringArray[settings.autoArchiveAfterPlaying.flow.value.toIndex()]
    }
}

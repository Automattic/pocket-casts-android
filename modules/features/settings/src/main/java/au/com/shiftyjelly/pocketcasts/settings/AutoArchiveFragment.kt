package au.com.shiftyjelly.pocketcasts.settings

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
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class AutoArchiveFragment : PreferenceFragmentCompat(), HasBackstack {
    @Inject lateinit var settings: Settings
    @Inject lateinit var theme: Theme

    private val viewModel: AutoArchiveFragmentViewModel by viewModels()

    private lateinit var autoArchivePlayedEpisodes: ListPreference
    private lateinit var autoArchiveInactiveEpisodes: ListPreference
    private lateinit var autoArchiveIncludeStarred: SwitchPreference

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
        autoArchiveInactiveEpisodes = preferenceManager.findPreference<ListPreference>("autoArchiveInactiveEpisodes")!!
            .apply {
                setOnPreferenceChangeListener { _, newValue ->
                    viewModel.onInactiveChanged(newValue as String)
                    true
                }
            }

        autoArchiveIncludeStarred = preferenceManager.findPreference<SwitchPreference>("autoArchiveIncludeStarred")!!
            .apply {
                setOnPreferenceChangeListener { _, newValue ->
                    viewModel.onStarredChanged(newValue as Boolean)
                    updateStarredSummary()
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
        setupAutoArchiveInactive()
        setupIncludeStarred()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onFragmentPause(activity?.isChangingConfigurations)
    }

    private fun updateStarredSummary() {
        val starredSummary = getString(if (settings.autoArchiveIncludeStarred.flow.value) LR.string.settings_auto_archive_starred_summary else LR.string.settings_auto_archive_no_starred_summary)
        autoArchiveIncludeStarred.summary = starredSummary
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

    private fun setupAutoArchiveInactive() {
        val stringArray = resources.getStringArray(LR.array.settings_auto_archive_inactive_values)
        autoArchiveInactiveEpisodes.value = stringArray[settings.autoArchiveInactive.flow.value.toIndex()]
    }

    private fun setupIncludeStarred() {
        autoArchiveIncludeStarred.isChecked = settings.autoArchiveIncludeStarred.flow.value
    }
}

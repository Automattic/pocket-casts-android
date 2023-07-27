package au.com.shiftyjelly.pocketcasts

import android.os.Bundle
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.toLiveData
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import au.com.shiftyjelly.pocketcasts.localization.BuildConfig
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralSeconds
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralSecondsMinutesHoursDaysOrYears
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.extensions.setInputAsSeconds
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class AutomotiveSettingsPreferenceFragment : PreferenceFragmentCompat(), Observer<RefreshState> {

    @Inject lateinit var settings: Settings
    @Inject lateinit var podcastManager: PodcastManager

    private var preferenceRefreshNow: Preference? = null
    private var preferenceSkipForward: EditTextPreference? = null
    private var preferenceSkipBackward: EditTextPreference? = null
    private var refreshObservable: LiveData<RefreshState>? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_auto)

        preferenceSkipForward = preferenceManager.findPreference<EditTextPreference>(Settings.PREFERENCE_SKIP_FORWARD)?.apply {
            setInputAsSeconds()
        }
        preferenceSkipBackward = preferenceManager.findPreference<EditTextPreference>(Settings.PREFERENCE_SKIP_BACKWARD)?.apply {
            setInputAsSeconds()
        }

        changeSkipTitles()
        setupRefreshNow()
        setupAbout()
    }

    private fun setupAbout() {
        val preference = findPreference<Preference>("about") ?: return
        preference.summary = getString(LR.string.settings_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toString())
        preference.setOnPreferenceClickListener {
            (activity as? FragmentHostListener)?.addFragment(AutomotiveAboutFragment())
            true
        }
    }

    private fun setupRefreshNow() {
        preferenceRefreshNow = findPreference<Preference>("refresh_now")?.apply {
            setOnPreferenceClickListener {
                podcastManager.refreshPodcasts(fromLog = "Automotive")
                updateRefreshSummary(RefreshState.Refreshing)
                true
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        refreshObservable =
            settings.refreshStateObservable
                .toFlowable(BackpressureStrategy.LATEST)
                .switchMap { state ->
                    Flowable.interval(500, TimeUnit.MILLISECONDS).switchMap { Flowable.just(state) }
                }
                .toLiveData()
        refreshObservable?.observe(viewLifecycleOwner, this)

        preferenceSkipForward?.setOnPreferenceChangeListener { _, newValue ->
            val value = newValue.toString().toIntOrNull() ?: 0
            if (value > 0) {
                settings.skipForwardInSecs.run {
                    set(value)
                    needsSync = true
                }
                changeSkipTitles()
                true
            } else {
                false
            }
        }

        preferenceSkipBackward?.setOnPreferenceChangeListener { _, newValue ->
            val value = newValue.toString().toIntOrNull() ?: 0
            if (value > 0) {
                settings.setSkipBackwardInSec(value)
                settings.setSkipBackNeedsSync(true)
                changeSkipTitles()
                true
            } else {
                false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        refreshObservable?.removeObserver(this)
    }

    override fun onChanged(value: RefreshState) {
        updateRefreshSummary(value)
    }

    private fun updateRefreshSummary(state: RefreshState) {
        val status = when (state) {
            is RefreshState.Success -> {
                val time = Date().time - state.date.time
                val timeAmount = resources.getStringPluralSecondsMinutesHoursDaysOrYears(time)
                getString(LR.string.profile_last_refresh, timeAmount)
            }
            is RefreshState.Never -> getString(LR.string.profile_refreshed_never)
            is RefreshState.Refreshing -> getString(LR.string.profile_refreshing)
            is RefreshState.Failed -> getString(LR.string.profile_refresh_failed)
            else -> getString(LR.string.profile_refresh_status_unknown)
        }
        preferenceRefreshNow?.summary = status
    }

    private fun changeSkipTitles() {
        val skipForwardSummary = resources.getStringPluralSeconds(settings.skipForwardInSecs.flow.value)
        preferenceSkipForward?.summary = skipForwardSummary
        val skipBackwardSummary = resources.getStringPluralSeconds(settings.getSkipBackwardInSecs())
        preferenceSkipBackward?.summary = skipBackwardSummary
    }
}

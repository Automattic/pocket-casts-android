package au.com.shiftyjelly.pocketcasts

import android.os.Bundle
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.toLiveData
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import au.com.shiftyjelly.pocketcasts.localization.BuildConfig
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralSeconds
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralSecondsMinutesHoursDaysOrYears
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class AutomotiveSettingsPreferenceFragment : PreferenceFragmentCompat(), Observer<RefreshState> {

    @Inject lateinit var settings: Settings

    @Inject lateinit var podcastManager: PodcastManager

    private lateinit var preferenceAutoPlay: SwitchPreference
    private lateinit var preferenceAutoSubscribeToPlayed: SwitchPreference
    private lateinit var preferenceAutoShowPlayed: SwitchPreference
    private lateinit var preferenceSkipForward: EditTextPreference
    private lateinit var preferenceSkipBackward: EditTextPreference
    private lateinit var preferenceRefreshNow: Preference
    private lateinit var about: Preference
    private var refreshObservable: LiveData<RefreshState>? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_auto)

        preferenceAutoPlay = findPreference("autoUpNextEmpty")!!
        preferenceAutoSubscribeToPlayed = findPreference("autoSubscribeToPlayed")!!
        preferenceAutoShowPlayed = findPreference("autoShowPlayed")!!
        preferenceRefreshNow = findPreference("refresh_now")!!
        preferenceSkipForward = findPreference(Settings.PREFERENCE_SKIP_FORWARD)!!
        preferenceSkipBackward = findPreference(Settings.PREFERENCE_SKIP_BACKWARD)!!
        about = findPreference("about")!!

        setupAutoPlay()
        changeSkipTitles()
        setupRefreshNow()
        setupAbout()
    }

    private fun setupAutoPlay() {
        preferenceAutoPlay.apply {
            setOnPreferenceChangeListener { _, newValue ->
                settings.autoPlayNextEpisodeOnEmpty.set(newValue as Boolean, needsSync = true)
                true
            }
        }
        settings.autoPlayNextEpisodeOnEmpty.flow
            .onEach { preferenceAutoPlay.isChecked = it }
            .launchIn(lifecycleScope)
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

        preferenceSkipForward.setOnPreferenceChangeListener { _, newValue ->
            val value = newValue.toString().toIntOrNull() ?: 0
            if (value > 0) {
                settings.skipForwardInSecs.set(value, needsSync = true)
                changeSkipTitles()
                true
            } else {
                false
            }
        }

        preferenceSkipBackward.setOnPreferenceChangeListener { _, newValue ->
            val value = newValue.toString().toIntOrNull() ?: 0
            if (value > 0) {
                settings.skipBackInSecs.set(value, needsSync = true)
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

    override fun onResume() {
        super.onResume()


        preferenceAutoSubscribeToPlayed.apply {
            isChecked = settings.autoSubscribeToPlayed.value
            setOnPreferenceChangeListener { _, newValue ->
                settings.autoSubscribeToPlayed.set(newValue as Boolean, needsSync = false)
                true
            }
        }

        preferenceAutoShowPlayed.apply {
            isChecked = settings.autoShowPlayed.value
            setOnPreferenceChangeListener { _, newValue ->
                settings.autoShowPlayed.set(newValue as Boolean, needsSync = false)
                true
            }
        }
    }

    private fun setupAbout() {
        about.apply {
            summary = getString(LR.string.settings_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toString())
            setOnPreferenceClickListener {
                (activity as? FragmentHostListener)?.addFragment(AutomotiveAboutFragment())
                true
            }
        }
    }

    private fun setupRefreshNow() {
        preferenceRefreshNow.setOnPreferenceClickListener {
            podcastManager.refreshPodcasts(fromLog = "Automotive")
            updateRefreshSummary(RefreshState.Refreshing)
            true
        }
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
        preferenceRefreshNow.summary = status
    }

    private fun changeSkipTitles() {
        val skipForwardSummary = resources.getStringPluralSeconds(settings.skipForwardInSecs.value)
        preferenceSkipForward.summary = skipForwardSummary
        val skipBackwardSummary = resources.getStringPluralSeconds(settings.skipBackInSecs.value)
        preferenceSkipBackward.summary = skipBackwardSummary
    }
}

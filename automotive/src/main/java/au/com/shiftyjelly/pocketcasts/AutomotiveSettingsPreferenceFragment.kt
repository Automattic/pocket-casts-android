package au.com.shiftyjelly.pocketcasts

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
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
import java.util.Date
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.rx2.asFlow
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class AutomotiveSettingsPreferenceFragment : PreferenceFragmentCompat() {

    @Inject lateinit var settings: Settings

    @Inject lateinit var podcastManager: PodcastManager

    private lateinit var preferenceAutoPlay: SwitchPreference
    private lateinit var preferenceAutoSubscribeToPlayed: SwitchPreference
    private lateinit var preferenceAutoShowPlayed: SwitchPreference
    private lateinit var preferenceSkipForward: EditTextPreference
    private lateinit var preferenceSkipBackward: EditTextPreference
    private lateinit var preferenceRefreshNow: Preference
    private lateinit var about: Preference

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
        setupAutoSubscribeToPlayed()
        setupAutoShowPlayed()
        setupRefreshNow()
        changeSkipTitles()
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

    private fun setupAutoSubscribeToPlayed() {
        preferenceAutoSubscribeToPlayed.apply {
            setOnPreferenceChangeListener { _, newValue ->
                settings.autoSubscribeToPlayed.set(newValue as Boolean, needsSync = false)
                true
            }
        }
        settings.autoSubscribeToPlayed.flow
            .onEach { preferenceAutoSubscribeToPlayed.isChecked = it }
            .launchIn(lifecycleScope)
    }

    private fun setupAutoShowPlayed() {
        preferenceAutoShowPlayed.apply {
            setOnPreferenceChangeListener { _, newValue ->
                settings.autoShowPlayed.set(newValue as Boolean, needsSync = false)
                true
            }
        }
        settings.autoShowPlayed.flow
            .onEach { preferenceAutoShowPlayed.isChecked = it }
            .launchIn(lifecycleScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun setupRefreshNow() {
        preferenceRefreshNow.setOnPreferenceClickListener {
            podcastManager.refreshPodcasts(fromLog = "Automotive")
            updateRefreshSummary(RefreshState.Refreshing)
            true
        }
        settings.refreshStateObservable.asFlow()
            .flatMapLatest { state ->
                flow {
                    while (true) {
                        emit(state)
                        delay(500.milliseconds)
                    }
                }
            }
            .onEach { updateRefreshSummary(it) }
            .launchIn(lifecycleScope)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

    private fun setupAbout() {
        about.apply {
            summary = getString(LR.string.settings_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toString())
            setOnPreferenceClickListener {
                (activity as? FragmentHostListener)?.addFragment(AutomotiveAboutFragment())
                true
            }
        }
    }

    private fun changeSkipTitles() {
        val skipForwardSummary = resources.getStringPluralSeconds(settings.skipForwardInSecs.value)
        preferenceSkipForward.summary = skipForwardSummary
        val skipBackwardSummary = resources.getStringPluralSeconds(settings.skipBackInSecs.value)
        preferenceSkipBackward.summary = skipBackwardSummary
    }
}

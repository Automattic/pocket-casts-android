package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlaybackSpeedPreference
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastEffectsViewModel
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.extensions.getTintedDrawable
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.views.extensions.updateColors
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import au.com.shiftyjelly.pocketcasts.views.helper.ToolbarColors
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class PodcastEffectsFragment : PreferenceFragmentCompat() {

    @Inject lateinit var theme: Theme
    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private var preferenceCustomForPodcast: SwitchPreference? = null
    private var preferencePlaybackSpeed: PlaybackSpeedPreference? = null
    private var preferenceTrimSilence: SwitchPreference? = null
    private var preferenceBoostVolume: SwitchPreference? = null
    private var preferenceTrimMode: ListPreference? = null

    private val viewModel: PodcastEffectsViewModel by viewModels()

    companion object {
        const val ARG_PODCAST_UUID = "ARG_PODCAST_UUID"

        fun newInstance(podcastUuid: String): PodcastEffectsFragment {
            return PodcastEffectsFragment().apply {
                arguments = bundleOf(
                    ARG_PODCAST_UUID to podcastUuid
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val podcastUuid = arguments?.getString(ARG_PODCAST_UUID) ?: return

        viewModel.loadPodcast(podcastUuid)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_podcast_effects, rootKey)

        preferenceCustomForPodcast = preferenceManager.findPreference("customForPodcast")
        preferencePlaybackSpeed = preferenceManager.findPreference("playbackSpeed")
        preferenceTrimSilence = preferenceManager.findPreference("trimSilence")
        preferenceBoostVolume = preferenceManager.findPreference("boostVolume")
        preferenceTrimMode = preferenceManager.findPreference("trimMode")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.setBackgroundColor(view.context.getThemeColor(UR.attr.primary_ui_01))
        view.isClickable = true

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = getString(LR.string.podcast_playback_effects)
        toolbar.navigationIcon?.setTint(ThemeColor.secondaryIcon01(theme.activeTheme))

        (activity as AppCompatActivity).setSupportActionBar(toolbar)

        preferencePlaybackSpeed?.isVisible = false
        preferenceTrimSilence?.isVisible = false
        preferenceBoostVolume?.isVisible = false
        preferenceTrimMode?.isVisible = false

        viewModel.podcast.observe(viewLifecycleOwner) { podcast ->

            val colors = ToolbarColors.Podcast(podcast = podcast, theme = theme)

            updateTintColor(colors.iconColor)
            toolbar.updateColors(toolbarColors = colors, navigationIcon = BackArrow)
            theme.updateWindowStatusBar(window = requireActivity().window, statusBarColor = StatusBarColor.Custom(colors.backgroundColor, true), context = requireContext())

            preferenceCustomForPodcast?.isChecked = podcast.overrideGlobalEffects

            preferenceTrimSilence?.isChecked = podcast.isSilenceRemoved
            preferenceBoostVolume?.isChecked = podcast.isVolumeBoosted
            if (podcast.isSilenceRemoved) {
                preferenceTrimMode?.setValueIndex((podcast.trimMode.ordinal - 1).coerceAtLeast(0)) // Shouldn't be needed but just in case the two settings get out of sync.
            }

            preferencePlaybackSpeed?.isVisible = podcast.overrideGlobalEffects
            preferenceTrimSilence?.isVisible = podcast.overrideGlobalEffects
            preferenceBoostVolume?.isVisible = podcast.overrideGlobalEffects
            preferenceTrimMode?.isVisible = podcast.overrideGlobalEffects && podcast.isSilenceRemoved

            preferencePlaybackSpeed?.speed = podcast.playbackSpeed
        }

        preferenceCustomForPodcast?.setOnPreferenceChangeListener { _, newValue ->
            analyticsTracker.track(
                AnalyticsEvent.PODCAST_SETTINGS_CUSTOM_PLAYBACK_EFFECTS_TOGGLED,
                mapOf("enabled" to newValue as Boolean)
            )
            viewModel.updateOverrideGlobalEffects(newValue)
            true
        }

        preferenceTrimSilence?.setOnPreferenceChangeListener { _, newValue ->
            viewModel.trackPlaybackEffectsEvent(AnalyticsEvent.PLAYBACK_EFFECT_TRIM_SILENCE_TOGGLED, mapOf(PlaybackManager.ENABLED_KEY to newValue))
            viewModel.updateTrimSilence(if (newValue as? Boolean == true) TrimMode.LOW else TrimMode.OFF)
            true
        }

        preferenceTrimMode?.setOnPreferenceChangeListener { preference, newValue ->
            val index = (preference as ListPreference).findIndexOfValue(newValue as String)
            val trimMode = TrimMode.values()[index + 1]
            viewModel.trackPlaybackEffectsEvent(AnalyticsEvent.PLAYBACK_EFFECT_TRIM_SILENCE_AMOUNT_CHANGED, mapOf(PlaybackManager.AMOUNT_KEY to trimMode.analyticsVale))
            viewModel.updateTrimSilence(trimMode)
            true
        }

        preferenceBoostVolume?.setOnPreferenceChangeListener { _, newValue ->
            viewModel.trackPlaybackEffectsEvent(AnalyticsEvent.PLAYBACK_EFFECT_VOLUME_BOOST_TOGGLED, mapOf(PlaybackManager.ENABLED_KEY to newValue))
            viewModel.updateBoostVolume(newValue as Boolean)
            true
        }

        preferencePlaybackSpeed?.onSpeedMinusClicked = {
            viewModel.decreasePlaybackSpeed()
        }

        preferencePlaybackSpeed?.onSpeedPlusClicked = {
            viewModel.increasePlaybackSpeed()
        }
    }

    private fun updateTintColor(tintColor: Int) {
        // xml doesn't support tinting icons so we need to do it manually
        val context = preferenceManager.context
        preferencePlaybackSpeed?.icon = context.getTintedDrawable(IR.drawable.ic_speed, tintColor)
        preferenceTrimSilence?.icon = context.getTintedDrawable(R.drawable.ic_silence, tintColor)
        preferenceBoostVolume?.icon = context.getTintedDrawable(R.drawable.ic_volumeboost, tintColor)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.trackSpeedChangeIfNeeded()
    }
}

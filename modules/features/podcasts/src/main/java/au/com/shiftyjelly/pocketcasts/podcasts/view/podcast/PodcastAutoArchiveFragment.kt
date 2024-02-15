package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.ListPreference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveAfterPlaying
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveInactive
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.extensions.updateColors
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import au.com.shiftyjelly.pocketcasts.views.helper.ToolbarColors
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class PodcastAutoArchiveFragment : PreferenceFragmentCompat() {
    @Inject lateinit var theme: Theme

    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private val viewModel by viewModels<PodcastAutoArchiveViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<PodcastAutoArchiveViewModel.Factory> { factory ->
                factory.create(requireArguments().getString(ARG_PODCAST_UUID)!!)
            }
        },
    )
    private lateinit var toolbar: Toolbar
    private var preferenceCustomForPodcast: SwitchPreference? = null
    private var preferenceCustomCategory: PreferenceCategory? = null
    private var preferenceEpisodeLimitCategory: PreferenceCategory? = null
    private var preferenceAutoArchivePodcastPlayedEpisodes: ListPreference? = null
    private var preferenceAutoArchivePodcastInactiveEpisodes: ListPreference? = null
    private var preferenceAutoArchiveEpisodeLimit: ListPreference? = null

    val afterPlayingValues
        get() = resources.getStringArray(LR.array.settings_auto_archive_played_values)
    val inactiveValues
        get() = resources.getStringArray(LR.array.settings_auto_archive_inactive_values)
    val episodeLimitValues
        get() = resources.getStringArray(LR.array.settings_auto_archive_episode_limit_values)

    companion object {
        const val ARG_PODCAST_UUID = "ARG_PODCAST_UUID"

        private val EpisodeLimits = listOf(null, 1, 2, 5, 10)

        fun newInstance(podcastUuid: String): PodcastAutoArchiveFragment {
            return PodcastAutoArchiveFragment().apply {
                arguments = bundleOf(
                    ARG_PODCAST_UUID to podcastUuid,
                )
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_podcast_auto_archive, rootKey)
        preferenceCustomForPodcast = preferenceManager.findPreference("customForPodcast")
        preferenceCustomCategory = preferenceManager.findPreference("customCategory")
        preferenceEpisodeLimitCategory = preferenceManager.findPreference("categoryEpisodeLimit")
        preferenceAutoArchivePodcastPlayedEpisodes = preferenceManager.findPreference("autoArchivePodcastPlayedEpisodes")
        preferenceAutoArchivePodcastInactiveEpisodes = preferenceManager.findPreference("autoArchivePodcastInactiveEpisodes")
        preferenceAutoArchiveEpisodeLimit = preferenceManager.findPreference("autoArchiveEpisodeLimit")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.setBackgroundColor(view.context.getThemeColor(UR.attr.primary_ui_01))
        view.isClickable = true

        toolbar = view.findViewById(R.id.toolbar)
        toolbar.title = getString(LR.string.settings_title_auto_archive)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)

        preferenceCustomCategory?.isVisible = false
        preferenceEpisodeLimitCategory?.isVisible = false

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.podcast.collect { podcast ->
                    val colors = ToolbarColors.Podcast(podcast = podcast, theme = theme)

                    toolbar.updateColors(toolbarColors = colors, navigationIcon = BackArrow)

                    preferenceCustomForPodcast?.isChecked = podcast.overrideGlobalArchive
                    preferenceCustomCategory?.isVisible = podcast.overrideGlobalArchive
                    preferenceEpisodeLimitCategory?.isVisible = podcast.overrideGlobalArchive

                    preferenceAutoArchivePodcastPlayedEpisodes?.value = afterPlayingValues[podcast.autoArchiveAfterPlaying.index]
                    preferenceAutoArchivePodcastInactiveEpisodes?.value = inactiveValues[podcast.autoArchiveInactive.index]
                    val limitIndex = EpisodeLimits.indexOf(podcast.autoArchiveEpisodeLimit).takeIf { it != -1 } ?: 0
                    preferenceAutoArchiveEpisodeLimit?.value = episodeLimitValues[limitIndex]
                }
            }
        }

        preferenceCustomForPodcast?.setOnPreferenceChangeListener { _, newValue ->
            analyticsTracker.track(
                AnalyticsEvent.PODCAST_SETTINGS_AUTO_ARCHIVE_TOGGLED,
                mapOf("enabled" to (newValue as Boolean)),
            )
            viewModel.updateGlobalOverride(newValue)
            true
        }

        preferenceAutoArchivePodcastPlayedEpisodes?.setOnPreferenceChangeListener { _, newValue ->
            val stringVal = newValue as? String ?: return@setOnPreferenceChangeListener false
            val index = max(afterPlayingValues.indexOf(stringVal), 0) // Returns -1 on not found, default it to 0
            val value = AutoArchiveAfterPlaying.fromIndex(index) ?: AutoArchiveAfterPlaying.defaultValue(requireContext())
            analyticsTracker.track(
                AnalyticsEvent.PODCAST_SETTINGS_AUTO_ARCHIVE_PLAYED_CHANGED,
                mapOf("value" to value.analyticsValue),
            )
            viewModel.updateAfterPlaying(value)
            true
        }

        preferenceAutoArchivePodcastInactiveEpisodes?.setOnPreferenceChangeListener { _, newValue ->
            val stringVal = newValue as? String ?: return@setOnPreferenceChangeListener false
            val index = max(inactiveValues.indexOf(stringVal), 0) // Returns -1 on not found, default it to 0
            val value = AutoArchiveInactive.fromIndex(index) ?: AutoArchiveInactive.Default
            analyticsTracker.track(
                AnalyticsEvent.PODCAST_SETTINGS_AUTO_ARCHIVE_INACTIVE_CHANGED,
                mapOf("value" to value.analyticsValue),
            )
            viewModel.updateInactive(value)
            true
        }

        preferenceAutoArchiveEpisodeLimit?.setOnPreferenceChangeListener { _, newValue ->
            val stringVal = newValue as? String ?: return@setOnPreferenceChangeListener false
            val index = max(episodeLimitValues.indexOf(stringVal), 0)
            val value = EpisodeLimits.getOrNull(index)
            analyticsTracker.track(
                AnalyticsEvent.PODCAST_SETTINGS_AUTO_ARCHIVE_EPISODE_LIMIT_CHANGED,
                mapOf("value" to (value?.toString() ?: "none")),
            )
            viewModel.updateEpisodeLimit(value)
            true
        }
    }
}

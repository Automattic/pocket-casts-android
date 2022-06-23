package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.preference.ListPreference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.extensions.updateColors
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import au.com.shiftyjelly.pocketcasts.views.helper.ToolbarColors
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.max
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class PodcastAutoArchiveFragment : PreferenceFragmentCompat() {
    @Inject lateinit var theme: Theme

    private val viewModel: PodcastAutoArchiveViewModel by viewModels()
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

    var episodeLimitIndex: Int? = null

    companion object {
        const val ARG_PODCAST_UUID = "ARG_PODCAST_UUID"

        fun newInstance(podcastUuid: String): PodcastAutoArchiveFragment {
            return PodcastAutoArchiveFragment().apply {
                arguments = bundleOf(
                    ARG_PODCAST_UUID to podcastUuid
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setup(arguments?.getString(ARG_PODCAST_UUID)!!)
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
        viewModel.podcast.observe(viewLifecycleOwner) { podcast ->
            val colors = ToolbarColors.Podcast(podcast = podcast, theme = theme)

            toolbar.updateColors(toolbarColors = colors, navigationIcon = BackArrow)

            preferenceCustomForPodcast?.isChecked = podcast.overrideGlobalArchive
            preferenceCustomCategory?.isVisible = podcast.overrideGlobalArchive
            preferenceEpisodeLimitCategory?.isVisible = podcast.overrideGlobalArchive

            preferenceAutoArchivePodcastPlayedEpisodes?.value = afterPlayingValues[podcast.autoArchiveAfterPlaying]
            preferenceAutoArchivePodcastInactiveEpisodes?.value = inactiveValues[podcast.autoArchiveInactive]

            val episodeLimitIndex = PodcastAutoArchiveViewModel.EPISODE_LIMITS.indexOf(podcast.autoArchiveEpisodeLimit)
            preferenceAutoArchiveEpisodeLimit?.value = episodeLimitValues[episodeLimitIndex]
        }

        preferenceCustomForPodcast?.setOnPreferenceChangeListener { _, newValue ->
            viewModel.updateGlobalOverride(newValue as Boolean)
            true
        }

        preferenceAutoArchivePodcastPlayedEpisodes?.setOnPreferenceChangeListener { _, newValue ->
            val stringVal = newValue as? String ?: return@setOnPreferenceChangeListener false
            val index = max(afterPlayingValues.indexOf(stringVal), 0) // Returns -1 on not found, default it to 0
            viewModel.updateAfterPlaying(index)
            true
        }

        preferenceAutoArchivePodcastInactiveEpisodes?.setOnPreferenceChangeListener { _, newValue ->
            val stringVal = newValue as? String ?: return@setOnPreferenceChangeListener false
            val index = max(inactiveValues.indexOf(stringVal), 0) // Returns -1 on not found, default it to 0
            viewModel.updateInactive(index)
            true
        }

        preferenceAutoArchiveEpisodeLimit?.setOnPreferenceChangeListener { _, newValue ->
            val stringVal = newValue as? String ?: return@setOnPreferenceChangeListener false
            val index = max(episodeLimitValues.indexOf(stringVal), 0)
            episodeLimitIndex = index
            true
        }
    }

    override fun onPause() {
        super.onPause()
        episodeLimitIndex?.let {
            viewModel.updateEpisodeLimit(it)
        }
    }
}

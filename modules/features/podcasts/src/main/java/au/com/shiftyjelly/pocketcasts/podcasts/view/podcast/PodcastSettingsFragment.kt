package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralSeconds
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastSettingsViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.settings.AutoAddSettingsFragment
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.extensions.getTintedDrawable
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.AnalyticsHelper
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.extensions.setInputAsSeconds
import au.com.shiftyjelly.pocketcasts.views.extensions.updateColors
import au.com.shiftyjelly.pocketcasts.views.fragments.FilterSelectFragment
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import au.com.shiftyjelly.pocketcasts.views.helper.ToolbarColors
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.settings.R as SR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class PodcastSettingsFragment : PreferenceFragmentCompat(), CoroutineScope, FilterSelectFragment.Listener, HasBackstack {
    @Inject lateinit var theme: Theme
    @Inject lateinit var podcastManager: PodcastManager

    private var preferenceFeedIssueDetected: Preference? = null
    private var preferenceNotifications: SwitchPreference? = null
    private var preferenceAutoDownload: SwitchPreference? = null
    private var preferenceAddToUpNext: SwitchPreference? = null
    private var preferenceAddToUpNextOrder: ListPreference? = null
    private var preferenceAddToUpNextGlobal: Preference? = null
    private var preferencePlaybackEffects: Preference? = null
    private var preferenceSkipFirst: EditTextPreference? = null
    private var preferenceAutoArchive: Preference? = null
    private var preferenceFilters: Preference? = null
    private var preferenceUnsubscribe: Preference? = null
    private var preferenceSkipLast: EditTextPreference? = null

    private val viewModel: PodcastSettingsViewModel by viewModels()
    private var toolbar: Toolbar? = null

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    val podcastUuid
        get() = arguments?.getString(ARG_PODCAST_UUID)!!

    companion object {
        const val ARG_PODCAST_UUID = "ARG_PODCAST_UUID"

        fun newInstance(podcastUuid: String): PodcastSettingsFragment {
            return PodcastSettingsFragment().apply {
                arguments = bundleOf(
                    ARG_PODCAST_UUID to podcastUuid
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.loadPodcast(podcastUuid)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_podcasts, rootKey)

        preferenceFeedIssueDetected = preferenceManager.findPreference("feedIssueDetected")
        preferenceNotifications = preferenceManager.findPreference("notifications")
        preferenceAutoDownload = preferenceManager.findPreference("autoDownload")
        preferenceAddToUpNext = preferenceManager.findPreference("addToUpNext")
        preferenceAddToUpNextOrder = preferenceManager.findPreference("addToUpNextOrder")
        preferenceAddToUpNextGlobal = preferenceManager.findPreference("addToUpNextGlobal")
        preferencePlaybackEffects = preferenceManager.findPreference("playbackEffects")
        preferenceAutoArchive = preferenceManager.findPreference("autoArchivePodcast")
        preferenceFilters = preferenceManager.findPreference("filters")
        preferenceUnsubscribe = preferenceManager.findPreference("unsubscribe")
        preferenceSkipFirst = preferenceManager.findPreference<EditTextPreference>("skipFirst")?.apply {
            setInputAsSeconds()
        }
        preferenceSkipLast = preferenceManager.findPreference<EditTextPreference>("skipLast")?.apply {
            setInputAsSeconds()
        }
    }

    override fun onDestroyView() {
        toolbar = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.setBackgroundColor(view.context.getThemeColor(UR.attr.primary_ui_01))
        view.isClickable = true

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        this.toolbar = toolbar
        toolbar.title = getString(LR.string.podcast_settings)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)

        preferenceAddToUpNextOrder?.isVisible = false

        viewModel.podcast.observe(viewLifecycleOwner) { podcast ->
            val context = toolbar.context

            val colors = ToolbarColors.Podcast(podcast = podcast, theme = theme)

            preferenceFeedIssueDetected?.icon = context.getTintedDrawable(IR.drawable.ic_alert_small, colors.iconColor)
            preferenceFeedIssueDetected?.isVisible = podcast.refreshAvailable

            val effectsDrawableId = if (podcast.overrideGlobalEffects) R.drawable.ic_effects_on else R.drawable.ic_effects_off
            preferencePlaybackEffects?.icon = context.getTintedDrawable(effectsDrawableId, colors.iconColor)
            preferencePlaybackEffects?.summary = buildEffectsSummary(podcast)

            toolbar.title = podcast.title

            updateTintColor(colors.iconColor)
            toolbar.updateColors(toolbarColors = colors, navigationIcon = BackArrow)
            theme.updateWindowStatusBar(
                window = requireActivity().window,
                statusBarColor = StatusBarColor.Custom(colors.backgroundColor, isWhiteIcons = theme.activeTheme.defaultLightIcons),
                context = requireContext()
            )

            preferenceNotifications?.isChecked = podcast.isShowNotifications

            preferenceAutoDownload?.isChecked = podcast.isAutoDownloadNewEpisodes

            preferenceAddToUpNext?.isChecked = !podcast.isAutoAddToUpNextOff
            preferenceAddToUpNextOrder?.isVisible = !podcast.isAutoAddToUpNextOff
            preferenceAddToUpNextOrder?.summary = if (podcast.autoAddToUpNext == Podcast.AUTO_ADD_TO_UP_NEXT_PLAY_NEXT) getString(LR.string.play_next) else getString(LR.string.play_last)
            preferenceAddToUpNextOrder?.setValueIndex(if (podcast.isAutoAddToUpNextOff) 0 else podcast.autoAddToUpNext - 1)
            preferenceAddToUpNextGlobal?.isVisible = preferenceAddToUpNextOrder?.isVisible ?: false

            preferenceSkipFirst?.text = podcast.startFromSecs.toString()
            preferenceSkipFirst?.summary = resources.getStringPluralSeconds(podcast.startFromSecs)

            preferenceSkipLast?.text = podcast.skipLastSecs.toString()
            preferenceSkipLast?.summary = resources.getStringPluralSeconds(podcast.skipLastSecs)

            preferenceFilters?.icon = context.getTintedDrawable(IR.drawable.ic_filters, colors.iconColor)

            preferenceUnsubscribe?.isVisible = podcast.isSubscribed
        }

        viewModel.includedFilters.observe(viewLifecycleOwner) {
            updateFiltersSummary(it)
        }

        viewModel.globalSettings.observe(viewLifecycleOwner) {
            val summary = when (it.second) {
                Settings.AutoAddUpNextLimitBehaviour.ONLY_ADD_TO_TOP, null -> getString(LR.string.settings_auto_up_next_limit_reached_top_summary, it.first)
                Settings.AutoAddUpNextLimitBehaviour.STOP_ADDING -> getString(LR.string.settings_auto_up_next_limit_reached_stop_summary, it.first)
            }

            preferenceAddToUpNextGlobal?.summary = getString(LR.string.podcast_settings_up_next_episode_limit, it.first) + "\n\n" + summary
        }

        setupAddToUpNext()
        setupPlaybackEffects()
        setupNotifications()
        setupArchive()
        setupSkipFirst()
        setupSkipLast()
        setupStatusBar()
        setupFilters()
        setupUnsubscribe()
        setupFeedIssueDetected()
    }

    private fun setupFeedIssueDetected() {
        preferenceFeedIssueDetected?.setOnPreferenceClickListener {
            val dialog = ConfirmationDialog().setButtonType(ConfirmationDialog.ButtonType.Normal(getString(LR.string.podcast_feed_issue_dialog_button)))
                .setTitle(getString(LR.string.podcast_feed_issue_dialog_title))
                .setSummary(getString(LR.string.podcast_feed_issue_dialog_summary))
                .setIconId(IR.drawable.ic_failedwarning)
                .setOnConfirm {
                    launch {
                        podcastManager.updateRefreshAvailable(podcastUuid = podcastUuid, refreshAvailable = false)
                        val success = podcastManager.refreshPodcastFeed(podcastUuid = podcastUuid)
                        AnalyticsHelper.podcastFeedRefreshed()
                        showFeedUpdateQueued(success = success)
                    }
                }
            parentFragmentManager.let {
                dialog.show(it, "feed_issue_detected")
            }
            true
        }
    }

    private fun showFeedUpdateQueued(success: Boolean) {
        val title: Int
        val summary: Int
        if (success) {
            title = LR.string.podcast_update_queued_success_title
            summary = LR.string.podcast_update_queued_success_summary
        } else {
            title = LR.string.podcast_update_queued_failed_title
            summary = LR.string.podcast_update_queued_failed_summary
        }

        val dialog = ConfirmationDialog().setButtonType(
            ConfirmationDialog.ButtonType.Normal(
                getString(
                    LR.string.ok
                )
            )
        )
            .setTitle(getString(title))
            .setSummary(getString(summary))
            .setIconId(SR.drawable.ic_refresh)
        parentFragmentManager.let {
            dialog.show(it, "update_queued")
        }
    }

    override fun onPause() {
        super.onPause()
        view?.let { UiUtil.hideKeyboard(it) }
    }

    private fun setupArchive() {
        preferenceAutoArchive?.setOnPreferenceClickListener {
            viewModel.podcastUuid?.let { uuid ->
                (activity as FragmentHostListener).addFragment(PodcastAutoArchiveFragment.newInstance(uuid))
            }
            true
        }
    }

    private fun setupSkipFirst() {
        preferenceSkipFirst?.setOnPreferenceChangeListener { _, newValue ->
            var stringValue = newValue as String
            try {
                if (stringValue.isBlank()) {
                    stringValue = "0"
                }
                viewModel.updateStartFrom(stringValue.toInt())
            } catch (e: NumberFormatException) {
                Timber.e(e)
            }
            true
        }
    }

    private fun setupSkipLast() {
        preferenceSkipLast?.setOnPreferenceChangeListener { _, newValue ->
            var stringValue = newValue as String
            try {
                if (stringValue.isBlank()) {
                    stringValue = "0"
                }
                viewModel.updateSkipLast(stringValue.toInt())
            } catch (e: java.lang.NumberFormatException) {
                Timber.e(e)
            }
            true
        }
    }

    private fun setupNotifications() {
        preferenceNotifications?.setOnPreferenceChangeListener { _, newValue ->
            viewModel.showNotifications(newValue as Boolean)
            true
        }
    }

    private fun setupPlaybackEffects() {
        preferencePlaybackEffects?.setOnPreferenceClickListener {
            viewModel.podcastUuid?.let { uuid ->
                (activity as FragmentHostListener).addFragment(PodcastEffectsFragment.newInstance(uuid))
            }
            true
        }
    }

    private fun setupFilters() {
        preferenceFilters?.setOnPreferenceClickListener {
            val fragment = FilterSelectFragment.newInstance(shouldFilterPlaylistsWithAllPodcasts = true)
            childFragmentManager.beginTransaction()
                .replace(UR.id.frameChildFragment, fragment)
                .addToBackStack("filterSelect")
                .commit()
            true
        }
    }

    private fun setupUnsubscribe() {
        preferenceUnsubscribe?.setOnPreferenceClickListener {
            launch {
                val resources = context?.resources ?: return@launch
                val downloaded = withContext(Dispatchers.Default) { podcastManager.countEpisodesInPodcastWithStatus(podcastUuid, EpisodeStatusEnum.DOWNLOADED) }
                val title = when (downloaded) {
                    0 -> resources.getString(LR.string.are_you_sure)
                    1 -> resources.getString(LR.string.podcast_unsubscribe_downloaded_file_singular)
                    else -> resources.getString(LR.string.podcast_unsubscribe_downloaded_file_plural, downloaded)
                }
                val dialog = ConfirmationDialog().setButtonType(
                    ConfirmationDialog.ButtonType.Danger(
                        resources.getString(
                            LR.string.unsubscribe
                        )
                    )
                )
                    .setTitle(title)
                    .setSummary(resources.getString(LR.string.podcast_unsubscribe_warning))
                    .setIconId(IR.drawable.ic_failedwarning)
                    .setOnConfirm {
                        viewModel.unsubscribe()
                        (activity as FragmentHostListener).closeToRoot()
                    }
                dialog.show(parentFragmentManager, "unsubscribe")
            }

            true
        }
    }

    private fun buildEffectsSummary(podcast: Podcast): String {
        return if (podcast.overrideGlobalEffects) {
            listOf(
                getString(LR.string.podcast_effects_summary_speed, podcast.playbackSpeed.toString()),
                getString(if (podcast.isSilenceRemoved) LR.string.podcast_effects_summary_trim_silence_on else LR.string.podcast_effects_summary_trim_silence_off),
                getString(if (podcast.isVolumeBoosted) LR.string.podcast_effects_summary_volume_boost_on else LR.string.podcast_effects_summary_volume_boost_off)
            ).joinToString()
        } else {
            getString(LR.string.podcast_effects_summary_default)
        }
    }

    private fun setupStatusBar() {
        activity?.let {
            theme.updateWindowStatusBar(window = it.window, statusBarColor = StatusBarColor.Light, context = it)
        }
    }

    private fun setupAddToUpNext() {
        preferenceAddToUpNext?.run {
            isChecked = viewModel.isAutoAddToUpNextOn()
            setOnPreferenceChangeListener { _, isOn ->
                viewModel.updateAutoAddToUpNext(isOn as Boolean)
                true
            }
        }

        preferenceAddToUpNextOrder?.run {
            entries = arrayOf(
                getString(LR.string.play_last),
                getString(
                    LR.string.play_next
                )
            )
            entryValues = arrayOf("1", "2")
            setOnPreferenceChangeListener { _, newValue ->
                val value = Integer.parseInt(newValue as String)
                viewModel.updateAutoAddToUpNextOrder(value)
                true
            }
        }

        preferenceAutoDownload?.setOnPreferenceChangeListener { _, newValue ->
            viewModel.setAutoDownloadEpisodes(newValue as Boolean)
            true
        }

        preferenceAddToUpNextGlobal?.run {
            setOnPreferenceClickListener {
                val fragment = AutoAddSettingsFragment()
                (activity as? FragmentHostListener)?.addFragment(fragment)
                true
            }
        }
    }

    private fun updateTintColor(tintColor: Int) {
        // xml doesn't support tinting icons so we need to do it manually
        val context = preferenceManager.context
        preferenceNotifications?.icon = context.getTintedDrawable(R.drawable.ic_notifications, tintColor)
        preferenceAutoDownload?.icon = context.getTintedDrawable(IR.drawable.ic_download, tintColor)
        preferenceAddToUpNext?.icon = context.getTintedDrawable(IR.drawable.ic_upnext, tintColor)
        preferenceSkipFirst?.icon = context.getTintedDrawable(R.drawable.ic_skipintros, tintColor)
        preferenceAutoArchive?.icon = context.getTintedDrawable(IR.drawable.ic_archive, tintColor)
        preferenceSkipLast?.icon = context.getTintedDrawable(R.drawable.ic_skip_outro, tintColor)
    }

    private fun updateFiltersSummary(filters: List<Playlist>) {
        val filterTitles = filters.map { it.title }
        if (filterTitles.isEmpty()) {
            preferenceFilters?.summary = getString(LR.string.podcast_not_in_filters)
        } else {
            preferenceFilters?.summary = getString(LR.string.podcast_included_in_filters, filterTitles.joinToString())
        }
    }

    override fun filterSelectFragmentSelectionChanged(newSelection: List<String>) {
        viewModel.filterSelectionChanged(newSelection)
    }

    override fun filterSelectFragmentGetCurrentSelection(): List<String> {
        return viewModel.includedFilters.value?.map { it.uuid } ?: emptyList()
    }

    override fun onBackPressed(): Boolean {
        if (childFragmentManager.backStackEntryCount > 0) {
            childFragmentManager.popBackStack()
            toolbar?.title = viewModel.podcast.value?.title ?: getString(LR.string.settings)
            return true
        }

        return false
    }

    override fun getBackstackCount(): Int {
        return childFragmentManager.backStackEntryCount
    }
}

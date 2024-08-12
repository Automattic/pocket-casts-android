package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralSeconds
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastSettingsViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoAddUpNextLimitBehaviour
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.settings.AutoAddSettingsFragment
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.extensions.getTintedDrawable
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.combineLatest
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.extensions.setInputAsSeconds
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.fragments.BasePreferenceFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.FilterSelectFragment
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import au.com.shiftyjelly.pocketcasts.views.helper.ToolbarColors
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.settings.R as SR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class PodcastSettingsFragment : BasePreferenceFragment(), FilterSelectFragment.Listener, HasBackstack {
    @Inject lateinit var theme: Theme

    @Inject lateinit var podcastManager: PodcastManager

    @Inject lateinit var analyticsTracker: AnalyticsTracker

    @Inject lateinit var settings: Settings

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

    val podcastUuid
        get() = arguments?.getString(ARG_PODCAST_UUID)!!

    companion object {
        const val ARG_PODCAST_UUID = "ARG_PODCAST_UUID"

        fun newInstance(podcastUuid: String): PodcastSettingsFragment {
            return PodcastSettingsFragment().apply {
                arguments = bundleOf(
                    ARG_PODCAST_UUID to podcastUuid,
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
        showLoading()

        view.setBackgroundColor(view.context.getThemeColor(UR.attr.primary_ui_01))
        view.isClickable = true

        toolbar = view.findViewById<Toolbar>(R.id.toolbar)

        preferenceAddToUpNextOrder?.isVisible = false

        viewModel.podcast.observe(viewLifecycleOwner) { podcast ->
            val context = context ?: return@observe

            val colors = ToolbarColors.podcast(podcast = podcast, theme = theme)

            preferenceFeedIssueDetected?.icon = context.getTintedDrawable(IR.drawable.ic_alert_small, colors.iconColor)
            preferenceFeedIssueDetected?.isVisible = podcast.refreshAvailable

            val effectsDrawableId = if (podcast.overrideGlobalEffects) R.drawable.ic_effects_on else R.drawable.ic_effects_off
            preferencePlaybackEffects?.icon = context.getTintedDrawable(effectsDrawableId, colors.iconColor)
            preferencePlaybackEffects?.summary = buildEffectsSummary(podcast)

            updateTintColor(colors.iconColor)
            toolbar?.setup(title = podcast.title, navigationIcon = BackArrow, toolbarColors = colors, theme = theme, activity = activity)

            theme.updateWindowStatusBar(
                window = requireActivity().window,
                statusBarColor = StatusBarColor.Custom(colors.backgroundColor, isWhiteIcons = theme.activeTheme.defaultLightIcons),
                context = context,
            )

            preferenceNotifications?.isChecked = podcast.isShowNotifications

            preferenceAutoDownload?.isChecked = podcast.isAutoDownloadNewEpisodes

            preferenceAddToUpNext?.isChecked = !podcast.isAutoAddToUpNextOff
            preferenceAddToUpNextOrder?.isVisible = !podcast.isAutoAddToUpNextOff
            preferenceAddToUpNextOrder?.summary = if (podcast.autoAddToUpNext == Podcast.AutoAddUpNext.PLAY_NEXT) getString(LR.string.play_next) else getString(LR.string.play_last)
            preferenceAddToUpNextOrder?.setValueIndex(if (podcast.isAutoAddToUpNextOff) 0 else podcast.autoAddToUpNext.databaseInt - 1)
            preferenceAddToUpNextGlobal?.isVisible = preferenceAddToUpNextOrder?.isVisible ?: false

            preferenceSkipFirst?.text = podcast.startFromSecs.toString()
            preferenceSkipFirst?.summary = resources.getStringPluralSeconds(podcast.startFromSecs)

            preferenceSkipLast?.text = podcast.skipLastSecs.toString()
            preferenceSkipLast?.summary = resources.getStringPluralSeconds(podcast.skipLastSecs)

            preferenceFilters?.icon = context.getTintedDrawable(IR.drawable.ic_filters, colors.iconColor)

            preferenceUnsubscribe?.isVisible = podcast.isSubscribed

            hideLoading()
        }

        viewModel.includedFilters
            .combineLatest(viewModel.availableFilters)
            .observe(viewLifecycleOwner) { (included, available) ->
                updateFiltersSummary(included, available)
            }

        viewModel.globalSettings.observe(viewLifecycleOwner) {
            val summary = when (it.second) {
                AutoAddUpNextLimitBehaviour.ONLY_ADD_TO_TOP -> getString(LR.string.settings_auto_up_next_limit_reached_top_summary, it.first)
                AutoAddUpNextLimitBehaviour.STOP_ADDING -> getString(LR.string.settings_auto_up_next_limit_reached_stop_summary, it.first)
            }

            preferenceAddToUpNextGlobal?.summary = getString(LR.string.podcast_settings_up_next_episode_limit, it.first) + "\n\n" + summary
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                settings.bottomInset.collect {
                    view.updatePadding(bottom = it)
                }
            }
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
            analyticsTracker.track(AnalyticsEvent.PODCAST_SETTINGS_FEED_ERROR_TAPPED)
            val dialog = ConfirmationDialog().setButtonType(ConfirmationDialog.ButtonType.Normal(getString(LR.string.podcast_feed_issue_dialog_button)))
                .setTitle(getString(LR.string.podcast_feed_issue_dialog_title))
                .setSummary(getString(LR.string.podcast_feed_issue_dialog_summary))
                .setIconId(IR.drawable.ic_failedwarning)
                .setOnConfirm {
                    lifecycleScope.launch {
                        analyticsTracker.track(AnalyticsEvent.PODCAST_SETTINGS_FEED_ERROR_UPDATE_TAPPED)
                        podcastManager.updateRefreshAvailable(podcastUuid = podcastUuid, refreshAvailable = false)
                        val success = podcastManager.refreshPodcastFeed(podcastUuid = podcastUuid)
                        analyticsTracker.track(
                            if (success) {
                                AnalyticsEvent.PODCAST_SETTINGS_FEED_ERROR_FIX_SUCCEEDED
                            } else {
                                AnalyticsEvent.PODCAST_SETTINGS_FEED_ERROR_FIX_FAILED
                            },
                        )
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
                    LR.string.ok,
                ),
            ),
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
            viewModel.podcast.value?.let { podcast ->
                (activity as FragmentHostListener).addFragment(
                    PodcastAutoArchiveFragment.newInstance(
                        podcast.uuid,
                        ToolbarColors.podcast(podcast, theme),
                    ),
                )
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
                val secs = stringValue.toInt()
                analyticsTracker.track(
                    AnalyticsEvent.PODCAST_SETTINGS_SKIP_FIRST_CHANGED,
                    mapOf("value" to secs),
                )
                viewModel.updateStartFrom(secs)
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
                val secs = stringValue.toInt()
                analyticsTracker.track(
                    AnalyticsEvent.PODCAST_SETTINGS_SKIP_LAST_CHANGED,
                    mapOf("value" to secs),
                )
                viewModel.updateSkipLast(secs)
            } catch (e: java.lang.NumberFormatException) {
                Timber.e(e)
            }
            true
        }
    }

    private fun setupNotifications() {
        preferenceNotifications?.setOnPreferenceChangeListener { _, newValue ->
            analyticsTracker.track(
                AnalyticsEvent.PODCAST_SETTINGS_NOTIFICATIONS_TOGGLED,
                mapOf("enabled" to (newValue as Boolean)),
            )
            viewModel.showNotifications(newValue)
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
            val fragment = FilterSelectFragment.newInstance(
                source = FilterSelectFragment.Source.PODCAST_SETTINGS,
                shouldFilterPlaylistsWithAllPodcasts = true,
            )
            childFragmentManager.beginTransaction()
                .replace(UR.id.frameChildFragment, fragment)
                .addToBackStack("filterSelect")
                .commit()
            true
        }
    }

    private fun setupUnsubscribe() {
        preferenceUnsubscribe?.setOnPreferenceClickListener {
            lifecycleScope.launch {
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
                            LR.string.unsubscribe,
                        ),
                    ),
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
                getString(if (podcast.isVolumeBoosted) LR.string.podcast_effects_summary_volume_boost_on else LR.string.podcast_effects_summary_volume_boost_off),
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
                analyticsTracker.track(
                    AnalyticsEvent.PODCAST_SETTINGS_AUTO_ADD_UP_NEXT_TOGGLED,
                    mapOf("enabled" to isOn as Boolean),
                )
                viewModel.updateAutoAddToUpNext(isOn)
                true
            }
        }

        preferenceAddToUpNextOrder?.run {
            entries = arrayOf(
                getString(LR.string.play_last),
                getString(
                    LR.string.play_next,
                ),
            )
            entryValues = arrayOf("1", "2")
            setOnPreferenceChangeListener { _, newValue ->
                when (newValue) {
                    "1" -> Podcast.AutoAddUpNext.PLAY_LAST
                    "2" -> Podcast.AutoAddUpNext.PLAY_NEXT
                    else -> {
                        Timber.e("Unknown value for auto add to up next order: $newValue")
                        null
                    }
                }?.let { value ->
                    viewModel.updateAutoAddToUpNextOrder(value)
                    analyticsTracker.track(
                        AnalyticsEvent.PODCAST_SETTINGS_AUTO_ADD_UP_NEXT_POSITION_OPTION_CHANGED,
                        mapOf("value" to value.analyticsValue),
                    )
                }
                true
            }
        }

        preferenceAutoDownload?.setOnPreferenceChangeListener { _, newValue ->
            analyticsTracker.track(
                AnalyticsEvent.PODCAST_SETTINGS_AUTO_DOWNLOAD_TOGGLED,
                mapOf("enabled" to newValue as Boolean),
            )
            viewModel.setAutoDownloadEpisodes(newValue)
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

    private fun updateFiltersSummary(includedFilters: List<Playlist>, availableFilters: List<Playlist>) {
        val filterTitles = includedFilters.map { it.title }
        if (filterTitles.isEmpty()) {
            preferenceFilters?.summary = getString(LR.string.podcast_not_in_filters)
        } else {
            preferenceFilters?.summary = getString(LR.string.podcast_included_in_filters, filterTitles.joinToString())
        }
        preferenceFilters?.isVisible = availableFilters.isNotEmpty()
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

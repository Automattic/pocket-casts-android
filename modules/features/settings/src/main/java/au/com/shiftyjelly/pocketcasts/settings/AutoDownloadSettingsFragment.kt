package au.com.shiftyjelly.pocketcasts.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPlural
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralPodcastsSelected
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.type.AutoDownloadLimitSetting
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.Settings.Companion.GLOBAL_AUTO_DOWNLOAD_NONE
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistProperty
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistUpdateSource
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserPlaylistUpdate
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.AutoDownloadSettingsViewModel
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.ManualCleanupViewModel
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.toAutoDownloadStatus
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.fragments.FilterSelectFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragmentSource
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.zipWith
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

private const val ARG_SHOW_TOOLBAR = "show_toolbar"

@AndroidEntryPoint
class AutoDownloadSettingsFragment :
    PreferenceFragmentCompat(),
    CoroutineScope,
    PodcastSelectFragment.Listener,
    FilterSelectFragment.Listener,
    HasBackstack {

    companion object {
        const val PREFERENCE_PODCASTS_CATEGORY = "podcasts_category"
        const val PREFERENCE_NEW_EPISODES = "autoDownloadNewEpisodes"
        const val PREFERENCE_ON_FOLLOW_PODCASTS = "onFollowPodcast"
        const val PREFERENCE_CHOOSE_PODCASTS = "autoDownloadPodcastsPreference"
        const val PREFERENCE_AUTO_DOWNLOAD_PODCAST_LIMIT = "autoDownloadPodcastsLimit"
        const val PREFERENCE_CHOOSE_FILTERS = "autoDownloadPlaylists"

        private const val PREFERENCE_CANCEL_ALL = "cancelAll"
        private const val PREFERENCE_CLEAR_DOWNLOAD_ERRORS = "clearDownloadErrors"

        fun newInstance(showToolbar: Boolean = true): AutoDownloadSettingsFragment {
            return AutoDownloadSettingsFragment().apply {
                arguments = bundleOf(
                    ARG_SHOW_TOOLBAR to showToolbar,
                )
            }
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    @Inject lateinit var podcastManager: PodcastManager

    @Inject lateinit var playlistManager: PlaylistManager

    @Inject lateinit var settings: Settings

    @Inject lateinit var analyticsTracker: AnalyticsTracker

    @Inject lateinit var theme: Theme

    private val viewModel: AutoDownloadSettingsViewModel by viewModels()
    private val cleanUpViewModel: ManualCleanupViewModel by viewModels()

    private var podcastsCategory: PreferenceCategory? = null
    private lateinit var upNextPreference: SwitchPreference
    private var newEpisodesPreference: SwitchPreference? = null
    private var onFollowPodcastPreference: SwitchPreference? = null
    private var podcastsPreference: Preference? = null
    private var podcastsAutoDownloadLimitPreference: ListPreference? = null
    private var filtersPreference: Preference? = null
    private lateinit var autoDownloadOnlyDownloadOnWifi: SwitchPreference
    private lateinit var autoDownloadOnlyWhenCharging: SwitchPreference

    private val showToolbar: Boolean
        get() = arguments?.getBoolean(ARG_SHOW_TOOLBAR) ?: true

    val toolbar
        get() = view?.findViewById<Toolbar>(R.id.toolbar)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar?.setup(title = getString(LR.string.settings_title_auto_download), navigationIcon = BackArrow, activity = activity, theme = theme)
        toolbar?.isVisible = showToolbar

        if (FeatureFlag.isEnabled(Feature.AUTO_DOWNLOAD)) {
            onFollowPodcastPreference?.let { podcastsCategory?.addPreference(it) }
            podcastsAutoDownloadLimitPreference?.let { podcastsCategory?.addPreference(it) }
        } else {
            onFollowPodcastPreference?.let { podcastsCategory?.removePreference(it) }
            podcastsAutoDownloadLimitPreference?.let { podcastsCategory?.removePreference(it) }
        }

        if (!showToolbar) {
            val listContainer = view.findViewById<View>(android.R.id.list_container)
            val childContainer = view.findViewById<View>(UR.id.frameChildFragment)

            listContainer.updateLayoutParams<FrameLayout.LayoutParams> { topMargin = 0 }
            childContainer.updateLayoutParams<FrameLayout.LayoutParams> { topMargin = 0 }
        }

        viewModel.onShown()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                settings.bottomInset.collect {
                    view.updatePadding(bottom = it)
                }
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_auto_download, rootKey)

        podcastsCategory = preferenceManager.findPreference(PREFERENCE_PODCASTS_CATEGORY)
        upNextPreference = preferenceManager.findPreference<SwitchPreference>("autoDownloadUpNext")!!
            .apply {
                setOnPreferenceChangeListener { _, newValue ->
                    viewModel.onUpNextChange(newValue as Boolean)
                    true
                }
            }
        newEpisodesPreference = preferenceManager.findPreference<SwitchPreference>(PREFERENCE_NEW_EPISODES)
            ?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    if (newValue is Boolean) {
                        viewModel.onNewEpisodesChange(newValue)
                        onNewEpisodesToggleChange(newValue.toAutoDownloadStatus(), isUserChange = true)

                        viewLifecycleOwner.lifecycleScope.launch {
                            val lowStorageDialogPresenter = LowStorageDialogPresenter(requireContext(), analyticsTracker, settings)

                            val downloadedFiles = cleanUpViewModel.state.value.diskSpaceViews.sumOf { it.episodesBytesSize }

                            if (newValue && lowStorageDialogPresenter.shouldShow(downloadedFiles)) {
                                lowStorageDialogPresenter.getDialog(
                                    totalDownloadSize = downloadedFiles,
                                    sourceView = SourceView.AUTO_DOWNLOAD,
                                    onManageDownloadsClick = {
                                        (activity as? FragmentHostListener)?.addFragment(ManualCleanupFragment.newInstance())
                                    },
                                ).show(parentFragmentManager, "low_storage_dialog")
                            }
                        }
                    }
                    true
                }
            }
        onFollowPodcastPreference = preferenceManager.findPreference<SwitchPreference>(PREFERENCE_ON_FOLLOW_PODCASTS)
            ?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    if (newValue is Boolean) {
                        viewModel.onOnFollowPodcastChange(newValue)
                    }
                    true
                }
            }
        podcastsPreference = preferenceManager.findPreference<Preference>(PREFERENCE_CHOOSE_PODCASTS)
            ?.apply {
                setOnPreferenceClickListener {
                    openPodcastsActivity()
                    true
                }
            }
        podcastsAutoDownloadLimitPreference = preferenceManager.findPreference<ListPreference>(PREFERENCE_AUTO_DOWNLOAD_PODCAST_LIMIT)
            ?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    val autoDownloadLimitSetting = (newValue as? String)
                        ?.let { AutoDownloadLimitSetting.fromPreferenceString(it) }
                        ?: AutoDownloadLimitSetting.TWO_LATEST_EPISODE

                    viewModel.onLimitDownloadsChange(autoDownloadLimitSetting)

                    updateLimitDownloadsSummary()
                    updateOnFollowSummary()
                    true
                }
            }
        filtersPreference = preferenceManager.findPreference<Preference>(PREFERENCE_CHOOSE_FILTERS)
            ?.apply {
                setOnPreferenceClickListener {
                    openPlaylistActivity()
                    true
                }
            }

        preferenceManager.findPreference<Preference>(PREFERENCE_CANCEL_ALL)
            ?.setOnPreferenceClickListener {
                context?.let {
                    Toast.makeText(it, LR.string.settings_auto_download_stopping_all, Toast.LENGTH_SHORT).show()
                }
                viewModel.stopAllDownloads()
                true
            }
        preferenceManager.findPreference<Preference>(PREFERENCE_CLEAR_DOWNLOAD_ERRORS)
            ?.setOnPreferenceClickListener {
                context?.let {
                    Toast.makeText(it, LR.string.settings_auto_download_clearing_errors, Toast.LENGTH_SHORT).show()
                }
                viewModel.clearDownloadErrors()
                true
            }

        autoDownloadOnlyDownloadOnWifi =
            preferenceManager.findPreference<SwitchPreference>("autoDownloadOnlyDownloadOnWifi")!!
                .apply {
                    setOnPreferenceChangeListener { _, newValue ->
                        viewModel.onDownloadOnlyOnUnmeteredChange(newValue as Boolean)
                        true
                    }
                }

        autoDownloadOnlyWhenCharging = preferenceManager.findPreference<SwitchPreference>("autoDownloadOnlyDownloadWhenCharging")!!
            .apply {
                setOnPreferenceChangeListener { _, newValue ->
                    (newValue as? Boolean)?.let {
                        viewModel.onDownloadOnlyWhenChargingChange(it)
                    }
                    true
                }
            }

        updateView()
    }

    override fun onResume() {
        super.onResume()

        setupAutoDownloadLimitOptions()
        updateView()
    }

    // The isUserChange parameter is used to identify when the user manually updates
    // the global auto-download toggle. This ensures that we will toggle off all podcasts
    // only when the user toggles the global auto-download toggle off.
    private fun onNewEpisodesToggleChange(status: Int, isUserChange: Boolean = false) {
        lifecycleScope.launch {
            if (status == Podcast.AUTO_DOWNLOAD_OFF && isUserChange) {
                viewModel.updateAllAutoDownloadStatus(Podcast.AUTO_DOWNLOAD_OFF)
            }
            updateNewEpisodesPreferencesVisibility(status)
            updatePodcastsSummary()
        }
    }

    private fun updateNewEpisodesPreferencesVisibility(status: Int) {
        val podcastsPreference = podcastsPreference ?: return
        val podcastsCategory = podcastsCategory ?: return

        lifecycleScope.launch {
            val isAutoDownloadEnabled = if (status == GLOBAL_AUTO_DOWNLOAD_NONE) {
                viewModel.hasEpisodesWithAutoDownloadEnabled()
            } else {
                status == Podcast.AUTO_DOWNLOAD_NEW_EPISODES
            }

            if (isAutoDownloadEnabled) {
                podcastsCategory.addPreference(podcastsPreference)
            } else {
                podcastsCategory.removePreference(podcastsPreference)
            }
        }
    }

    private fun openPodcastsActivity() {
        val fragment = PodcastSelectFragment.newInstance(source = PodcastSelectFragmentSource.DOWNLOADS)
        childFragmentManager.beginTransaction()
            .replace(UR.id.frameChildFragment, fragment)
            .addToBackStack("podcastSelect")
            .commit()
        toolbar?.title = getString(LR.string.settings_auto_download_podcasts)
    }

    override fun podcastSelectFragmentSelectionChanged(newSelection: List<String>) {
        lifecycleScope.launch(Dispatchers.Default) {
            podcastManager.findSubscribedBlocking().forEach {
                val autodownloadStatus = if (newSelection.contains(it.uuid)) Podcast.AUTO_DOWNLOAD_NEW_EPISODES else Podcast.AUTO_DOWNLOAD_OFF
                podcastManager.updateAutoDownloadStatusBlocking(it, autodownloadStatus)
            }
            launch(Dispatchers.Main) { updatePodcastsSelectedSummary() }
        }
    }

    override fun podcastSelectFragmentGetCurrentSelection(): List<String> {
        return runBlocking {
            val podcasts = async(Dispatchers.Default) { podcastManager.findPodcastsAutodownloadBlocking() }.await()
            podcasts.map { it.uuid }
        }
    }

    private fun updatePodcastsSelectedSummary() {
        lifecycleScope.launch {
            val count = async(Dispatchers.Default) { podcastManager.findPodcastsAutodownloadBlocking() }.await().count()
            val preference = preferenceManager.findPreference<Preference>(PREFERENCE_CHOOSE_PODCASTS)
            preference?.summary = context?.resources?.getStringPluralPodcastsSelected(count)
        }
    }

    private fun openPlaylistActivity() {
        val fragment = FilterSelectFragment.newInstance(FilterSelectFragment.Source.AUTO_DOWNLOAD)
        childFragmentManager.beginTransaction()
            .replace(UR.id.frameChildFragment, fragment)
            .addToBackStack("filterSelect")
            .commit()
        toolbar?.title = getString(LR.string.settings_auto_download_filters)
    }

    override fun filterSelectFragmentGetCurrentSelection(): List<String> {
        return runBlocking {
            val filters = withContext(Dispatchers.Default) { playlistManager.findAllBlocking() }.filter { it.autoDownload }
            filters.map { it.uuid }
        }
    }

    override fun filterSelectFragmentSelectionChanged(newSelection: List<String>) {
        lifecycleScope.launch(Dispatchers.Default) {
            playlistManager.findAllBlocking().forEach {
                val autoDownloadStatus = newSelection.contains(it.uuid)
                val userChanged = autoDownloadStatus != it.autoDownload
                it.autoDownload = autoDownloadStatus

                val userPlaylistUpdate = if (userChanged) {
                    UserPlaylistUpdate(
                        listOf(PlaylistProperty.AutoDownload(autoDownloadStatus)),
                        PlaylistUpdateSource.AUTO_DOWNLOAD_SETTINGS,
                    )
                } else {
                    null
                }
                playlistManager.updateBlocking(it, userPlaylistUpdate)
            }
            launch(Dispatchers.Main) { updateFiltersSelectedSummary() }
        }
    }

    private fun updateFiltersSelectedSummary() {
        lifecycleScope.launch {
            val count = withContext(Dispatchers.Default) { playlistManager.findAllBlocking() }.filter { it.autoDownload }.count()
            val preference = preferenceManager.findPreference<Preference>(PREFERENCE_CHOOSE_FILTERS)
            preference?.summary = context?.resources?.getStringPlural(count = count, singular = LR.string.filters_chosen_singular, plural = LR.string.filters_chosen_plural)
        }
    }

    private fun updateView() {
        updatePodcastsSummary()
        updateFiltersSelectedSummary()

        upNextPreference.isChecked = viewModel.getAutoDownloadUpNext()
        setupNewEpisodesToggleStatusCheck()
        setupOnFollowPodcastToggleStatusCheck()
        autoDownloadOnlyDownloadOnWifi.isChecked = viewModel.getAutoDownloadUnmeteredOnly()
        autoDownloadOnlyWhenCharging.isChecked = viewModel.getAutoDownloadOnlyWhenCharging()
        if (FeatureFlag.isEnabled(Feature.AUTO_DOWNLOAD)) {
            newEpisodesPreference?.summary = getString(LR.string.settings_auto_download_new_episodes_description)
        }
    }

    @SuppressLint("CheckResult")
    private fun updatePodcastsSummary() {
        viewModel.countPodcastsAutoDownloading().zipWith(viewModel.countPodcasts())
            .subscribeBy(
                onError = { Timber.e(it) },
                onSuccess = { (autoDownloadingCount, allCount) ->
                    val resources = context?.resources ?: return@subscribeBy
                    val summary = when (autoDownloadingCount) {
                        0 -> resources.getString(LR.string.settings_podcasts_selected_zero)
                        1 -> resources.getString(LR.string.settings_podcasts_selected_one)
                        allCount -> resources.getString(LR.string.settings_podcasts_selected_all)
                        else -> resources.getString(LR.string.settings_podcasts_selected_x, autoDownloadingCount)
                    }
                    podcastsPreference?.summary = summary
                },
            )
    }

    private fun setupNewEpisodesToggleStatusCheck() {
        lifecycleScope.launch {
            val newValue = viewModel.hasEpisodesWithAutoDownloadEnabled()
            newEpisodesPreference?.isChecked = newValue
            onNewEpisodesToggleChange(newValue.toAutoDownloadStatus())
        }
    }

    private fun setupOnFollowPodcastToggleStatusCheck() {
        onFollowPodcastPreference?.isChecked = viewModel.isAutoDownloadOnFollowPodcastEnabled()
    }

    override fun onBackPressed(): Boolean {
        if (childFragmentManager.backStackEntryCount > 0) {
            childFragmentManager.popBackStack()
            toolbar?.title = getString(LR.string.settings_title_auto_download)
            return true
        }
        return false
    }

    override fun getBackstackCount(): Int {
        return childFragmentManager.backStackEntryCount
    }

    override fun onPause() {
        super.onPause()
        viewModel.onFragmentPause(activity?.isChangingConfigurations)
    }

    private fun setupAutoDownloadLimitOptions() {
        podcastsAutoDownloadLimitPreference?.apply {
            val options = AutoDownloadLimitSetting.entries
            entries = options.map { getString(it.titleRes) }.toTypedArray()
            entryValues = options.map { it.id.toString() }.toTypedArray()
            value = settings.autoDownloadLimit.value.id.toString()
        }
        updateLimitDownloadsSummary()
        updateOnFollowSummary()
    }

    private fun updateLimitDownloadsSummary() {
        podcastsAutoDownloadLimitPreference?.summary = getString(settings.autoDownloadLimit.value.titleRes)
    }

    private fun updateOnFollowSummary() {
        val limitDownloads = AutoDownloadLimitSetting.getNumberOfEpisodes(viewModel.getLimitDownload())

        val localizedLimitDownloads = when (limitDownloads) {
            2 -> resources.getString(LR.string.number_two)
            3 -> resources.getString(LR.string.number_three)
            5 -> resources.getString(LR.string.number_five)
            10 -> resources.getString(LR.string.number_ten)
            else -> limitDownloads.toString()
        }

        onFollowPodcastPreference?.summary = resources.getQuantityString(
            LR.plurals.settings_auto_download_on_follow_podcast_description, limitDownloads, localizedLimitDownloads,
        )
    }
}

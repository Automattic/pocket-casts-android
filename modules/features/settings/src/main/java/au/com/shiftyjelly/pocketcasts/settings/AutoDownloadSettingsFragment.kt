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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPlural
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralPodcastsSelected
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.Settings.Companion.PREFERENCE_PODCAST_AUTO_DOWNLOAD_ON_UNMETERED
import au.com.shiftyjelly.pocketcasts.preferences.Settings.Companion.PREFERENCE_PODCAST_AUTO_DOWNLOAD_WHEN_CHARGING
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistProperty
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistUpdateSource
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserPlaylistUpdate
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.AutoDownloadSettingsViewModel
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.fragments.FilterSelectFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragmentSource
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
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
        const val PREFERENCE_CHOOSE_PODCASTS = "autoDownloadPodcastsPreference"
        const val PREFERENCE_CHOOSE_FILTERS = "autoDownloadPlaylists"

        private const val PREFERENCE_CANCEL_ALL = "cancelAll"
        private const val PREFERENCE_CLEAR_DOWNLOAD_ERRORS = "clearDownloadErrors"

        fun newInstance(showToolbar: Boolean = true): AutoDownloadSettingsFragment {
            return AutoDownloadSettingsFragment().apply {
                arguments = bundleOf(
                    ARG_SHOW_TOOLBAR to showToolbar
                )
            }
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    @Inject lateinit var podcastManager: PodcastManager
    @Inject lateinit var playlistManager: PlaylistManager
    @Inject lateinit var settings: Settings
    @Inject lateinit var theme: Theme

    private val viewModel: AutoDownloadSettingsViewModel by viewModels()

    private var podcastsCategory: PreferenceCategory? = null
    private lateinit var upNextPreference: SwitchPreference
    private var newEpisodesPreference: SwitchPreference? = null
    private var podcastsPreference: Preference? = null
    private var filtersPreference: Preference? = null

    private val showToolbar: Boolean
        get() = arguments?.getBoolean(ARG_SHOW_TOOLBAR) ?: true

    val toolbar
        get() = view?.findViewById<Toolbar>(R.id.toolbar)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar?.setup(title = getString(LR.string.settings_title_auto_download), navigationIcon = BackArrow, activity = activity, theme = theme)
        toolbar?.isVisible = showToolbar

        if (!showToolbar) {
            val listContainer = view.findViewById<View>(android.R.id.list_container)
            val childContainer = view.findViewById<View>(UR.id.frameChildFragment)

            listContainer.updateLayoutParams<FrameLayout.LayoutParams> { topMargin = 0 }
            childContainer.updateLayoutParams<FrameLayout.LayoutParams> { topMargin = 0 }
        }

        viewModel.onShown()
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
                        onChangeNewEpisodes(newValue)
                        viewModel.onNewEpisodesChange(newValue)
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
        filtersPreference = preferenceManager.findPreference<Preference>(PREFERENCE_CHOOSE_FILTERS)
            ?.apply {
                setOnPreferenceClickListener {
                    openPlaylistActivity()
                    true
                }
            }

        updateView()

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

        preferenceManager.findPreference<SwitchPreference>(PREFERENCE_PODCAST_AUTO_DOWNLOAD_ON_UNMETERED)
            ?.setOnPreferenceChangeListener { _, newValue ->
                (newValue as? Boolean)?.let {
                    viewModel.onDownloadOnlyOnUnmeteredChange(it)
                }
                true
            }

        preferenceManager.findPreference<SwitchPreference>(PREFERENCE_PODCAST_AUTO_DOWNLOAD_WHEN_CHARGING)
            ?.setOnPreferenceChangeListener { _, newValue ->
                (newValue as? Boolean)?.let {
                    viewModel.onDownloadOnlyWhenChargingChange(it)
                }
                true
            }
    }

    override fun onResume() {
        super.onResume()

        updateView()
    }

    private fun onChangeNewEpisodes(on: Boolean) {
        lifecycleScope.launch {
            if (!on) {
                async(Dispatchers.Default) { podcastManager.updateAllAutoDownloadStatus(Podcast.AUTO_DOWNLOAD_OFF) }.await()
            }
            updateNewEpisodesSwitch(on)
            updatePodcastsSummary()
        }
    }

    private fun updateNewEpisodesSwitch(on: Boolean) {
        val podcastsPreference = podcastsPreference ?: return
        val podcastsCategory = podcastsCategory ?: return
        if (on) {
            podcastsCategory.addPreference(podcastsPreference)
        } else {
            podcastsCategory.removePreference(podcastsPreference)
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
            podcastManager.findSubscribed().forEach {
                val autodownloadStatus = if (newSelection.contains(it.uuid)) Podcast.AUTO_DOWNLOAD_NEW_EPISODES else Podcast.AUTO_DOWNLOAD_OFF
                podcastManager.updateAutoDownloadStatus(it, autodownloadStatus)
            }
            launch(Dispatchers.Main) { updatePodcastsSelectedSummary() }
        }
    }

    override fun podcastSelectFragmentGetCurrentSelection(): List<String> {
        return runBlocking {
            val podcasts = async(Dispatchers.Default) { podcastManager.findPodcastsAutodownload() }.await()
            podcasts.map { it.uuid }
        }
    }

    private fun updatePodcastsSelectedSummary() {
        lifecycleScope.launch {
            val count = async(Dispatchers.Default) { podcastManager.findPodcastsAutodownload() }.await().count()
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
            val filters = withContext(Dispatchers.Default) { playlistManager.findAll() }.filter { it.autoDownload }
            filters.map { it.uuid }
        }
    }

    override fun filterSelectFragmentSelectionChanged(newSelection: List<String>) {
        lifecycleScope.launch(Dispatchers.Default) {
            playlistManager.findAll().forEach {
                val autoDownloadStatus = newSelection.contains(it.uuid)
                val userChanged = autoDownloadStatus != it.autoDownload
                it.autoDownload = autoDownloadStatus

                val userPlaylistUpdate = if (userChanged) {
                    UserPlaylistUpdate(
                        listOf(PlaylistProperty.AutoDownload(autoDownloadStatus)),
                        PlaylistUpdateSource.AUTO_DOWNLOAD_SETTINGS
                    )
                } else null
                playlistManager.update(it, userPlaylistUpdate)
            }
            launch(Dispatchers.Main) { updateFiltersSelectedSummary() }
        }
    }

    private fun updateFiltersSelectedSummary() {
        lifecycleScope.launch {
            val count = withContext(Dispatchers.Default) { playlistManager.findAll() }.filter { it.autoDownload }.count()
            val preference = preferenceManager.findPreference<Preference>(PREFERENCE_CHOOSE_FILTERS)
            preference?.summary = context?.resources?.getStringPlural(count = count, singular = LR.string.filters_chosen_singular, plural = LR.string.filters_chosen_plural)
        }
    }

    private fun updateView() {
        updateAutoDownloadUpNext()
        updatePodcastsSummary()
        updateFiltersSelectedSummary()
        updateNewEpisodesSwitch()
    }

    private fun updateAutoDownloadUpNext() {
        upNextPreference.isChecked = viewModel.getAutoDownloadUpNext()
    }

    private fun countPodcastsAutoDownloading(): Single<Int> {
        return podcastManager.countDownloadStatusRx(Podcast.AUTO_DOWNLOAD_NEW_EPISODES)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    private fun countPodcasts(): Single<Int> {
        return podcastManager.countSubscribedRx()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
    }

    @SuppressLint("CheckResult")
    private fun updatePodcastsSummary() {
        countPodcastsAutoDownloading().zipWith(countPodcasts())
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
                }
            )
    }

    @SuppressLint("CheckResult")
    private fun updateNewEpisodesSwitch() {
        countPodcastsAutoDownloading()
            .map { it > 0 }
            .subscribeBy(
                onError = { Timber.e(it) },
                onSuccess = { on ->
                    updateNewEpisodesSwitch(on)
                    newEpisodesPreference?.isChecked = on
                }
            )
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
}

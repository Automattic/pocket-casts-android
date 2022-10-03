package au.com.shiftyjelly.pocketcasts.ui

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.Observer
import androidx.transition.Slide
import au.com.shiftyjelly.pocketcasts.R
import au.com.shiftyjelly.pocketcasts.account.AccountActivity
import au.com.shiftyjelly.pocketcasts.account.PromoCodeUpgradedFragment
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.databinding.ActivityMainBinding
import au.com.shiftyjelly.pocketcasts.discover.view.DiscoverFragment
import au.com.shiftyjelly.pocketcasts.filters.FiltersFragment
import au.com.shiftyjelly.pocketcasts.localization.helper.LocaliseHelper
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.navigation.BottomNavigator
import au.com.shiftyjelly.pocketcasts.navigation.FragmentInfo
import au.com.shiftyjelly.pocketcasts.navigation.NavigatorAction
import au.com.shiftyjelly.pocketcasts.player.view.PlayerBottomSheet
import au.com.shiftyjelly.pocketcasts.player.view.PlayerContainerFragment
import au.com.shiftyjelly.pocketcasts.player.view.UpNextFragment
import au.com.shiftyjelly.pocketcasts.player.view.dialog.MiniPlayerDialog
import au.com.shiftyjelly.pocketcasts.player.view.video.VideoActivity
import au.com.shiftyjelly.pocketcasts.podcasts.view.ProfileEpisodeListFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.EpisodeFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcasts.PodcastsFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.share.ShareListIncomingFragment
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.profile.ProfileFragment
import au.com.shiftyjelly.pocketcasts.profile.SubCancelledFragment
import au.com.shiftyjelly.pocketcasts.profile.TrialFinishedFragment
import au.com.shiftyjelly.pocketcasts.profile.cloud.CloudFileBottomSheetFragment
import au.com.shiftyjelly.pocketcasts.profile.cloud.CloudFilesFragment
import au.com.shiftyjelly.pocketcasts.profile.sonos.SonosAppLinkActivity
import au.com.shiftyjelly.pocketcasts.repositories.opml.OpmlImportTask
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager.PlaybackSource
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsTask
import au.com.shiftyjelly.pocketcasts.repositories.shortcuts.PocketCastsShortcuts
import au.com.shiftyjelly.pocketcasts.repositories.shortcuts.PocketCastsShortcuts.INTENT_EXTRA_PAGE
import au.com.shiftyjelly.pocketcasts.repositories.shortcuts.PocketCastsShortcuts.INTENT_EXTRA_PLAYLIST_ID
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.search.SearchFragment
import au.com.shiftyjelly.pocketcasts.servers.ServerCallback
import au.com.shiftyjelly.pocketcasts.servers.ServerManager
import au.com.shiftyjelly.pocketcasts.servers.discover.PodcastSearch
import au.com.shiftyjelly.pocketcasts.settings.whatsnew.WhatsNewFragment
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.SentryHelper
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.utils.observeOnce
import au.com.shiftyjelly.pocketcasts.view.BottomNavHideManager
import au.com.shiftyjelly.pocketcasts.view.LockableBottomSheetBehavior
import au.com.shiftyjelly.pocketcasts.views.extensions.showAllowingStateLoss
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import au.com.shiftyjelly.pocketcasts.views.helper.IntentUtil
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import au.com.shiftyjelly.pocketcasts.views.helper.WarningsHelper
import au.com.shiftyjelly.pocketcasts.views.helper.WhatsNew
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.views.R as VR
import com.google.android.material.R as MR

private const val SAVEDSTATE_PLAYER_OPEN = "player_open"

@AndroidEntryPoint
class MainActivity :
    AppCompatActivity(),
    FragmentHostListener,
    PlayerBottomSheet.PlayerBottomSheetListener,
    SearchFragment.Listener,
    CoroutineScope {

    companion object {
        private const val INITIAL_KEY = "initial"
        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }

        const val PROMOCODE_REQUEST_CODE = 2
    }

    @Inject lateinit var multiSelectHelper: MultiSelectHelper
    @Inject lateinit var playbackManager: PlaybackManager
    @Inject lateinit var podcastManager: PodcastManager
    @Inject lateinit var playlistManager: PlaylistManager
    @Inject lateinit var episodeManager: EpisodeManager
    @Inject lateinit var serverManager: ServerManager
    @Inject lateinit var theme: Theme
    @Inject lateinit var settings: Settings
    @Inject lateinit var subscriptionManager: SubscriptionManager
    @Inject lateinit var userEpisodeManager: UserEpisodeManager
    @Inject lateinit var warningsHelper: WarningsHelper
    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private lateinit var bottomNavHideManager: BottomNavHideManager
    private lateinit var observeUpNext: LiveData<UpNextQueue.State>

    private val viewModel: MainActivityViewModel by viewModels()
    private val disposables = CompositeDisposable()
    private var videoPlayerShown: Boolean = false
    private var overrideNextRefreshTimer: Boolean = false

    private val childrenWithBackStack: List<HasBackstack>
        get() = supportFragmentManager.fragments.filterIsInstance<HasBackstack>()

    private val frameBottomSheetBehavior: LockableBottomSheetBehavior<View>
        get() = getBottomSheetBehavior()

    private var bottomSheetTag: String? = null
    private val bottomSheetQueue: MutableList<(() -> Unit)?> = mutableListOf()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private lateinit var binding: ActivityMainBinding
    private lateinit var navigator: BottomNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("Main Activity onCreate")
        super.onCreate(savedInstanceState)
        theme.setupThemeForConfig(this, resources.configuration)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        var selectedTab = settings.selectedTab()
        val tabs = mapOf(
            VR.id.navigation_podcasts to { FragmentInfo(PodcastsFragment(), true) },
            VR.id.navigation_filters to { FragmentInfo(FiltersFragment(), true) },
            VR.id.navigation_discover to { FragmentInfo(DiscoverFragment(), false) },
            VR.id.navigation_profile to { FragmentInfo(ProfileFragment(), true) }
        )

        if (!tabs.keys.contains(selectedTab)) {
            // Guard against tab ids changing and settings having an out of date copy
            settings.setSelectedTab(null)
            selectedTab = null
        }

        if (selectedTab == null) {
            // We cheat a little here and block because we need to know the podcast count to work out the default tab.
            // The navigator has to be initialised in onCreate or else race conditions happen
            val podcastCount = runBlocking(Dispatchers.Default) { podcastManager.countSubscribed() }
            selectedTab =
                if (podcastCount == 0) VR.id.navigation_discover else VR.id.navigation_podcasts
        }

        navigator = BottomNavigator.onCreateWithDetachability(
            fragmentContainer = R.id.mainFragment,
            modalContainer = R.id.fragmentOverBottomNav,
            bottomNavigationView = binding.bottomNavigation,
            rootFragmentsFactory = tabs,
            defaultTab = selectedTab,
            activity = this
        )

        setupPlayerViews()

        if (savedInstanceState != null) {
            val videoComingToPortrait =
                (playbackManager.isPlaying() && playbackManager.getCurrentEpisode()?.isVideo == true && resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT && viewModel.isPlayerOpen)
            if (savedInstanceState.getBoolean(
                    SAVEDSTATE_PLAYER_OPEN,
                    false
                ) && !playbackManager.upNextQueue.isEmpty || videoComingToPortrait
            ) {
                binding.playerBottomSheet.openPlayer()
            }
        } else {
            trackTabOpened(selectedTab, isInitial = true)
        }
        navigator.infoStream()
            .doOnNext {
                updateStatusBar()
                if (it is NavigatorAction.TabSwitched) {
                    val currentTab = navigator.currentTab()
                    if (settings.selectedTab() != currentTab) {
                        trackTabOpened(currentTab)
                        when (currentTab) {
                            VR.id.navigation_podcasts -> FirebaseAnalyticsTracker.navigatedToPodcasts()
                            VR.id.navigation_filters -> FirebaseAnalyticsTracker.navigatedToFilters()
                            VR.id.navigation_discover -> FirebaseAnalyticsTracker.navigatedToDiscover()
                            VR.id.navigation_profile -> FirebaseAnalyticsTracker.navigatedToProfile()
                        }
                    }
                    settings.setSelectedTab(currentTab)
                }
            }
            .subscribe()
            .addTo(disposables)

        handleIntent(intent, savedInstanceState)

        updateSystemColors()
    }

    override fun onStart() {
        super.onStart()
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (!videoPlayerShown && playbackManager.getCurrentEpisode()?.isVideo == true && playbackManager.isPlaybackLocal() && playbackManager.isPlaying() && viewModel.isPlayerOpen) {
                openFullscreenViewPlayer()
                videoPlayerShown = true
            } else {
                videoPlayerShown = false
            }
        }
    }

    private fun openFullscreenViewPlayer() {
        startActivity(VideoActivity.buildIntent(context = this))
    }

    override fun onResume() {
        super.onResume()

        if (settings.selectedTab() == VR.id.navigation_discover) {
            FirebaseAnalyticsTracker.navigatedToDiscover()
        }

        refreshApp()

        addLineView()
    }

    override fun onPause() {
        super.onPause()
        removeLineView()
    }

    private fun addLineView() {
        removeLineView()
        if (theme.activeTheme != Theme.ThemeType.RADIOACTIVE) return
        binding.radioactiveLineView.isVisible = true
    }

    private fun removeLineView() {
        binding.radioactiveLineView.isVisible = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(SAVEDSTATE_PLAYER_OPEN, binding.playerBottomSheet.isPlayerOpen)
    }

    override fun overrideNextRefreshTimer() {
        overrideNextRefreshTimer = true
    }

    private fun refreshApp() {
        if (overrideNextRefreshTimer) {
            podcastManager.refreshPodcasts("open app - ignore timer")
            overrideNextRefreshTimer = false
        } else {
            // delay the refresh to allow the UI to load
            Observable.timer(1, TimeUnit.SECONDS, Schedulers.io())
                .doOnNext {
                    podcastManager.refreshPodcastsIfRequired(fromLog = "open app")
                }
                .subscribeBy(onError = { Timber.e(it) })
                .addTo(disposables)
        }
        PocketCastsShortcuts.update(playlistManager, true, this)

        subscriptionManager.refreshPurchases()

        // Schedule next refresh in the background
        RefreshPodcastsTask.scheduleOrCancel(this@MainActivity, settings)
    }

    @Suppress("DEPRECATION")
    private suspend fun refreshAppAndWait() = withContext(Dispatchers.Main) {
        val dialog = android.app.ProgressDialog.show(this@MainActivity, getString(LR.string.loading), getString(LR.string.please_wait), true)
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Running refresh from refresh and wait")
        RefreshPodcastsTask.runNowSync(application)

        UiUtil.hideProgressDialog(dialog)
    }

    override fun onDestroy() {
        super.onDestroy()

        disposables.clear()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (multiSelectHelper.isMultiSelecting) {
            multiSelectHelper.isMultiSelecting = false
            return
        }

        if (frameBottomSheetBehavior.state != BottomSheetBehavior.STATE_COLLAPSED) {
            frameBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            return
        }

        if (navigator.isShowingModal()) {
            val currentFragment = navigator.currentFragment()
            if (currentFragment is HasBackstack) {
                val handled = currentFragment.onBackPressed()
                if (!handled) {
                    navigator.pop()
                }
                return
            } else {
                navigator.pop()
                return
            }
        }

        // Check for embedded up next fragment being shown in player container
        val playerContainerFragment =
            supportFragmentManager.fragments.find { it is PlayerContainerFragment } as? PlayerContainerFragment
        if (playerContainerFragment != null) {
            if (playerContainerFragment.getBackstackCount() > 0) {
                if (playerContainerFragment.onBackPressed()) {
                    return
                }
            }
        }

        if (binding.playerBottomSheet.isPlayerOpen) {
            binding.playerBottomSheet.closePlayer()
            return
        }

        // Some fragments have child fragments that require a back stack, we need to check for those
        // before popping the main back stack
        if (childrenWithBackStack.count() > 0) {
            var handled = false
            var index = 0
            do {
                val child = childrenWithBackStack[index++]
                if (child is Fragment && child.userVisibleHint) {
                    handled = child.onBackPressed()
                }
            } while (!handled && index < childrenWithBackStack.count())
            if (handled) {
                return
            }
        }

        if (navigator.isAtRootOfStack() || !navigator.pop()) {
            super.onBackPressed()
        }
    }

    override fun updateStatusBar() {
        val topFragment = navigator.currentFragment()
        val color = if (binding.playerBottomSheet.isPlayerOpen) {
            val playerBgColor = theme.playerBackgroundColor(viewModel.lastPlaybackState?.podcast)
            StatusBarColor.Custom(playerBgColor, true)
        } else if (topFragment is BaseFragment) {
            topFragment.statusBarColor
        } else {
            null
        }

        if (color != null) {
            theme.updateWindowStatusBar(window = window, statusBarColor = color, context = this)
        }
    }

    private fun updateNavAndStatusColors(playerOpen: Boolean, playingPodcast: Podcast?) {
        if (playerOpen) {
            val playerBgColor = theme.playerBackgroundColor(playingPodcast)
            theme.setNavigationBarColor(window, true, playerBgColor)
        } else {
            theme.setNavigationBarColor(
                window,
                theme.isDarkTheme,
                ThemeColor.primaryUi03(theme.activeTheme)
            )
        }

        updateStatusBar()
    }

    override fun onUpNextClicked() {
        showBottomSheet(UpNextFragment())
    }

    override fun onMiniPlayerLongClick() {
        MiniPlayerDialog(playbackManager, podcastManager, episodeManager, supportFragmentManager).show(this)
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Suppress("DEPRECATION")
    private fun setupPlayerViews() {
        binding.playerBottomSheet.listener = this

        viewModel.playbackState.observe(this) { state ->
            if (viewModel.lastPlaybackState?.episodeUuid != state.episodeUuid || (viewModel.lastPlaybackState?.isPlaying == false && state.isPlaying)) {
                launch(Dispatchers.Default) {
                    val playable = episodeManager.findPlayableByUuid(state.episodeUuid)
                    if (playable?.isVideo == true && state.isPlaying) {
                        launch(Dispatchers.Main) {
                            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                                binding.playerBottomSheet.openPlayer()
                            } else {
                                openFullscreenViewPlayer()
                            }
                        }
                    }
                }
            }

            if (viewModel.isPlayerOpen && viewModel.lastPlaybackState?.episodeUuid != state.episodeUuid) {
                updateNavAndStatusColors(true, state.podcast)
            }

            updatePlaybackState(state)

            viewModel.lastPlaybackState = state
        }

        val upNextQueueObservable =
            playbackManager.upNextQueue.getChangesObservableWithLiveCurrentEpisode(
                episodeManager,
                podcastManager
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .toFlowable(BackpressureStrategy.LATEST)
        observeUpNext = LiveDataReactiveStreams.fromPublisher(upNextQueueObservable)
        observeUpNext.observe(this) { upNext ->
            binding.playerBottomSheet.setUpNext(upNext, theme)
        }

        viewModel.signInState.observe(this) { signinState ->
            val status = (signinState as? SignInState.SignedIn)?.subscriptionStatus

            if (signinState.isSignedInAsPlus) {
                status?.let {
                    if (viewModel.shouldShowCancelled(it)) {
                        val cancelledFragment = SubCancelledFragment.newInstance()
                        showBottomSheet(cancelledFragment)
                    }
                }
            } else {
                GlobalScope.launch { userEpisodeManager.removeCloudStatusFromFiles(playbackManager) }
            }

            if (viewModel.shouldShowTrialFinished(signinState)) {
                val trialFinished = TrialFinishedFragment()
                showBottomSheet(trialFinished)

                settings.setTrialFinishedSeen(true)
            }
        }

        val lastSeenVersionCode = settings.getWhatsNewVersionCode()
        val migratedVersion = settings.getMigratedVersionCode()
        if (migratedVersion != 0) { // We don't want to show this to new users, there is a race condition between this and the version migration
            val whatsNewShouldBeShown = WhatsNew.isWhatsNewNewerThan(lastSeenVersionCode)
            if (whatsNewShouldBeShown) {
                settings.setWhatsNewVersionCode(Settings.WHATS_NEW_VERSION_CODE)
                val fragment = WhatsNewFragment()
                showBottomSheet(fragment, swipeEnabled = false)
            }
        }

        bottomNavHideManager =
            BottomNavHideManager(findViewById(R.id.root), binding.bottomNavigation)
        frameBottomSheetBehavior.setBottomSheetCallback(object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {}

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                        supportFragmentManager.findFragmentByTag(bottomSheetTag)?.let {
                            removeBottomSheetFragment(it)
                        }

                        binding.playerBottomSheet.isDragEnabled = true

                        updateNavAndStatusColors(playerOpen = viewModel.isPlayerOpen, viewModel.lastPlaybackState?.podcast)
                    } else {
                        binding.playerBottomSheet.isDragEnabled = false
                    }
                }
            })
    }

    override fun updatePlayerView() {
        binding.playerBottomSheet.sheetBehavior?.updateScrollingChild()
    }

    override fun getPlayerBottomSheetState(): Int {
        return binding.playerBottomSheet.sheetBehavior?.state ?: BottomSheetBehavior.STATE_COLLAPSED
    }

    override fun lockPlayerBottomSheet(locked: Boolean) {
        binding.playerBottomSheet.isDragEnabled = !locked
    }

    private fun updatePlaybackState(state: PlaybackState) {
        binding.playerBottomSheet.setPlaybackState(state)

        if ((state.isPlaying || state.isBuffering) && settings.keepScreenAwake()) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun snackBarView(): View {
        return binding.snackbarFragment
    }

    override fun onMiniPlayerHidden() {
        binding.mainFragment.updatePadding(
            bottom = resources.getDimension(MR.dimen.design_bottom_navigation_height).toInt()
        )
        binding.snackbarFragment.updatePadding(bottom = binding.mainFragment.paddingBottom)
    }

    override fun onMiniPlayerVisible() {
        binding.mainFragment.updatePadding(
            bottom = resources.getDimension(MR.dimen.design_bottom_navigation_height)
                .toInt() + resources.getDimension(R.dimen.miniPlayerHeight).toInt()
        )
        binding.snackbarFragment.updatePadding(bottom = binding.mainFragment.paddingBottom)

        // Handle up next shortcut
        if (intent.getStringExtra(INTENT_EXTRA_PAGE) == "upnext") {
            intent.putExtra(INTENT_EXTRA_PAGE, null as String?)
            binding.playerBottomSheet.openPlayer()
            onUpNextClicked()
        }
    }

    override fun onPlayerBottomSheetSlide(slideOffset: Float) {
        bottomNavHideManager.onSlide(slideOffset)
    }

    override fun updateSystemColors() {
        updateNavAndStatusColors(viewModel.isPlayerOpen, viewModel.lastPlaybackState?.podcast)
    }

    override fun onPlayerOpen() {
        launch {
            val isVideo = playbackManager.upNextQueue.currentEpisode?.isVideo ?: false
            if (isVideo) {
                playbackManager.preparePlayer()
            }
        }

        updateNavAndStatusColors(true, viewModel.lastPlaybackState?.podcast)
        UiUtil.hideKeyboard(binding.root)

        FirebaseAnalyticsTracker.nowPlayingOpen()

        viewModel.isPlayerOpen = true
    }

    override fun onPlayerClosed() {
        updateNavAndStatusColors(false, null)

        viewModel.isPlayerOpen = false
    }

    override fun openTab(tabId: Int) {
        navigator.switchTab(tabId)
    }

    override fun showBottomSheet(fragment: Fragment) {
        showBottomSheet(fragment, showImmediate = true, swipeEnabled = true)
    }

    private fun showBottomSheet(
        fragment: Fragment,
        showImmediate: Boolean = true,
        swipeEnabled: Boolean = true
    ) {
        if (bottomSheetTag != null && !showImmediate) {
            bottomSheetQueue.add { showBottomSheet(fragment) }
            return
        }

        supportFragmentManager.commitNow {
            bottomSheetTag = fragment::class.java.name
            replace(R.id.frameBottomSheet, fragment, bottomSheetTag)
        }

        frameBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        binding.frameBottomSheet.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        frameBottomSheetBehavior.swipeEnabled = swipeEnabled
    }

    override fun bottomSheetClosePressed(fragment: Fragment) {
        frameBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        binding.frameBottomSheet.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun removeBottomSheetFragment(fragment: Fragment) {
        val tag = fragment::class.java.name
        supportFragmentManager.findFragmentByTag(tag)?.let {
            supportFragmentManager.commitNow(allowStateLoss = true) {
                remove(it)

                updateStatusBar()
                bottomSheetTag = null

                if (bottomSheetQueue.isNotEmpty()) {
                    val next = bottomSheetQueue.removeAt(0)
                    next?.invoke()
                }
            }
        }
    }

    override fun showModal(fragment: Fragment) {
        fragment.enterTransition = Slide()
        fragment.exitTransition = Slide()
        navigator.addFragment(fragment, modal = true)

        updateStatusBar()
    }

    override fun closeModal(fragment: Fragment) {
        navigator.pop()
        updateStatusBar()
    }

    override fun openPlayer() {
        binding.playerBottomSheet.openPlayer()
    }

    override fun closePlayer() {
        binding.playerBottomSheet.closePlayer()
    }

    override fun onPlayClicked() {
        playbackManager.playbackSource = PlaybackSource.MINIPLAYER
        if (playbackManager.shouldWarnAboutPlayback()) {
            launch {
                // show the stream warning if the episode isn't downloaded
                playbackManager.getCurrentEpisode()?.let { episode ->
                    launch(Dispatchers.Main) {
                        if (episode.isDownloaded) {
                            playbackManager.playQueue()
                            warningsHelper.showBatteryWarningSnackbarIfAppropriate()
                        } else {
                            warningsHelper.streamingWarningDialog(episode)
                                .show(supportFragmentManager, "streaming dialog")
                        }
                    }
                }
            }
        } else {
            playbackManager.playQueue()
            warningsHelper.showBatteryWarningSnackbarIfAppropriate()
        }
    }

    override fun onPauseClicked() {
        playbackManager.playbackSource = PlaybackSource.MINIPLAYER
        playbackManager.pause()
    }

    override fun onSkipBackwardClicked() {
        playbackManager.playbackSource = PlaybackSource.MINIPLAYER
        playbackManager.skipBackward()
    }

    override fun onSkipForwardClicked() {
        playbackManager.playbackSource = PlaybackSource.MINIPLAYER
        playbackManager.skipForward()
    }

    override fun addFragment(fragment: Fragment, onTop: Boolean) {
        navigator.addFragment(fragment, onTop = onTop)
    }

    override fun replaceFragment(fragment: Fragment) {
        navigator.addFragment(fragment)
    }

    override fun closeToRoot() {
        navigator.clearAll()
    }

    override fun closePodcastsToRoot() {
        navigator.reset(tab = VR.id.navigation_podcasts, resetRootFragment = true)
    }

    override fun setSupportActionBar(toolbar: Toolbar?) {
        super.setSupportActionBar(toolbar)

        if (!navigator.isAtRootOfStack()) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onSearchPodcastClick(podcastUuid: String) {
        val fragment = PodcastFragment.newInstance(podcastUuid)
        addFragment(fragment)
    }

    override fun onSearchFolderClick(folderUuid: String) {
        val fragment = PodcastsFragment.newInstance(folderUuid)
        addFragment(fragment)
    }

    override fun showAccountUpgradeNow(autoSelectPlus: Boolean) {
        showAccountUpgradeNowDialog(shouldClose = false, autoSelectPlus = autoSelectPlus)
    }

    private fun showAccountUpgradeNowDialog(shouldClose: Boolean, autoSelectPlus: Boolean = false) {
        val observer: Observer<SignInState> = Observer { value ->
            val intent: Intent
            if (value != null && value.isSignedInAsFree) {
                intent =
                    AccountActivity.newUpgradeInstance(this)
            } else if (autoSelectPlus) {
                intent =
                    AccountActivity.newAutoSelectPlusInstance(
                        this
                    )
            } else {
                intent =
                    Intent(this, AccountActivity::class.java)
            }
            startActivity(intent)

            if (shouldClose) {
                finish()
            }
        }

        viewModel.signInState.observeOnce(this, observer)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent, null)
    }

    @Suppress("DEPRECATION")
    private fun handleIntent(intent: Intent?, savedInstanceState: Bundle?) {
        val action = intent?.action
        if (action == null || savedInstanceState != null) {
            return
        }

        try {
            // downloading episode notification tapped
            if (action == Settings.INTENT_OPEN_APP_DOWNLOADING) {
                closeToRoot()
                addFragment(ProfileEpisodeListFragment.newInstance(ProfileEpisodeListFragment.Mode.Downloaded))
            }
            // new episode notification tapped
            else if (intent.extras?.containsKey(Settings.INTENT_OPEN_APP_EPISODE_UUID) ?: false) {
                // intents were being reused for notifications so we had to use the extra to pass action
                val episodeUuid =
                    intent.extras?.getString(Settings.INTENT_OPEN_APP_EPISODE_UUID, null)
                openEpisodeDialog(episodeUuid, null, forceDark = false)
            } else if (action == Intent.ACTION_VIEW) {
                val extraPage = intent.extras?.getString(INTENT_EXTRA_PAGE, null)
                if (extraPage != null) {
                    when (extraPage) {
                        "podcasts" -> openTab(VR.id.navigation_podcasts)
                        "search" -> openTab(VR.id.navigation_discover)
                        "playlist" -> {
                            val playlistId = intent.extras?.getLong(INTENT_EXTRA_PLAYLIST_ID, -1)
                                ?: -1
                            launch(Dispatchers.Default) {
                                playlistManager.findById(playlistId)?.let {
                                    withContext(Dispatchers.Main) {
                                        settings.setSelectedFilter(it.uuid)

                                        // HACK: Go diving to find if a filter fragment
                                        openTab(VR.id.navigation_filters)
                                        val filtersFragment =
                                            supportFragmentManager.fragments.find { it is FiltersFragment } as? FiltersFragment
                                        filtersFragment?.openPlaylist(it)
                                    }
                                }
                            }
                        }
                    }
                } else if (IntentUtil.isPocketCastsWebsite(intent)) {
                    // when the user goes to https://pocketcasts.com/get it should either open the play store or the user's app
                    return
                } else if (IntentUtil.isPodloveUrl(intent)) {
                    openPodcastUrl(IntentUtil.getPodloveUrl(intent))
                    return
                } else if (IntentUtil.isSonosAppLinkUrl(intent)) {
                    startActivityForResult(
                        SonosAppLinkActivity.buildIntent(intent, this),
                        SonosAppLinkActivity.SONOS_APP_ACTIVITY_RESULT
                    )
                    return
                } else if (IntentUtil.isPodcastListShare(intent) || IntentUtil.isPodcastListShareMobile(
                        intent
                    )
                ) {
                    intent.data?.path?.let { addFragment(ShareListIncomingFragment.newInstance(it)) }
                    return
                } else if (IntentUtil.isSubscribeOnAndroidUrl(intent)) {
                    openPodcastUrl(IntentUtil.getSubscribeOnAndroidUrl(intent))
                    return
                } else if (IntentUtil.isItunesLink(intent)) {
                    openPodcastUrl(IntentUtil.getUrl(intent))
                    return
                } else if (IntentUtil.isCloudFilesIntent(intent)) {
                    openCloudFiles()
                    return
                } else if (IntentUtil.isUpgradeIntent(intent)) {
                    showAccountUpgradeNowDialog(shouldClose = true)
                    return
                } else if (IntentUtil.isPromoCodeIntent(intent)) {
                    openPromoCode(intent)
                    return
                } else if (IntentUtil.isShareLink(intent)) { // Must go last, catches all pktc links
                    openSharingUrl(intent)
                    return
                }

                val scheme = intent.scheme
                if (scheme != null) {
                    // import opml from email
                    if (scheme == "content") {
                        val uri = Uri.parse(intent.dataString)
                        OpmlImportTask.run(uri, this)
                    }
                    // import podcast feed
                    else if (scheme == "rss" || scheme == "feed" || scheme == "pcast" || scheme == "itpc" || scheme == "http" || scheme == "https") {
                        openPodcastUrl(IntentUtil.getUrl(intent))
                    }
                    // import opml from file
                    else if (intent.data != null) {
                        val uri = intent.data ?: return
                        OpmlImportTask.run(uri, this)
                    }
                }
                // import opml from file
                else if (intent.data != null) {
                    val uri = intent.data ?: return
                    OpmlImportTask.run(uri, this)
                }
            } else if (action == MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) {
                val bundle = intent.extras ?: return
                playbackManager.mediaSessionManager.playFromSearchExternal(bundle)
            } else if (intent.extras?.getBoolean(
                    "extra_accl_intent",
                    false
                ) == true || intent.extras?.getBoolean("handled_by_nga", false) == true
            ) {
                // This is what the assistant sends us when it doesn't know what to do and just opens the app. Assume the user wants to play.
                playbackManager.playQueue()
            }
        } catch (e: Exception) {
            Timber.e(e)
            SentryHelper.recordException(e)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        theme.setupThemeForConfig(this, newConfig)
    }

    override fun openCloudFiles() {
        if (supportFragmentManager.fragments.find { it is CloudFilesFragment } == null) {
            addFragment(CloudFilesFragment())
        }
    }

    override fun openPodcastPage(uuid: String) {
        closePlayer()
        frameBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        val currentFragment = navigator.currentFragment()
        if (currentFragment is PodcastFragment && uuid == currentFragment.podcastUuid) return // We are already showing it
        addFragment(PodcastFragment.newInstance(podcastUuid = uuid))
    }

    override fun openEpisodeDialog(episodeUuid: String?, podcastUuid: String?, forceDark: Boolean) {
        episodeUuid ?: return

        launch(Dispatchers.Main.immediate) {
            val playable =
                withContext(Dispatchers.Default) { episodeManager.findPlayableByUuid(episodeUuid) }
            val fragment = if (playable == null) {
                val podcastUuidFound = podcastUuid ?: return@launch
                // Assume it's an episode we don't know about
                EpisodeFragment.newInstance(
                    episodeUuid,
                    podcastUuid = podcastUuidFound,
                    forceDark = forceDark
                )
            } else if (playable is Episode) {
                EpisodeFragment.newInstance(
                    episodeUuid,
                    podcastUuid = podcastUuid,
                    forceDark = forceDark
                )
            } else {
                CloudFileBottomSheetFragment.newInstance(playable.uuid, forceDark = true)
            }

            fragment.showAllowingStateLoss(supportFragmentManager, "episode_card")
        }
    }

    @Suppress("DEPRECATION")
    private fun openPodcastUrl(url: String?) {
        url ?: return

        val dialog = android.app.ProgressDialog.show(this, getString(LR.string.loading), getString(LR.string.please_wait), true)
        serverManager.searchForPodcasts(
            url,
            object : ServerCallback<PodcastSearch> {
                override fun dataReturned(result: PodcastSearch?) {
                    UiUtil.hideProgressDialog(dialog)
                    val uuid = result?.searchResults?.firstOrNull()?.uuid ?: return
                    openPodcastPage(uuid)
                }

                override fun onFailed(
                    errorCode: Int,
                    userMessage: String?,
                    serverMessageId: String?,
                    serverMessage: String?,
                    throwable: Throwable?
                ) {
                    UiUtil.hideProgressDialog(dialog)

                    val message = LocaliseHelper.serverMessageIdToMessage(serverMessageId, ::getString)
                        ?: userMessage
                        ?: getString(LR.string.podcast_add_failed)
                    UiUtil.displayAlertError(
                        context = this@MainActivity,
                        message = message,
                        null
                    )
                }
            }
        )
    }

    @Suppress("DEPRECATION")
    private fun openSharingUrl(intent: Intent) {
        val url = intent.data?.path ?: return
        val dialog = android.app.ProgressDialog.show(this, getString(LR.string.loading), getString(LR.string.please_wait), true)
        serverManager.getSharedItemDetails(
            url,
            object : ServerCallback<au.com.shiftyjelly.pocketcasts.models.to.Share> {
                override fun dataReturned(result: au.com.shiftyjelly.pocketcasts.models.to.Share?) {
                    UiUtil.hideProgressDialog(dialog)
                    result ?: return

                    val podcastUuid = result.podcast.uuid
                    if (podcastUuid.isBlank()) {
                        UiUtil.displayAlertError(
                            this@MainActivity,
                            getString(LR.string.podcast_share_open_fail_title),
                            getString(LR.string.podcast_share_open_fail),
                            null
                        )
                        return
                    }

                    val episode = result.episode
                    if (episode != null) {
                        openEpisodeDialog(episode.uuid, podcastUuid, forceDark = false)
                    } else {
                        openPodcastPage(podcastUuid)
                    }
                }

                override fun onFailed(
                    errorCode: Int,
                    userMessage: String?,
                    serverMessageId: String?,
                    serverMessage: String?,
                    throwable: Throwable?
                ) {
                    UiUtil.hideProgressDialog(dialog)
                    Timber.e(serverMessage)
                    UiUtil.displayAlertError(
                        this@MainActivity,
                        getString(LR.string.podcast_share_open_fail_title),
                        getString(LR.string.podcast_share_open_fail),
                        null
                    )
                }
            }
        )
    }

    @Suppress("DEPRECATION")
    private fun openPromoCode(intent: Intent) {
        val code = intent.data?.lastPathSegment

        if (code != null) {
            val accountIntent = AccountActivity.promoCodeInstance(this, code)
            startActivityForResult(accountIntent, PROMOCODE_REQUEST_CODE)
        }
    }

    private fun showUpgradedFromPromoCode(description: String) {
        openTab(VR.id.navigation_profile)
        PromoCodeUpgradedFragment.newInstance(description)
            .show(supportFragmentManager, "upgraded_from_promocode")
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SonosAppLinkActivity.SONOS_APP_ACTIVITY_RESULT) {
            setResult(Activity.RESULT_OK, data)
            finish()
        } else if (requestCode == PROMOCODE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val message = data?.getStringExtra(AccountActivity.PROMO_CODE_RETURN_DESCRIPTION)
            showUpgradedFromPromoCode(message ?: "")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getBottomSheetBehavior(): LockableBottomSheetBehavior<View> {
        return (BottomSheetBehavior.from(binding.frameBottomSheet) as LockableBottomSheetBehavior<View>)
    }

    private fun trackTabOpened(tab: Int, isInitial: Boolean = false) {
        val event: AnalyticsEvent? = when (tab) {
            VR.id.navigation_podcasts -> AnalyticsEvent.PODCASTS_TAB_OPENED
            VR.id.navigation_filters -> AnalyticsEvent.FILTERS_TAB_OPENED
            VR.id.navigation_discover -> AnalyticsEvent.DISCOVER_TAB_OPENED
            VR.id.navigation_profile -> AnalyticsEvent.PROFILE_TAB_OPENED
            else -> {
                Timber.e("Can't open invalid tab")
                null
            }
        }
        event?.let { analyticsTracker.track(event, mapOf(INITIAL_KEY to isInitial)) }
    }
}

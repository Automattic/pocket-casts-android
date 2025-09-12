package au.com.shiftyjelly.pocketcasts.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.dynamicanimation.animation.DynamicAnimation.TRANSLATION_Y
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import androidx.transition.Slide
import au.com.shiftyjelly.pocketcasts.R
import au.com.shiftyjelly.pocketcasts.account.AccountActivity
import au.com.shiftyjelly.pocketcasts.account.PromoCodeUpgradedFragment
import au.com.shiftyjelly.pocketcasts.account.onboarding.AccountBenefitsFragment
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingActivity
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingActivityContract
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingActivityContract.OnboardingFinish
import au.com.shiftyjelly.pocketcasts.account.watchsync.WatchSync
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.databinding.ActivityMainBinding
import au.com.shiftyjelly.pocketcasts.deeplink.AddBookmarkDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.AppOpenDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.AssistantDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ChangeBookmarkTitleDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.CloudFilesDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.CreateAccountDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.EXTRA_PAGE
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLinkFactory
import au.com.shiftyjelly.pocketcasts.deeplink.DeleteBookmarkDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.DeveloperOptionsDeeplink
import au.com.shiftyjelly.pocketcasts.deeplink.DownloadsDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ImportDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.NativeShareDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.OpmlImportDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.PlayFromSearchDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.PocketCastsWebsiteGetDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.PromoCodeDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.RecommendationsDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ReferralsDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ShareListDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ShowBookmarkDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ShowDiscoverDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ShowEpisodeDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ShowFiltersDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ShowPlaylistDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ShowPodcastDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ShowPodcastFromUrlDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ShowPodcastsDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ShowUpNextModalDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ShowUpNextTabDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.SignInDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.SmartFoldersDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.SonosDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.StaffPicksDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ThemesDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.TrendingDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.UpgradeAccountDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.UpsellDeepLink
import au.com.shiftyjelly.pocketcasts.discover.util.DiscoverDeepLinkManager
import au.com.shiftyjelly.pocketcasts.discover.util.DiscoverDeepLinkManager.Companion.RECOMMENDATIONS_USER
import au.com.shiftyjelly.pocketcasts.discover.util.DiscoverDeepLinkManager.Companion.STAFF_PICKS_LIST_ID
import au.com.shiftyjelly.pocketcasts.discover.view.DiscoverFragment
import au.com.shiftyjelly.pocketcasts.discover.view.PodcastGridListFragment
import au.com.shiftyjelly.pocketcasts.discover.view.PodcastListFragment
import au.com.shiftyjelly.pocketcasts.endofyear.StoriesActivity
import au.com.shiftyjelly.pocketcasts.endofyear.StoriesActivity.StoriesSource
import au.com.shiftyjelly.pocketcasts.endofyear.ui.EndOfYearLaunchBottomSheet
import au.com.shiftyjelly.pocketcasts.filters.FiltersFragment
import au.com.shiftyjelly.pocketcasts.localization.helper.LocaliseHelper
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.navigation.BottomNavigator
import au.com.shiftyjelly.pocketcasts.navigation.FragmentInfo
import au.com.shiftyjelly.pocketcasts.navigation.NavigatorAction
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.player.view.PlayerBottomSheet
import au.com.shiftyjelly.pocketcasts.player.view.PlayerContainerFragment
import au.com.shiftyjelly.pocketcasts.player.view.UpNextFragment
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarkActivity
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarkActivityContract
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarksContainerFragment
import au.com.shiftyjelly.pocketcasts.player.view.dialog.MiniPlayerDialog
import au.com.shiftyjelly.pocketcasts.player.view.video.VideoActivity
import au.com.shiftyjelly.pocketcasts.playlists.PlaylistFragment
import au.com.shiftyjelly.pocketcasts.playlists.PlaylistsFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.ProfileEpisodeListFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.EpisodeContainerFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.SuggestedFoldersFragment
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
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsGuestPassFragment
import au.com.shiftyjelly.pocketcasts.repositories.bumpstats.BumpStatsTask
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.di.NotificationPermissionChecker
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.opml.OpmlImportTask
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextSource
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SmartPlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsTask
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.search.SearchFragment
import au.com.shiftyjelly.pocketcasts.servers.ServerCallback
import au.com.shiftyjelly.pocketcasts.servers.ServiceManager
import au.com.shiftyjelly.pocketcasts.servers.discover.PodcastSearch
import au.com.shiftyjelly.pocketcasts.servers.model.NetworkLoadableList.Companion.TRENDING
import au.com.shiftyjelly.pocketcasts.settings.AppearanceSettingsFragment
import au.com.shiftyjelly.pocketcasts.settings.ExportSettingsFragment
import au.com.shiftyjelly.pocketcasts.settings.SettingsFragment
import au.com.shiftyjelly.pocketcasts.settings.developer.DeveloperFragment
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.settings.whatsnew.WhatsNewFragment
import au.com.shiftyjelly.pocketcasts.ui.MainActivityViewModel.NavigationState
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.NavigationBarColor
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarIconColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.utils.observeOnce
import au.com.shiftyjelly.pocketcasts.view.LockableBottomSheetBehavior
import au.com.shiftyjelly.pocketcasts.views.activity.WebViewActivity
import au.com.shiftyjelly.pocketcasts.views.extensions.showAllowingStateLoss
import au.com.shiftyjelly.pocketcasts.views.extensions.spring
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.TopScrollable
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import au.com.shiftyjelly.pocketcasts.views.helper.OffsettingBottomSheetCallback
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import au.com.shiftyjelly.pocketcasts.views.helper.WarningsHelper
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import android.provider.Settings as AndroidProviderSettings
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.views.R as VR

private const val SAVEDSTATE_BOTTOM_SHEET_TAG = "bottom_sheet_tag"
private const val EXTRA_LONG_SNACKBAR_DURATION_MS: Int = 5000

@AndroidEntryPoint
class MainActivity :
    AppCompatActivity(),
    FragmentHostListener,
    PlayerBottomSheet.PlayerBottomSheetListener,
    SearchFragment.Listener,
    OnboardingLauncher,
    CoroutineScope,
    NotificationPermissionChecker {

    companion object {
        private const val INITIAL_KEY = "initial"
        private const val SOURCE_KEY = "source"

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }

        const val PROMOCODE_REQUEST_CODE = 2
    }

    @Inject
    lateinit var playbackManager: PlaybackManager

    @Inject
    lateinit var podcastManager: PodcastManager

    @Inject
    lateinit var smartPlaylistManager: SmartPlaylistManager

    @Inject
    lateinit var episodeManager: EpisodeManager

    @Inject
    lateinit var serviceManager: ServiceManager

    @Inject
    lateinit var theme: Theme

    @Inject
    lateinit var settings: Settings

    @Inject
    lateinit var userEpisodeManager: UserEpisodeManager

    @Inject
    lateinit var warningsHelper: WarningsHelper

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    @Inject
    lateinit var episodeAnalytics: EpisodeAnalytics

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var watchSync: WatchSync

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var discoverDeepLinkManager: DiscoverDeepLinkManager

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var crashLogging: CrashLogging

    @Inject
    lateinit var paymentClient: PaymentClient

    private val viewModel: MainActivityViewModel by viewModels()
    private val disposables = CompositeDisposable()
    private var videoPlayerShown: Boolean = false
    private var overrideNextRefreshTimer: Boolean = false

    private var mediaRouter: MediaRouter? = null
    private val mediaRouterCallback = object : MediaRouter.Callback() {}
    private val mediaRouteSelector = MediaRouteSelector.Builder()
        .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
        .build()

    private val childrenWithBackStack: List<HasBackstack>
        get() = supportFragmentManager.fragments.filterIsInstance<HasBackstack>()

    @Suppress("UNCHECKED_CAST")
    private val frameBottomSheetBehavior: LockableBottomSheetBehavior<View>
        get() = BottomSheetBehavior.from(binding.frameBottomSheet) as LockableBottomSheetBehavior<View>

    private val miniPlayerHeight: Int
        get() = resources.getDimension(R.dimen.miniPlayerHeight).toInt()

    private val bottomNavigationHeight: Int
        get() = binding.bottomNavigation.height - binding.bottomNavigation.paddingBottom

    private var bottomSheetTag: String? = null
    private val bottomSheetQueue: MutableList<(() -> Unit)?> = mutableListOf()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private lateinit var binding: ActivityMainBinding
    private lateinit var navigator: BottomNavigator

    private val onboardingLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(OnboardingActivityContract()) { result ->
        when (result) {
            is OnboardingFinish.Done -> {
                if (!settings.hasCompletedOnboarding()) {
                    val podcastCount = runBlocking(Dispatchers.Default) { podcastManager.countSubscribed() }
                    val landingTab = if (podcastCount == 0) VR.id.navigation_discover else VR.id.navigation_podcasts
                    openTab(landingTab)
                }
                settings.setHasDoneInitialOnboarding()
            }

            is OnboardingFinish.DoneGoToDiscover -> {
                settings.setHasDoneInitialOnboarding()
                openTab(VR.id.navigation_discover)
            }

            is OnboardingFinish.DoneShowPlusPromotion -> {
                settings.setHasDoneInitialOnboarding()
                OnboardingLauncher.openOnboardingFlow(this, OnboardingFlow.Upsell(OnboardingUpgradeSource.LOGIN_PLUS_PROMOTION))
            }

            is OnboardingFinish.DoneShowWelcomeInReferralFlow -> {
                settings.showReferralWelcome.set(true, updateModifiedAt = false)
            }

            is OnboardingFinish.DoneApplySuggestedFolders, null -> {
                Timber.e("Unexpected result $result from onboarding activity")
            }
        }
    }

    private val bookmarkActivityLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        BookmarkActivityContract(),
    ) { result ->
        showViewBookmarksSnackbar(result)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}

    private val deepLinkFactory = DeepLinkFactory()

    @SuppressLint("WrongConstant") // for custom snackbar duration constant
    private fun checkForNotificationPermission(onPermissionGranted: () -> Unit = {}) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                    onPermissionGranted()
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    if (settings.isNotificationsDisabledMessageShown()) return
                    Snackbar.make(
                        snackBarView(),
                        getString(LR.string.notifications_blocked_warning),
                        EXTRA_LONG_SNACKBAR_DURATION_MS,
                    ).setAction(
                        getString(LR.string.notifications_blocked_warning_snackbar_action)
                            .uppercase(Locale.getDefault()),
                    ) {
                        // Responds to click on the action
                        val intent = Intent(AndroidProviderSettings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        val uri: Uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    }.show()
                    settings.setNotificationsDisabledMessageShown(true)
                }

                else -> {
                    notificationPermissionLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS,
                    )
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("Main Activity onCreate")
        // Changing the theme draws the status and navigation bars as black, unless this is manually set
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        theme.setupThemeForConfig(this, resources.configuration)
        enableEdgeToEdge(navigationBarStyle = theme.getNavigationBarStyle(this))
        bottomSheetTag = savedInstanceState?.getString(SAVEDSTATE_BOTTOM_SHEET_TAG)

        playbackManager.setNotificationPermissionChecker(this)

        val showOnboarding = !settings.hasCompletedOnboarding() && !syncManager.isLoggedIn()
        // Only show if savedInstanceState is null in order to avoid creating onboarding activity twice.
        if (showOnboarding && savedInstanceState == null) {
            openOnboardingFlow(OnboardingFlow.InitialOnboarding)
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (!FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_ACCOUNT_CREATION)) {
            checkForNotificationPermission()
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            binding.root.updatePadding(left = insets.left, right = insets.right)
            binding.bottomNavigation.updatePadding(bottom = insets.bottom)
            windowInsets
        }
        binding.bottomNavigation.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            binding.mainFragment.updatePadding(bottom = view.height)

            BottomSheetBehavior.from(binding.playerBottomSheet).apply {
                peekHeight = miniPlayerHeight + view.height
            }
        }

        val menuId = if (FeatureFlag.isEnabled(Feature.PLAYLISTS_REBRANDING, immutable = true)) {
            VR.menu.navigation_playlists
        } else {
            VR.menu.navigation
        }
        binding.bottomNavigation.inflateMenu(menuId)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                val isEligible = viewModel.isEndOfYearStoriesEligible()
                if (isEligible) {
                    if (settings.getEndOfYearShowModal()) {
                        setupEndOfYearLaunchBottomSheet()
                    }
                    if (settings.getEndOfYearShowBadge2023()) {
                        binding.bottomNavigation.getOrCreateBadge(VR.id.navigation_profile)
                    }
                }
            }
        }

        var selectedTab = settings.selectedTab()
        val tabs = buildMap {
            put(VR.id.navigation_podcasts) { FragmentInfo(PodcastsFragment(), true) }
            put(VR.id.navigation_filters) {
                val fragment = if (FeatureFlag.isEnabled(Feature.PLAYLISTS_REBRANDING, immutable = true)) {
                    PlaylistsFragment()
                } else {
                    FiltersFragment()
                }
                FragmentInfo(fragment, true)
            }
            put(VR.id.navigation_discover) { FragmentInfo(DiscoverFragment(), false) }
            put(VR.id.navigation_profile) { FragmentInfo(ProfileFragment(), true) }
            put(VR.id.navigation_upnext) {
                FragmentInfo(
                    UpNextFragment.newInstance(
                        embedded = false,
                        source = UpNextSource.UP_NEXT_TAB,
                    ),
                    true,
                )
            }
        }

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
            activity = this,
        )

        navigator.resetRootFragmentCommand()
            .subscribe({ fragment ->
                val didScroll = (fragment as? TopScrollable)?.scrollToTop()

                // Open search UI when tapping the Discover navigation button
                // while at the top of the Discover page already
                val discoverId = VR.id.navigation_discover
                val currentFragmentIsDiscover = navigator.currentTab() == discoverId
                if (currentFragmentIsDiscover && didScroll == false) {
                    val searchFragment = SearchFragment.newInstance(
                        floating = true,
                        onlySearchRemote = true,
                        source = SourceView.DISCOVER,
                    )
                    addFragment(searchFragment, onTop = true)
                }
            })
            .addTo(disposables)

        setupPlayerViews(
            animateMiniPlayer = savedInstanceState == null,
        )

        setupBottomSheetTranslation()

        if (savedInstanceState == null) {
            trackTabOpened(selectedTab, isInitial = true)
        }
        navigator.infoStream()
            .doOnNext {
                updateSystemColors()
                if (it is NavigatorAction.TabSwitched) {
                    val currentTab = navigator.currentTab()
                    if (settings.selectedTab() != currentTab) {
                        trackTabOpened(currentTab)
                        when (currentTab) {
                            VR.id.navigation_profile -> resetEoYBadgeIfNeeded()
                        }
                    }
                    settings.setSelectedTab(currentTab)
                } else if (it is NavigatorAction.NewFragmentAdded) {
                    if (navigator.currentTab() == VR.id.navigation_profile) {
                        resetEoYBadgeIfNeeded()
                    }
                }
            }
            .subscribe()
            .addTo(disposables)

        handleIntent(intent, savedInstanceState)

        updateSystemColors()

        mediaRouter = MediaRouter.getInstance(this)

        ThemeSettingObserver(this, theme, settings.themeReconfigurationEvents).observeThemeChanges()

        encourageAccountCreation()
    }

    private fun resetEoYBadgeIfNeeded() {
        if (binding.bottomNavigation.getBadge(VR.id.navigation_profile) != null &&
            settings.getEndOfYearShowBadge2023()
        ) {
            binding.bottomNavigation.removeBadge(VR.id.navigation_profile)
            settings.setEndOfYearShowBadge2023(false)
        }
    }

    private fun encourageAccountCreation() {
        if (FeatureFlag.isEnabled(Feature.ENCOURAGE_ACCOUNT_CREATION)) {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    val encourageAccountCreation = settings.showFreeAccountEncouragement.value
                    if (!encourageAccountCreation) {
                        return@repeatOnLifecycle
                    }
                    settings.showFreeAccountEncouragement.set(false, updateModifiedAt = true)

                    val isSignedIn = viewModel.signInState.asFlow().first().isSignedIn
                    if (isSignedIn) {
                        return@repeatOnLifecycle
                    }

                    if (Util.isTablet(this@MainActivity)) {
                        AccountBenefitsFragment().show(supportFragmentManager, "account_benefits_fragment")
                    } else {
                        openOnboardingFlow(OnboardingFlow.AccountEncouragement)
                    }
                }
            }
        }
    }

    override fun launchIntent(onboardingFlow: OnboardingFlow): Intent {
        return OnboardingActivity.newInstance(this, onboardingFlow)
    }

    override fun openOnboardingFlow(onboardingFlow: OnboardingFlow) {
        onboardingLauncher.launch(
            launchIntent(onboardingFlow),
            ActivityOptionsCompat
                .makeCustomAnimation(this, R.anim.onboarding_enter, R.anim.onboarding_disappear),
        )
    }

    override fun onStart() {
        super.onStart()
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (!videoPlayerShown && playbackManager.getCurrentEpisode()?.isVideo == true && playbackManager.isPlaybackLocal() && playbackManager.isPlaying() && viewModel.isPlayerOpen) {
                openFullscreenViewPlayer()
            } else {
                videoPlayerShown = false
            }
        }

        // Tell media router to discover routes
        mediaRouter?.addCallback(mediaRouteSelector, mediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
    }

    override fun onStop() {
        super.onStop()
        // Remove the callback flag CALLBACK_FLAG_REQUEST_DISCOVERY on stop by calling
        // addCallback() again in order to tell the media router that it no longer
        // needs to invest effort trying to discover routes of these kinds for now.
        mediaRouter?.addCallback(mediaRouteSelector, mediaRouterCallback, 0)
    }

    private fun openFullscreenViewPlayer() {
        videoPlayerShown = true
        startActivity(VideoActivity.buildIntent(context = this))
    }

    override fun onResume() {
        super.onResume()

        refreshApp()
        addLineView()
        BumpStatsTask.scheduleToRun(this)
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
        outState.putString(SAVEDSTATE_BOTTOM_SHEET_TAG, bottomSheetTag)
    }

    override fun overrideNextRefreshTimer() {
        overrideNextRefreshTimer = true
    }

    private fun refreshApp() {
        fun doRefresh() {
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
        }

        // If the user chooses the advanced option to only sync on unmetered networks
        // then don't auto-refresh when the app resumes. Still let them swipe down though
        // to refresh if they wish and still schedule the worker to do updates
        if (settings.refreshPodcastsOnResume(Network.isUnmeteredConnection(this@MainActivity))) {
            doRefresh()
        }

        lifecycleScope.launch {
            paymentClient.acknowledgePendingPurchases()
        }

        // Schedule next refresh in the background
        RefreshPodcastsTask.scheduleOrCancel(this@MainActivity, settings)
    }

    @Suppress("DEPRECATION")
    private suspend fun refreshAppAndWait() = withContext(Dispatchers.Main) {
        val dialog = android.app.ProgressDialog.show(this@MainActivity, getString(LR.string.loading), getString(LR.string.please_wait), true)
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Running refresh from refresh and wait")
        RefreshPodcastsTask.runNowSync(application, applicationScope)

        UiUtil.hideProgressDialog(dialog)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
        mediaRouter?.removeCallback(mediaRouterCallback)
    }

    @SuppressLint("GestureBackNavigation")
    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (isUpNextShowing()) {
            val fragment = supportFragmentManager.findFragmentByTag(UpNextFragment::class.java.name)
            if ((fragment as UpNextFragment).multiSelectHelper.isMultiSelecting) {
                fragment.multiSelectHelper.isMultiSelecting = false
                return
            }
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
        val topFragment = supportFragmentManager.fragments.lastOrNull()
        val color = if (binding.playerBottomSheet.isPlayerOpen) {
            StatusBarIconColor.Light
        } else if (topFragment is BaseFragment) {
            topFragment.statusBarIconColor
        } else {
            null
        }

        if (color != null) {
            theme.updateWindowStatusBarIcons(window = window, statusBarIconColor = color)
        }
    }

    private fun updateNavAndStatusColors(playerOpen: Boolean, playingPodcast: Podcast?) {
        val navigationBarColor = if (playerOpen) {
            NavigationBarColor.Player(playingPodcast)
        } else {
            NavigationBarColor.Theme
        }

        theme.updateWindowNavigationBarColor(window = window, navigationBarColor = navigationBarColor)

        // Color the side bars of the screen if there is inset for areas such as cameras
        binding.root.setBackgroundColor(theme.getNavigationBarColor(navigationBarColor))

        updateStatusBar()
    }

    override fun onUpNextClicked() {
        showUpNextFragment(UpNextSource.MINI_PLAYER)
    }

    override fun onMiniPlayerLongClick() {
        MiniPlayerDialog(
            playbackManager = playbackManager,
            podcastManager = podcastManager,
            episodeManager = episodeManager,
            fragmentManager = supportFragmentManager,
            analyticsTracker = analyticsTracker,
            episodeAnalytics = episodeAnalytics,
            settings = settings,
        ).show(this)
    }

    private fun showUpNextFragment(source: UpNextSource) {
        analyticsTracker.track(AnalyticsEvent.UP_NEXT_SHOWN, mapOf(SOURCE_KEY to source.analyticsValue))
        showBottomSheet(UpNextFragment.newInstance(source = source))
    }

    private fun setupEndOfYearLaunchBottomSheet() {
        val viewGroup = binding.modalBottomSheet
        viewGroup.removeAllViews()
        viewGroup.addView(
            ComposeView(viewGroup.context).apply {
                setContent {
                    val shouldShow by viewModel.shouldShowStoriesModal.collectAsState()
                    AppTheme(theme.activeTheme) {
                        EndOfYearLaunchBottomSheet(
                            parent = viewGroup,
                            shouldShow = shouldShow,
                            onClick = {
                                showStoriesOrAccount(StoriesSource.MODAL.value)
                            },
                            onExpand = {
                                analyticsTracker.track(
                                    AnalyticsEvent.END_OF_YEAR_MODAL_SHOWN,
                                    mapOf("year" to EndOfYearManager.YEAR_TO_SYNC.value),
                                )
                                settings.setEndOfYearShowModal(false)
                                viewModel.updateStoriesModalShowState(false)
                            },
                        )
                    }
                }
            },
        )
    }

    private fun showEndOfYearModal() {
        viewModel.updateStoriesModalShowState(true)
        launch(Dispatchers.Main) {
            if (viewModel.isEndOfYearStoriesEligible()) setupEndOfYearLaunchBottomSheet()
        }
    }

    override fun showStoriesOrAccount(source: String) {
        if (viewModel.isSignedIn) {
            showStories(StoriesSource.fromString(source))
        } else {
            viewModel.waitingForSignInToShowStories = true
            openOnboardingFlow(OnboardingFlow.LoggedOut)
        }
    }

    private fun showStories(source: StoriesSource) {
        StoriesActivity.open(this, source)
    }

    @Suppress("DEPRECATION")
    private fun setupPlayerViews(animateMiniPlayer: Boolean) {
        binding.playerBottomSheet.listener = this
        binding.playerBottomSheet.initializeBottomSheetBehavior()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playbackState.collect { state ->
                    val lastPlaybackState = viewModel.lastPlaybackState
                    val isEpisodeChanged = lastPlaybackState?.episodeUuid != state.episodeUuid
                    val isPlaybackChanged = lastPlaybackState?.isPlaying == false && state.isPlaying

                    if (isEpisodeChanged || isPlaybackChanged) {
                        val episode = withContext(Dispatchers.Default) {
                            episodeManager.findEpisodeByUuid(state.episodeUuid)
                        }
                        if (episode?.isVideo == true && state.isPlaying) {
                            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                                binding.playerBottomSheet.openPlayer()
                            } else {
                                openFullscreenViewPlayer()
                            }
                        }
                    }

                    if (viewModel.isPlayerOpen && isEpisodeChanged) {
                        updateNavAndStatusColors(playerOpen = true, playingPodcast = state.podcast)
                    }

                    if (lastPlaybackState != null && (isEpisodeChanged || isPlaybackChanged) && settings.openPlayerAutomatically.value) {
                        binding.playerBottomSheet.openPlayer()
                    }

                    updatePlaybackState(state)

                    viewModel.lastPlaybackState = state
                }
            }
        }

        val upNextQueueChanges = playbackManager.upNextQueue.getChangesFlowWithLiveCurrentEpisode(episodeManager, podcastManager)
        val artworkConfigurationChanges = settings.artworkConfiguration.flow

        val combinedFlow = combine(upNextQueueChanges, artworkConfigurationChanges) { upNextQueue, artworkConfiguration ->
            upNextQueue to artworkConfiguration
        }
            .onEach { (upNextQueue, artworkConfiguration) ->
                binding.playerBottomSheet.setUpNext(
                    upNext = upNextQueue,
                    theme = theme,
                    shouldAnimateOnAttach = animateMiniPlayer,
                    useEpisodeArtwork = artworkConfiguration.useEpisodeArtwork,
                )
            }
            .catch { Timber.e(it) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combinedFlow.collect()
            }
        }

        viewModel.signInState.observe(this) { signinState ->
            val subscription = (signinState as? SignInState.SignedIn)?.subscription

            if (signinState.isSignedIn) {
                if (viewModel.waitingForSignInToShowStories) {
                    showStories(StoriesSource.USER_LOGIN)
                    viewModel.waitingForSignInToShowStories = false
                } else if (settings.getEndOfYearShowModal()) {
                    if (isWhatsNewShowing()) return@observe
                    showEndOfYearModal()
                }
            }

            if (subscription != null) {
                if (viewModel.shouldShowCancelled(subscription)) {
                    val cancelledFragment = SubCancelledFragment.newInstance()
                    showBottomSheet(cancelledFragment)
                }
            } else {
                applicationScope.launch { userEpisodeManager.removeCloudStatusFromFiles(playbackManager) }
            }

            if (viewModel.shouldShowTrialFinished(signinState)) {
                val trialFinished = TrialFinishedFragment()
                showBottomSheet(trialFinished)

                settings.setTrialFinishedSeen(true)
            }

            lifecycleScope.launch { watchSync.sendAuthToDataLayer() }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.state.collect { state ->
                    if (state.shouldShowWhatsNew) {
                        showBottomSheet(
                            fragment = WhatsNewFragment(),
                        )
                        viewModel.onWhatsNewShown()
                    }
                }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.snackbarMessage.collect { messageResId ->
                    Snackbar.make(snackBarView(), getString(messageResId), Snackbar.LENGTH_LONG)
                        .show()
                }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationState.collect { navigationState ->
                    when (navigationState) {
                        is NavigationState.BookmarksForCurrentlyPlaying -> showPlayerBookmarks()
                        is NavigationState.BookmarksForPodcastEpisode -> {
                            // Once episode container fragment is shown, bookmarks tab is shown from inside it based on the new source
                            openEpisodeDialog(
                                episodeUuid = navigationState.episode.uuid,
                                source = EpisodeViewSource.NOTIFICATION_BOOKMARK,
                                podcastUuid = navigationState.episode.podcastUuid,
                                forceDark = false,
                                autoPlay = false,
                            )
                        }

                        is NavigationState.BookmarksForUserEpisode -> {
                            // Bookmarks container is directly shown for user episode
                            val fragment = BookmarksContainerFragment.newInstance(navigationState.episode.uuid, SourceView.NOTIFICATION_BOOKMARK)
                            fragment.show(supportFragmentManager, "bookmarks_container")
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.snackbarMessage.collect { messageResId ->
                    Snackbar.make(snackBarView(), getString(messageResId), Snackbar.LENGTH_LONG)
                        .show()
                }
            }
        }

        frameBottomSheetBehavior.setBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                settings.updatePlayerOrUpNextBottomSheetState(newState)
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    if (bottomSheetTag == UpNextFragment::class.java.name) {
                        analyticsTracker.track(AnalyticsEvent.UP_NEXT_DISMISSED)
                    }
                    supportFragmentManager.findFragmentByTag(bottomSheetTag)?.let {
                        removeBottomSheetFragment(it)
                    }

                    binding.playerBottomSheet.isDragEnabled = true
                    frameBottomSheetBehavior.swipeEnabled = false

                    updateNavAndStatusColors(playerOpen = viewModel.isPlayerOpen, playingPodcast = viewModel.lastPlaybackState?.podcast)
                } else {
                    binding.playerBottomSheet.isDragEnabled = false
                }
            }
        })
    }

    private fun setupBottomSheetTranslation() {
        frameBottomSheetBehavior.addBottomSheetCallback(OffsettingBottomSheetCallback(binding.frameBottomSheet))
    }

    override fun whatsNewDismissed(fromConfirmAction: Boolean) {
        if (fromConfirmAction) return
        if (settings.getEndOfYearShowModal()) showEndOfYearModal()
    }

    override fun getPlayerBottomSheetState(): Int {
        return binding.playerBottomSheet.sheetBehavior?.state ?: BottomSheetBehavior.STATE_COLLAPSED
    }

    override fun addPlayerBottomSheetCallback(callback: BottomSheetBehavior.BottomSheetCallback) {
        binding.playerBottomSheet.sheetBehavior?.addBottomSheetCallback(callback)
    }

    override fun removePlayerBottomSheetCallback(callback: BottomSheetBehavior.BottomSheetCallback) {
        binding.playerBottomSheet.sheetBehavior?.removeBottomSheetCallback(callback)
    }

    override fun lockPlayerBottomSheet(locked: Boolean) {
        binding.playerBottomSheet.isDragEnabled = !locked
    }

    private fun updatePlaybackState(state: PlaybackState) {
        binding.playerBottomSheet.setPlaybackState(state)

        if ((state.isPlaying || state.isBuffering) && settings.keepScreenAwake.value) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun snackBarView(): View {
        val playerView = supportFragmentManager
            .takeIf { viewModel.isPlayerOpen }
            ?.fragments
            ?.firstNotNullOfOrNull { it as? PlayerContainerFragment }
            ?.view
        return playerView ?: binding.snackbarFragment
    }

    override fun setFullScreenDarkOverlayViewVisibility(visible: Boolean) {
        binding.fullScreenDarkOverlayView.isVisible = visible
    }

    override fun onMiniPlayerHidden() {
        updateSnackbarPosition(miniPlayerOpen = false)
        settings.updateBottomInset(0)
    }

    private fun updateSnackbarPosition(miniPlayerOpen: Boolean) {
        binding.snackbarFragment.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = (if (miniPlayerOpen) miniPlayerHeight else 0) + bottomNavigationHeight
        }
    }

    override fun onMiniPlayerVisible() {
        updateSnackbarPosition(miniPlayerOpen = true)
        settings.updateBottomInset(miniPlayerHeight)

        // Handle up next shortcut
        if (intent.getStringExtra(EXTRA_PAGE) == ShowUpNextModalDeepLink.pageId) {
            intent.removeExtra(EXTRA_PAGE)
            binding.playerBottomSheet.openPlayer()
            showUpNextFragment(UpNextSource.UP_NEXT_SHORTCUT)
        }
    }

    override fun onPlayerBottomSheetSlide(bottomSheetView: View, slideOffset: Float) {
        val view = binding.bottomNavigation
        view.doOnLayout {
            val targetPosition = 2 * view.height * slideOffset
            view.spring(TRANSLATION_Y).animateToFinalPosition(targetPosition)
        }
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

        updateNavAndStatusColors(playerOpen = true, playingPodcast = viewModel.lastPlaybackState?.podcast)
        UiUtil.hideKeyboard(binding.root)

        viewModel.isPlayerOpen = true

        val playerContainerFragment = supportFragmentManager.fragments
            .find { it is PlayerContainerFragment } as? PlayerContainerFragment
        playerContainerFragment?.onPlayerOpen()
    }

    override fun onPlayerClosed() {
        updateNavAndStatusColors(playerOpen = false, playingPodcast = null)

        viewModel.isPlayerOpen = false
        viewModel.closeMultiSelect()

        val playerContainerFragment = supportFragmentManager.fragments
            .find { it is PlayerContainerFragment } as? PlayerContainerFragment
        playerContainerFragment?.onPlayerClose()
    }

    override fun openTab(tabId: Int) {
        navigator.switchTab(tabId)
    }

    override fun showBottomSheet(fragment: Fragment) {
        supportFragmentManager.commitNow {
            bottomSheetTag = fragment::class.java.name
            replace(R.id.frameBottomSheet, fragment, bottomSheetTag)
        }

        frameBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        frameBottomSheetBehavior.swipeEnabled = true
        binding.frameBottomSheet.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    override fun bottomSheetClosePressed(fragment: Fragment) {
        frameBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        binding.frameBottomSheet.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun isUpNextShowing() = bottomSheetTag == UpNextFragment::class.java.name

    private fun isWhatsNewShowing() = bottomSheetTag == WhatsNewFragment::class.java.name

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
        if (playbackManager.shouldWarnAboutPlayback()) {
            launch {
                // show the stream warning if the episode isn't downloaded
                playbackManager.getCurrentEpisode()?.let { episode ->
                    launch(Dispatchers.Main) {
                        if (episode.isDownloaded) {
                            playbackManager.playQueue(SourceView.MINIPLAYER)
                            warningsHelper.showBatteryWarningSnackbarIfAppropriate()
                        } else {
                            warningsHelper.streamingWarningDialog(episode = episode, sourceView = SourceView.MINIPLAYER)
                                .show(supportFragmentManager, "streaming dialog")
                        }
                    }
                }
            }
        } else {
            playbackManager.playQueue(SourceView.MINIPLAYER)
            warningsHelper.showBatteryWarningSnackbarIfAppropriate()
        }
    }

    override fun onPauseClicked() {
        playbackManager.pause(sourceView = SourceView.MINIPLAYER)
    }

    override fun onSkipBackwardClicked() {
        playbackManager.skipBackward(sourceView = SourceView.MINIPLAYER)
    }

    override fun onSkipForwardClicked() {
        playbackManager.skipForward(sourceView = SourceView.MINIPLAYER)
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

    @Suppress("DEPRECATION")
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onSearchEpisodeClick(
        episodeUuid: String,
        podcastUuid: String,
        source: EpisodeViewSource,
    ) {
        openEpisodeDialog(
            episodeUuid = episodeUuid,
            source = source,
            podcastUuid = podcastUuid,
            forceDark = false,
            autoPlay = false,
        )
    }

    override fun onSearchPodcastClick(podcastUuid: String, source: SourceView) {
        val fragment = PodcastFragment.newInstance(podcastUuid, source)
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
            val intent = if (value.isSignedInAsFree) {
                AccountActivity.newUpgradeInstance(this)
            } else if (autoSelectPlus) {
                AccountActivity.newAutoSelectPlusInstance(
                    this,
                )
            } else {
                Intent(this, AccountActivity::class.java)
            }
            startActivity(intent)

            if (shouldClose) {
                finish()
            }
        }

        viewModel.signInState.observeOnce(this, observer)
    }

    override fun onNewIntent(intent: Intent) {
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
            val safeUri = intent.data?.buildUpon()?.clearQuery()?.build() // Remove query parameters from logging
            LogBuffer.i("DeepLink", "Opening deep link: $intent. Safe URI: $safeUri")
            when (val deepLink = deepLinkFactory.create(intent)) {
                is AppOpenDeepLink -> {
                    closeToRoot()
                }

                is DownloadsDeepLink -> {
                    closePlayer()
                    closeToRoot()
                    addFragment(ProfileEpisodeListFragment.newInstance(ProfileEpisodeListFragment.Mode.Downloaded))
                }

                is AddBookmarkDeepLink -> {
                    launch {
                        val bookmarkArguments = viewModel.createBookmarkArguments(bookmarkUuid = null)
                        if (bookmarkArguments != null) {
                            bookmarkActivityLauncher.launch(BookmarkActivity.launchIntent(this@MainActivity, bookmarkArguments))
                        }
                    }
                }

                is ChangeBookmarkTitleDeepLink -> {
                    launch {
                        val bookmarkArguments = viewModel.createBookmarkArguments(deepLink.bookmarkUuid)
                        if (bookmarkArguments != null) {
                            bookmarkActivityLauncher.launch(BookmarkActivity.launchIntent(this@MainActivity, bookmarkArguments))
                        }
                        notificationHelper.removeNotification(intent.extras, Settings.NotificationId.BOOKMARK.value)
                    }
                }

                is ShowBookmarkDeepLink -> {
                    viewModel.viewBookmark(deepLink.bookmarkUuid)
                }

                is DeleteBookmarkDeepLink -> {
                    viewModel.deleteBookmark(deepLink.bookmarkUuid)
                    notificationHelper.removeNotification(intent.extras, Settings.NotificationId.BOOKMARK.value)
                }

                is ShowPodcastDeepLink -> {
                    closePlayer()
                    openPodcastPage(deepLink.podcastUuid, deepLink.sourceView)
                }

                is ShowEpisodeDeepLink -> {
                    openEpisodeDialog(
                        episodeUuid = deepLink.episodeUuid,
                        podcastUuid = deepLink.podcastUuid,
                        source = EpisodeViewSource.fromString(deepLink.sourceView),
                        forceDark = false,
                        autoPlay = deepLink.autoPlay,
                        startTimestamp = deepLink.startTimestamp,
                        endTimestamp = deepLink.endTimestamp,
                    )
                }

                is ShowPodcastsDeepLink -> {
                    closePlayer()
                    openTab(VR.id.navigation_podcasts)
                }

                is ShowDiscoverDeepLink -> {
                    closePlayer()
                    openTab(VR.id.navigation_discover)
                }

                is ShowUpNextModalDeepLink -> {
                    // Do nothig, handled in onMiniPlayerVisible()
                }

                is ShowUpNextTabDeepLink -> {
                    closePlayer()
                    openTab(VR.id.navigation_upnext)
                }

                is ShowPlaylistDeepLink -> {
                    if (FeatureFlag.isEnabled(Feature.PLAYLISTS_REBRANDING, immutable = true)) {
                        val type = Playlist.Type.fromValue(deepLink.playlistType) ?: return
                        closePlayer()
                        openTab(VR.id.navigation_filters)
                        addFragment(PlaylistFragment.newInstance(deepLink.playlistUuid, type))
                    } else {
                        launch(Dispatchers.Default) {
                            smartPlaylistManager.findByUuid(deepLink.playlistUuid)?.let {
                                withContext(Dispatchers.Main) {
                                    settings.setSelectedFilter(it.uuid)
                                    // HACK: Go diving to find if a filter fragment
                                    closePlayer()
                                    openTab(VR.id.navigation_filters)
                                    val filtersFragment = supportFragmentManager.fragments.find { it is FiltersFragment } as? FiltersFragment
                                    filtersFragment?.openPlaylist(it)
                                }
                            }
                        }
                    }
                }

                is CreateAccountDeepLink -> {
                    openOnboardingFlow(OnboardingFlow.LoggedOut)
                }

                is ShowFiltersDeepLink -> {
                    closePlayer()
                    openTab(VR.id.navigation_filters)
                }

                is PocketCastsWebsiteGetDeepLink -> {
                    // Do nothing when the user goes to https://pocketcasts.com/get it should either open the play store or the user's app
                }

                is ReferralsDeepLink -> {
                    openReferralClaim(deepLink.code)
                }

                is ShowPodcastFromUrlDeepLink -> {
                    openPodcastUrl(deepLink.url)
                }

                is SonosDeepLink -> {
                    startActivityForResult(
                        SonosAppLinkActivity.buildIntent(deepLink.state, this),
                        SonosAppLinkActivity.SONOS_APP_ACTIVITY_RESULT,
                    )
                }

                is ShareListDeepLink -> {
                    addFragment(ShareListIncomingFragment.newInstance(deepLink.path, SourceView.fromString(deepLink.sourceView)))
                }

                is CloudFilesDeepLink -> {
                    openCloudFiles()
                }

                is UpsellDeepLink -> {
                    closePlayer()
                    openOnboardingFlow(OnboardingFlow.Upsell(OnboardingUpgradeSource.DEEP_LINK))
                }

                is SmartFoldersDeepLink -> {
                    if (supportFragmentManager.findFragmentByTag("suggested_folders") == null) {
                        closePlayer()
                        SuggestedFoldersFragment.newInstance(SuggestedFoldersFragment.Source.DEEPLINK).show(supportFragmentManager, "suggested_folders")
                    }
                    openTab(VR.id.navigation_podcasts)
                }

                is UpgradeAccountDeepLink -> {
                    showAccountUpgradeNowDialog(shouldClose = true)
                }

                is PromoCodeDeepLink -> {
                    openPromoCode(deepLink.code)
                }

                is NativeShareDeepLink -> {
                    openSharingUrl(deepLink)
                }

                is OpmlImportDeepLink -> {
                    closePlayer()
                    OpmlImportTask.run(deepLink.uri, this)
                }

                is ImportDeepLink -> {
                    openImport()
                }

                is StaffPicksDeepLink -> {
                    val podcastListFragment = supportFragmentManager.fragments.find { it is PodcastGridListFragment } as? PodcastGridListFragment
                    if (podcastListFragment?.listUuid != STAFF_PICKS_LIST_ID) {
                        openDiscoverListDeeplink(STAFF_PICKS_LIST_ID)
                    }
                }

                is TrendingDeepLink -> {
                    val podcastListFragment = supportFragmentManager.fragments.find { it is PodcastGridListFragment } as? PodcastGridListFragment
                    if (podcastListFragment?.inferredId != TRENDING) {
                        openDiscoverListDeeplink(TRENDING)
                    }
                }

                is RecommendationsDeepLink -> {
                    val podcastListFragment = supportFragmentManager.fragments.find { it is PodcastGridListFragment } as? PodcastGridListFragment
                    if (podcastListFragment?.inferredId != RECOMMENDATIONS_USER) {
                        openDiscoverListDeeplink(RECOMMENDATIONS_USER)
                    }
                }

                is PlayFromSearchDeepLink -> {
                    playbackManager.mediaSessionManager.playFromSearchExternal(deepLink.query)
                }

                is AssistantDeepLink -> {
                    // This is what the assistant sends us when it doesn't know what to do and just opens the app. Assume the user wants to play.
                    playbackManager.playQueue()
                }

                is SignInDeepLink -> {
                    val onboardingFlow = when (SourceView.fromString(deepLink.sourceView)) {
                        SourceView.ENGAGE_SDK_SIGN_IN -> OnboardingFlow.EngageSdk
                        else -> OnboardingFlow.LoggedOut
                    }
                    openOnboardingFlow(onboardingFlow)
                }

                is ThemesDeepLink -> {
                    closePlayer()
                    addFragment(AppearanceSettingsFragment.newInstance())
                }

                is DeveloperOptionsDeeplink -> {
                    closePlayer()
                    addFragment(DeveloperFragment())
                }

                null -> {
                    LogBuffer.i("DeepLink", "Did not find any matching deep link for: $intent")
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
            crashLogging.sendReport(e)
        }
    }

    private fun openDiscoverListDeeplink(listId: String) {
        closePlayer()
        openTab(VR.id.navigation_discover)
        lifecycleScope.launch {
            val discoverList = discoverDeepLinkManager.getDiscoverList(listId, resources) ?: return@launch
            val fragment = PodcastListFragment.newInstance(discoverList)
            addFragment(fragment)
        }
    }

    private fun openReferralClaim(code: String) {
        settings.referralClaimCode.set(code, false)
        openTab(VR.id.navigation_profile)
        val fragment = ReferralsGuestPassFragment.newInstance(ReferralsGuestPassFragment.ReferralsPageType.Claim)
        showBottomSheet(fragment)
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

    override fun openPodcastPage(uuid: String, sourceView: String?) {
        closePlayer()
        frameBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        val currentFragment = navigator.currentFragment()
        if (currentFragment is PodcastFragment && uuid == currentFragment.podcastUuid) return // We are already showing it
        addFragment(PodcastFragment.newInstance(podcastUuid = uuid, sourceView = SourceView.fromString(sourceView)))
    }

    @Suppress("DEPRECATION")
    override fun openEpisodeDialog(
        episodeUuid: String?,
        source: EpisodeViewSource,
        podcastUuid: String?,
        forceDark: Boolean,
        autoPlay: Boolean,
        startTimestamp: Duration?,
        endTimestamp: Duration?,
    ) {
        episodeUuid ?: return

        // If a clip has both start and end we don't open it in the app.
        // We do not have a capability of playing a section of an episode between some timestamps.
        if (startTimestamp != null && endTimestamp != null) {
            val url = "${Settings.SERVER_SHORT_URL}/episode/$episodeUuid?t=${startTimestamp.inWholeSeconds},${endTimestamp.inWholeSeconds}"
            WebViewActivity.show(this, getString(LR.string.clip_title), url)
            return
        }

        launch(Dispatchers.Main.immediate) {
            val fragment = when (val localEpisode = withContext(Dispatchers.Default) { episodeManager.findEpisodeByUuid(episodeUuid) }) {
                is UserEpisode -> {
                    CloudFileBottomSheetFragment.newInstance(localEpisode.uuid, forceDark = true, source)
                }

                is PodcastEpisode -> {
                    EpisodeContainerFragment.newInstance(
                        episodeUuid = localEpisode.uuid,
                        source = source,
                        podcastUuid = localEpisode.podcastUuid,
                        forceDark = forceDark,
                        timestamp = startTimestamp,
                        autoPlay = autoPlay,
                    )
                }

                null -> {
                    val dialog = android.app.ProgressDialog.show(this@MainActivity, getString(LR.string.loading), getString(LR.string.please_wait), true)
                    val searchResult = serviceManager.getSharedItemDetailsSuspend("/social/share/show/$episodeUuid")
                    dialog.hide()
                    searchResult?.episode?.let {
                        EpisodeContainerFragment.newInstance(
                            episodeUuid = it.uuid,
                            source = source,
                            podcastUuid = it.podcastUuid,
                            forceDark = forceDark,
                            timestamp = startTimestamp,
                            autoPlay = autoPlay,
                        )
                    }
                }
            }
            fragment?.showAllowingStateLoss(supportFragmentManager, "episode_card")
        }
    }

    @Suppress("DEPRECATION")
    private fun openPodcastUrl(url: String?) {
        url ?: return

        val dialog = android.app.ProgressDialog.show(this, getString(LR.string.loading), getString(LR.string.please_wait), true)
        serviceManager.searchForPodcasts(
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
                    throwable: Throwable?,
                ) {
                    UiUtil.hideProgressDialog(dialog)

                    val message = LocaliseHelper.serverMessageIdToMessage(serverMessageId, ::getString)
                        ?: userMessage
                        ?: getString(LR.string.podcast_add_failed)
                    UiUtil.displayAlertError(
                        context = this@MainActivity,
                        message = message,
                        null,
                    )
                }
            },
        )
    }

    @Suppress("DEPRECATION")
    private fun openSharingUrl(deepLink: NativeShareDeepLink) {
        // If a clip has both start and end we don't open it in the app.
        // We do not have a capability of playing a section of an episode between some timestamps.
        if (deepLink.startTimestamp != null && deepLink.endTimestamp != null) {
            WebViewActivity.show(this, getString(LR.string.clip_title), deepLink.uri.toString())
            return
        }

        val dialog = android.app.ProgressDialog.show(this, getString(LR.string.loading), getString(LR.string.please_wait), true)
        serviceManager.getSharedItemDetails(
            deepLink.sharePath,
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
                            null,
                        )
                        return
                    }

                    val episode = result.episode
                    if (episode != null) {
                        openEpisodeDialog(
                            episodeUuid = episode.uuid,
                            source = EpisodeViewSource.SHARE,
                            podcastUuid = podcastUuid,
                            forceDark = false,
                            autoPlay = false,
                            startTimestamp = deepLink.startTimestamp,
                        )
                    } else {
                        openPodcastPage(podcastUuid)
                    }
                }

                override fun onFailed(
                    errorCode: Int,
                    userMessage: String?,
                    serverMessageId: String?,
                    serverMessage: String?,
                    throwable: Throwable?,
                ) {
                    UiUtil.hideProgressDialog(dialog)
                    Timber.e(serverMessage)
                    UiUtil.displayAlertError(
                        this@MainActivity,
                        getString(LR.string.podcast_share_open_fail_title),
                        getString(LR.string.podcast_share_open_fail),
                        null,
                    )
                }
            },
        )
    }

    @Suppress("DEPRECATION")
    private fun openPromoCode(code: String) {
        val accountIntent = AccountActivity.promoCodeInstance(this, code)
        startActivityForResult(accountIntent, PROMOCODE_REQUEST_CODE)
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

    private fun trackTabOpened(tab: Int, isInitial: Boolean = false) {
        val event: AnalyticsEvent? = when (tab) {
            VR.id.navigation_podcasts -> AnalyticsEvent.PODCASTS_TAB_OPENED
            VR.id.navigation_upnext -> AnalyticsEvent.UP_NEXT_TAB_OPENED
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

    override fun checkNotificationPermission(onPermissionGranted: () -> Unit) {
        checkForNotificationPermission(onPermissionGranted)
    }

    private fun showPlayerBookmarks() {
        openPlayer()
        launch {
            delay(1000) // To let the player open and load tabs
            withContext(Dispatchers.Main) {
                val playerContainerFragment =
                    supportFragmentManager.fragments.find { it is PlayerContainerFragment } as? PlayerContainerFragment
                playerContainerFragment?.openBookmarks()
            }
        }
    }

    private fun showViewBookmarksSnackbar(
        result: BookmarkActivityContract.BookmarkResult?,
    ) {
        val view = snackBarView()
        if (result == null) return

        val snackbarMessage = if (result.isExistingBookmark) {
            getString(LR.string.bookmark_updated, result.title)
        } else {
            getString(LR.string.bookmark_added, result.title)
        }

        val action = View.OnClickListener {
            showPlayerBookmarks()
        }

        Snackbar.make(view, snackbarMessage, Snackbar.LENGTH_LONG)
            .setAction(LR.string.settings_view, action)
            .setActionTextColor(result.tintColor)
            .setBackgroundTint(ThemeColor.primaryUi01(Theme.ThemeType.DARK))
            .setTextColor(ThemeColor.primaryText01(Theme.ThemeType.DARK))
            .show()
    }

    private fun openImport() {
        closePlayer()
        openTab(VR.id.navigation_profile)
        addFragment(SettingsFragment())
        addFragment(ExportSettingsFragment())
    }
}

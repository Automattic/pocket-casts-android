package au.com.shiftyjelly.pocketcasts.utils.featureflag

import au.com.shiftyjelly.pocketcasts.helper.BuildConfig

enum class Feature(
    val key: String,
    val title: String,
    val defaultValue: Boolean,
    val tier: FeatureTier,
    val hasFirebaseRemoteFlag: Boolean,
    val hasDevToggle: Boolean,
) {
    SYNC_EOY_DATA_ON_STARTUP(
        key = "sync_eoy_data_on_startup",
        title = "Whether the End of Year data should be synced on startup",
        defaultValue = false,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = false,
    ),
    END_OF_YEAR_2024(
        key = "end_of_year_2024",
        title = "End of Year 2024",
        defaultValue = false,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = false,
    ),
    INTRO_PLUS_OFFER_ENABLED(
        key = "intro_plus_offer_enabled",
        title = "Intro Offer Plus",
        defaultValue = BuildConfig.DEBUG,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),
    SLUMBER_STUDIOS_YEARLY_PROMO(
        key = "slumber_studios_yearly_promo_code",
        title = "Slumber Studios Yearly Promo",
        defaultValue = BuildConfig.DEBUG,
        tier = FeatureTier.Plus(null),
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),
    NOVA_LAUNCHER(
        key = "nova_launcher",
        title = "Integrate Pocket Casts with Nova Launcher",
        defaultValue = BuildConfig.DEBUG,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = false,
        hasDevToggle = true,
    ),
    CACHE_ENTIRE_PLAYING_EPISODE(
        key = "cache_entire_playing_episode",
        title = "Cache entire playing episode",
        defaultValue = true,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),
    REIMAGINE_SHARING(
        key = "reimagine_sharing",
        title = "Use new sharing designs",
        defaultValue = true,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),
    EXPLAT_EXPERIMENT(
        key = "explat_experiment",
        title = "ExPlat Experiment",
        defaultValue = BuildConfig.DEBUG,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),
    ENGAGE_SDK(
        key = "engage_sdk",
        title = "Integrate Pocket Casts with Engage SDK",
        defaultValue = true,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = false,
    ),
    AUTO_DOWNLOAD(
        key = "auto_download",
        title = "Auto download episodes after subscribing to a podcast",
        defaultValue = BuildConfig.DEBUG,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),
    UP_NEXT_SHUFFLE(
        key = "up_next_shuffle",
        title = "Up Next Shuffle",
        defaultValue = BuildConfig.DEBUG,
        tier = FeatureTier.Plus(),
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),
    MANAGE_DOWNLOADED_EPISODES(
        key = "manage_downloaded_episodes",
        title = "Manage Downloaded Episodes",
        defaultValue = BuildConfig.DEBUG,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),
    RESET_EPISODE_CACHE_ON_416_ERROR(
        key = "reset_episode_cache_on_416_error",
        title = "Reset episode cache on 416 error",
        defaultValue = true,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = false,
    ),
    BASIC_AUTHENTICATION(
        key = "basic_authentication",
        title = "Support episode basic authentication",
        defaultValue = BuildConfig.DEBUG,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),
    PODCAST_HTML_DESCRIPTION(
        key = "podcast_html_description",
        title = "Use HTML in the podcast description",
        defaultValue = true,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),
    SHARE_PODCAST_PRIVATE_NOT_AVAILABLE(
        key = "share_podcast_private_not_available",
        title = "Sharing is not available for private podcasts",
        defaultValue = BuildConfig.DEBUG,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),
    WINBACK(
        key = "winback",
        title = "Winback flow",
        defaultValue = BuildConfig.DEBUG,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),
    PODCAST_FEED_UPDATE(
        key = "podcast_feed_update",
        title = "Podcast Feed Update",
        defaultValue = BuildConfig.DEBUG,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),
    SUGGESTED_FOLDERS(
        key = "suggested_folders",
        title = "Suggested Folders",
        defaultValue = BuildConfig.DEBUG,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),
    PODCASTS_SORT_CHANGES(
        key = "podcasts_sort_changes",
        title = "Podcasts Sort Changes",
        defaultValue = true,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),
    GUEST_LISTS_NETWORK_HIGHLIGHTS_REDESIGN(
        key = "guest_lists_network_highlights_redesign",
        title = "Guest Lists and Network Highlights Redesign",
        defaultValue = BuildConfig.DEBUG,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = false,
        hasDevToggle = true,
    ),
    GENERATED_TRANSCRIPTS(
        key = "generated_transcripts",
        title = "Generated transcripts",
        defaultValue = BuildConfig.DEBUG,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),
    APPSFLYER_ANALYTICS(
        key = "appsflyer_analytics",
        title = "AppsFlyer Analytics",
        defaultValue = true,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),
    LIBRO_FM(
        key = "libro_fm",
        title = "Libro FM in Upsell",
        defaultValue = BuildConfig.DEBUG,
        tier = FeatureTier.Plus(),
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),
    ENCOURAGE_ACCOUNT_CREATION(
        key = "encourage_account_creation",
        title = "Account creation encouragement",
        defaultValue = BuildConfig.DEBUG,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),
    RECOMMENDATIONS(
        key = "recommendations",
        title = "Recommendations",
        defaultValue = BuildConfig.DEBUG,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),

    // This is set of features used only for testing purposes.
    TEST_FREE_FEATURE(
        key = "test_free_feautre",
        title = "Free feature used for testing",
        defaultValue = true,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = false,
        hasDevToggle = false,
    ),
    TEST_PLUS_FEATURE(
        key = "test_plus_feautre",
        title = "Plus feature used for testing",
        defaultValue = true,
        tier = FeatureTier.Plus(),
        hasFirebaseRemoteFlag = false,
        hasDevToggle = false,
    ),
    TEST_PLUS_RESTRICTED_FEATURE(
        key = "test_plus_restricted_feautre",
        title = "Plus feature with Patron exclusive access used for testing",
        defaultValue = true,
        tier = FeatureTier.Plus(ReleaseVersion(1, 0)),
        hasFirebaseRemoteFlag = false,
        hasDevToggle = false,
    ),
    TEST_PATRON_FEATURE(
        key = "test_patron_feautre",
        title = "Patron feature used for testing",
        defaultValue = true,
        tier = FeatureTier.Patron,
        hasFirebaseRemoteFlag = false,
        hasDevToggle = false,
    ),
}

sealed class FeatureTier {
    data object Free : FeatureTier()

    class Plus(
        val patronExclusiveAccessRelease: ReleaseVersion? = null,
    ) : FeatureTier()

    data object Patron : FeatureTier()
}

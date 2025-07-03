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
        defaultValue = true,
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
    NOTIFICATIONS_REVAMP(
        key = "notifications_revamp",
        title = "Notifications Revamp",
        defaultValue = BuildConfig.DEBUG,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),
    BANNER_ADS(
        key = "banner_ads",
        title = "Banner Ads",
        defaultValue = BuildConfig.DEBUG,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = false,
        hasDevToggle = true,
    ),
    SMART_CATEGORIES(
        key = "smart_categories",
        title = "Smart Categories",
        defaultValue = BuildConfig.DEBUG,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),

    // This is set of features used only for testing purposes.
    TEST_FREE_FEATURE(
        key = "test_free_feature",
        title = "Free feature used for testing",
        defaultValue = true,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = false,
        hasDevToggle = false,
    ),
    TEST_PLUS_FEATURE(
        key = "test_plus_feature",
        title = "Plus feature used for testing",
        defaultValue = true,
        tier = FeatureTier.Plus(),
        hasFirebaseRemoteFlag = false,
        hasDevToggle = false,
    ),
    TEST_PLUS_RESTRICTED_FEATURE(
        key = "test_plus_restricted_feature",
        title = "Plus feature with Patron exclusive access used for testing",
        defaultValue = true,
        tier = FeatureTier.Plus(ReleaseVersion(1, 0)),
        hasFirebaseRemoteFlag = false,
        hasDevToggle = false,
    ),
    TEST_PATRON_FEATURE(
        key = "test_patron_feature",
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

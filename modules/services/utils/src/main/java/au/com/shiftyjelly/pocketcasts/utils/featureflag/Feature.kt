package au.com.shiftyjelly.pocketcasts.utils.featureflag

import au.com.shiftyjelly.pocketcasts.helper.BuildConfig
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersion.Companion.comparedToEarlyPatronAccess

enum class Feature(
    val key: String,
    val title: String,
    val defaultValue: Boolean,
    val tier: FeatureTier,
    val hasFirebaseRemoteFlag: Boolean,
    val hasDevToggle: Boolean,
) {
    END_OF_YEAR_ENABLED(
        key = "end_of_year_enabled",
        title = "End of Year",
        defaultValue = false,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = false,
        hasDevToggle = false,
    ),
    GIVE_RATINGS(
        key = "give_ratings",
        title = "Give Ratings",
        defaultValue = BuildConfig.DEBUG,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = false,
        hasDevToggle = true,
    ),
    SETTINGS_SYNC(
        key = "settings_sync",
        title = "Settings Sync",
        defaultValue = false,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),
    REPORT_VIOLATION(
        key = "report_violation",
        title = "Report Violation",
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
        hasDevToggle = false,
    ),
    SLUMBER_STUDIOS_YEARLY_PROMO(
        key = "slumber_studios_yearly_promo_code",
        title = "Slumber Studios Yearly Promo",
        defaultValue = BuildConfig.DEBUG,
        tier = FeatureTier.Plus(null),
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),
    DESELECT_CHAPTERS(
        key = "deselect_chapters_enabled",
        title = "Deselect Chapters",
        defaultValue = true,
        tier = FeatureTier.Plus(ReleaseVersion(7, 60)),
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),
    CACHE_PLAYING_EPISODE(
        key = "cache_playing_episode",
        title = "Cache playing episode",
        defaultValue = BuildConfig.DEBUG,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = false,
    ),
    CATEGORIES_REDESIGN(
        key = "CATEGORIES_REDESIGN",
        title = "Podcasts by category shown in discover view",
        defaultValue = true,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),
    UPNEXT_IN_TAB_BAR(
        key = "upnext_in_tab_bar",
        title = "Show Up Next in tab bar",
        defaultValue = true,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),
    PODCASTS_GRID_VIEW_DESIGN_CHANGES(
        key = "podcasts_grid_view_changes",
        title = "Podcasts grid view design changes",
        defaultValue = true,
        tier = FeatureTier.Free,
        hasFirebaseRemoteFlag = true,
        hasDevToggle = true,
    ),
    ;

    companion object {

        fun isUserEntitled(
            feature: Feature,
            userTier: UserTier,
            releaseVersion: ReleaseVersionWrapper = ReleaseVersionWrapper(),
        ) = when (userTier) {
            // Patron users can use all features
            UserTier.Patron -> when (feature.tier) {
                FeatureTier.Patron,
                is FeatureTier.Plus,
                FeatureTier.Free,
                -> true
            }

            UserTier.Plus -> {
                when (feature.tier) {
                    // Patron features can only be used by Patrons
                    FeatureTier.Patron -> false

                    // Plus users cannot use Plus features during early access for patrons except when the app is in beta
                    is FeatureTier.Plus -> {
                        val isReleaseCandidate = releaseVersion.currentReleaseVersion.releaseCandidate != null
                        val relativeToEarlyAccess = feature.tier.patronExclusiveAccessRelease?.let {
                            releaseVersion.currentReleaseVersion.comparedToEarlyPatronAccess(it)
                        }
                        when (relativeToEarlyAccess) {
                            null -> true // no early access release
                            EarlyAccessState.Before,
                            EarlyAccessState.During,
                            -> isReleaseCandidate
                            EarlyAccessState.After -> true
                        }
                    }

                    FeatureTier.Free -> true
                }
            }

            // Free users can only use free features
            UserTier.Free -> when (feature.tier) {
                FeatureTier.Patron -> false
                is FeatureTier.Plus -> false
                FeatureTier.Free -> true
            }
        }
    }

    // Please do not delete this method because sometimes we need it
    fun isCurrentlyExclusiveToPatron(
        releaseVersion: ReleaseVersionWrapper = ReleaseVersionWrapper(),
    ): Boolean {
        val isReleaseCandidate = releaseVersion.currentReleaseVersion.releaseCandidate != null
        val relativeToEarlyAccessState = (this.tier as? FeatureTier.Plus)?.patronExclusiveAccessRelease?.let {
            releaseVersion.currentReleaseVersion.comparedToEarlyPatronAccess(it)
        }
        return when (relativeToEarlyAccessState) {
            null -> false
            EarlyAccessState.Before,
            EarlyAccessState.During,
            -> !isReleaseCandidate
            EarlyAccessState.After -> false
        }
    }
}

// It would be nice to be able to use Subscription.SubscriptionTier here, but that's in the
// models module, which already depends on this featureflag module, so we can't depend on it
enum class UserTier {
    Patron,
    Plus,
    Free,
}

sealed class FeatureTier {
    data object Patron : FeatureTier()
    class Plus(val patronExclusiveAccessRelease: ReleaseVersion?) : FeatureTier()
    data object Free : FeatureTier()
}

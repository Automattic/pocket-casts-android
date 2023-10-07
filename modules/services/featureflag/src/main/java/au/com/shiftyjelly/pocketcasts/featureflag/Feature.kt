package au.com.shiftyjelly.pocketcasts.featureflag

import au.com.shiftyjelly.pocketcasts.featureflag.ReleaseVersion.Companion.comparedToEarlyPatronAccess

enum class Feature(
    val key: String,
    val title: String,
    val defaultValue: Boolean,
    val tier: FeatureTier,
    val hasDevToggle: Boolean,
) {
    END_OF_YEAR_ENABLED(
        key = "end_of_year_enabled",
        title = "End of Year",
        defaultValue = false,
        tier = FeatureTier.Free,
        hasDevToggle = false,
    ),
    SHOW_RATINGS_ENABLED(
        key = "show_ratings_enabled",
        title = "Show Ratings",
        defaultValue = true,
        tier = FeatureTier.Free,
        hasDevToggle = false,
    ),
    ADD_PATRON_ENABLED(
        key = "add_patron_enabled",
        title = "Patron",
        defaultValue = false,
        tier = FeatureTier.Free,
        hasDevToggle = true,
    ),
    BOOKMARKS_ENABLED(
        key = "bookmarks_enabled",
        title = "Bookmarks",
        defaultValue = BuildConfig.DEBUG,
        tier = FeatureTier.Plus(null),
        hasDevToggle = true,
    ),
    IN_APP_REVIEW_ENABLED(
        key = "in_app_review_enabled",
        title = "In App Review",
        defaultValue = false,
        tier = FeatureTier.Free,
        hasDevToggle = true,
    );

    companion object {

        fun isAvailable(
            feature: Feature,
            userTier: UserTier,
            releaseVersion: ReleaseVersionWrapper = ReleaseVersionWrapper(),
        ) = when (userTier) {

            // Patron users can use all features
            UserTier.Patron -> when (feature.tier) {
                FeatureTier.Patron,
                is FeatureTier.Plus,
                FeatureTier.Free -> FeatureFlag.isEnabled(feature)
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
                        FeatureFlag.isEnabled(feature) &&
                            when (relativeToEarlyAccess) {
                                null -> true // no early access release
                                EarlyAccessState.Before,
                                EarlyAccessState.During -> isReleaseCandidate
                                EarlyAccessState.After -> true
                            }
                    }

                    FeatureTier.Free -> FeatureFlag.isEnabled(feature)
                }
            }

            // Free users can only use free features
            UserTier.Free -> when (feature.tier) {
                FeatureTier.Patron -> false
                is FeatureTier.Plus -> false
                FeatureTier.Free -> FeatureFlag.isEnabled(feature)
            }
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
    object Patron : FeatureTier()
    class Plus(val patronExclusiveAccessRelease: ReleaseVersion?) : FeatureTier()
    object Free : FeatureTier()
}

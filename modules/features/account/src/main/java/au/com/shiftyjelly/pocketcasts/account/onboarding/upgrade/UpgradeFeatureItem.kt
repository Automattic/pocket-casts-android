package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

interface UpgradeFeatureItem {
    @get:DrawableRes val image: Int

    @get:StringRes val title: Int

    @get:StringRes val text: Int?

    val isYearlyFeature: Boolean
    val isMonthlyFeature: Boolean
}

enum class PlusUpgradeFeatureItem(
    override val image: Int,
    override val title: Int,
    override val text: Int? = null,
    override val isYearlyFeature: Boolean = true,
    override val isMonthlyFeature: Boolean = true,
) : UpgradeFeatureItem {
    DesktopApps(
        image = IR.drawable.ic_desktop_apps,
        title = LR.string.onboarding_plus_feature_desktop_and_web_apps_title,
    ),
    Folders(
        image = IR.drawable.ic_folders,
        title = LR.string.onboarding_plus_feature_folders_and_bookmarks_title,
    ),
    SkipChapters(
        image = IR.drawable.ic_tick_circle_filled,
        title = LR.string.skip_chapters,
        isYearlyFeature = FeatureFlag.isEnabled(Feature.DESELECT_CHAPTERS) &&
            SubscriptionTier.fromFeatureTier(Feature.DESELECT_CHAPTERS) == SubscriptionTier.PLUS,
        isMonthlyFeature = FeatureFlag.isEnabled(Feature.DESELECT_CHAPTERS) &&
            SubscriptionTier.fromFeatureTier(Feature.DESELECT_CHAPTERS) == SubscriptionTier.PLUS,
    ),
    CloudStorage(
        image = IR.drawable.ic_cloud_storage,
        title = LR.string.onboarding_plus_feature_cloud_storage_title,
    ),
    WatchPlayback(
        image = IR.drawable.ic_watch_play,
        title = LR.string.onboarding_plus_feature_watch_playback,
    ),
    ThemesIcons(
        image = IR.drawable.ic_themes,
        title = LR.string.onboarding_plus_feature_extra_themes_icons_title,
    ),
    UndyingGratitude(
        image = IR.drawable.ic_heart,
        title = LR.string.onboarding_plus_feature_gratitude_title,
        isYearlyFeature = !FeatureFlag.isEnabled(Feature.SLUMBER_STUDIOS_YEARLY_PROMO),
    ),
    SlumberStudiosPromo(
        image = IR.drawable.ic_slumber_studios,
        title = LR.string.onboarding_plus_feature_slumber_studios_title,
        isMonthlyFeature = false,
        isYearlyFeature = FeatureFlag.isEnabled(Feature.SLUMBER_STUDIOS_YEARLY_PROMO),
    ),
}

enum class PatronUpgradeFeatureItem(
    override val image: Int,
    override val title: Int,
    override val text: Int? = null,
    override val isYearlyFeature: Boolean = true,
    override val isMonthlyFeature: Boolean = true,
) : UpgradeFeatureItem {
    EverythingInPlus(
        image = IR.drawable.ic_check,
        title = LR.string.onboarding_patron_feature_everything_in_plus_title,
    ),
    EarlyAccess(
        image = IR.drawable.ic_new_features,
        title = LR.string.onboarding_patron_feature_early_access_title,
    ),
    SkipChapters(
        image = IR.drawable.ic_tick_circle_filled,
        title = LR.string.skip_chapters,
        isYearlyFeature = FeatureFlag.isEnabled(Feature.DESELECT_CHAPTERS) &&
            SubscriptionTier.fromFeatureTier(Feature.DESELECT_CHAPTERS) == SubscriptionTier.PATRON,
        isMonthlyFeature = FeatureFlag.isEnabled(Feature.DESELECT_CHAPTERS) &&
            SubscriptionTier.fromFeatureTier(Feature.DESELECT_CHAPTERS) == SubscriptionTier.PATRON,
    ),
    CloudStorage(
        image = IR.drawable.ic_cloud_storage,
        title = LR.string.onboarding_patron_feature_cloud_storage_title,
    ),
    ProfileBadge(
        image = IR.drawable.ic_profile_badge,
        title = LR.string.onboarding_patron_feature_profile_badge_title,
    ),
    SpecialIcons(
        image = IR.drawable.ic_icons,
        title = LR.string.onboarding_patron_feature_special_icons_title,
    ),
    UndyingGratitude(
        image = IR.drawable.ic_heart,
        title = LR.string.onboarding_patron_feature_gratitude_title,
    ),
}

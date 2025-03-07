package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

interface UpgradeFeatureItem {
    @get:DrawableRes
    val image: Int

    @get:StringRes
    val title: Int

    @get:StringRes
    val text: Int?

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
    Folders(
        image = IR.drawable.ic_plus_feature_folder,
        title = LR.string.onboarding_plus_feature_folders_title,
    ),
    UpNextShuffle(
        image = IR.drawable.ic_plus_feature_shuffle,
        title = LR.string.onboarding_plus_feature_up_next_shuffle_title,
    ),
    Bookmarks(
        image = IR.drawable.ic_plus_feature_bookmark,
        title = LR.string.onboarding_plus_feature_bookmarks_title,
    ),
    SkipChapters(
        image = IR.drawable.ic_plus_feature_chapters,
        title = LR.string.onboarding_plus_feature_chapters_title,
    ),
    CloudStorage(
        image = IR.drawable.ic_plus_feature_cloud_storage,
        title = LR.string.onboarding_plus_feature_cloud_storage_title,
    ),
    WatchPlayback(
        image = IR.drawable.ic_plus_feature_wearable,
        title = LR.string.onboarding_plus_feature_watch_playback,
    ),
    ThemesIcons(
        image = IR.drawable.ic_plus_feature_themes,
        title = LR.string.onboarding_plus_feature_extra_themes_icons_title,
    ),
    SlumberStudiosPromo(
        image = IR.drawable.ic_plus_feature_slumber_studios,
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

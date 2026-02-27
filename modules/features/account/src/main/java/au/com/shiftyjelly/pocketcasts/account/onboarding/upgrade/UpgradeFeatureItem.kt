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

    @StringRes
    fun title(): Int

    val isYearlyFeature: Boolean

    val isMonthlyFeature: Boolean
}

enum class PlusUpgradeFeatureItem(
    override val image: Int,
) : UpgradeFeatureItem {
    BannerAds(
        image = IR.drawable.ic_remove_ads,
    ) {
        override val isYearlyFeature get() = FeatureFlag.isEnabled(Feature.BANNER_ADS_PLAYER) || FeatureFlag.isEnabled(Feature.BANNER_ADS_PODCASTS)
        override val isMonthlyFeature get() = FeatureFlag.isEnabled(Feature.BANNER_ADS_PLAYER) || FeatureFlag.isEnabled(Feature.BANNER_ADS_PODCASTS)
        override fun title() = LR.string.onboarding_plus_feature_no_banner_ads
    },
    Transcripts(
        image = IR.drawable.ic_transcript_24,
    ) {
        override fun title() = LR.string.onboarding_upgrade_features_transcripts
    },
    Folders(
        image = IR.drawable.ic_plus_feature_folder,
    ) {
        override fun title() = LR.string.onboarding_upgrade_features_folders
    },
    UpNextShuffle(
        image = IR.drawable.ic_plus_feature_shuffle,
    ) {
        override fun title() = LR.string.onboarding_upgrade_features_shuffle
    },
    Bookmarks(
        image = IR.drawable.ic_plus_feature_bookmark,
    ) {
        override fun title() = LR.string.onboarding_upgrade_features_bookmarks
    },
    SkipChapters(
        image = IR.drawable.ic_plus_feature_chapters,
    ) {
        override fun title() = LR.string.onboarding_upgrade_features_chapters
    },
    CloudStorage(
        image = IR.drawable.ic_plus_feature_cloud_storage,
    ) {
        override fun title() = LR.string.onboarding_upgrade_features_files
    },
    WatchPlayback(
        image = IR.drawable.ic_plus_feature_wearable,
    ) {
        override fun title() = LR.string.onboarding_upgrade_features_wear
    },
    ThemesIcons(
        image = IR.drawable.ic_plus_feature_themes,
    ) {
        override fun title() = LR.string.onboarding_upgrade_features_themes
    },
    SlumberStudiosPromo(
        image = IR.drawable.ic_plus_feature_slumber_studios,
    ) {
        override val isMonthlyFeature get() = false
        override val isYearlyFeature get() = FeatureFlag.isEnabled(Feature.SLUMBER_STUDIOS_YEARLY_PROMO)
        override fun title() = LR.string.onboarding_upgrade_features_slumber
    },
    LibroFm(
        image = IR.drawable.ic_plus_feature_libro,
    ) {

        override val isYearlyFeature get() = FeatureFlag.isEnabled(Feature.LIBRO_FM)
        override val isMonthlyFeature get() = FeatureFlag.isEnabled(Feature.LIBRO_FM)
        override fun title() = LR.string.onboarding_plus_feature_libro_title
    },
    ;

    override val isYearlyFeature get() = true
    override val isMonthlyFeature get() = true
}

enum class PatronUpgradeFeatureItem(
    override val image: Int,
) : UpgradeFeatureItem {
    EverythingInPlus(
        image = IR.drawable.ic_check,
    ) {
        override fun title() = LR.string.onboarding_upgrade_patron_plus_features
    },
    EarlyAccess(
        image = IR.drawable.ic_new_features,
    ) {
        override fun title() = LR.string.onboarding_upgrade_patron_early_access
    },
    CloudStorage(
        image = IR.drawable.ic_cloud_storage,
    ) {
        override fun title() = LR.string.onboarding_upgrade_patron_cloud_storage
    },
    ProfileBadge(
        image = IR.drawable.ic_profile_badge,
    ) {
        override fun title() = LR.string.onboarding_upgrade_patron_badge
    },
    SpecialIcons(
        image = IR.drawable.ic_icons,
    ) {
        override fun title() = LR.string.onboarding_upgrade_app_icons
    },
    UndyingGratitude(
        image = IR.drawable.ic_heart,
    ) {
        override fun title() = LR.string.onboarding_upgrade_gratitude
    },
    ;

    override val isYearlyFeature get() = true
    override val isMonthlyFeature get() = true
}

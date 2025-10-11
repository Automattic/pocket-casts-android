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
        override fun title() = if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
            LR.string.onboarding_upgrade_features_folders
        } else {
            LR.string.onboarding_plus_feature_folders_title
        }
    },
    UpNextShuffle(
        image = IR.drawable.ic_plus_feature_shuffle,
    ) {
        override fun title() = if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
            LR.string.onboarding_upgrade_features_shuffle
        } else {
            LR.string.onboarding_plus_feature_up_next_shuffle_title
        }
    },
    Bookmarks(
        image = IR.drawable.ic_plus_feature_bookmark,
    ) {
        override fun title() = if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
            LR.string.onboarding_upgrade_features_bookmarks
        } else {
            LR.string.onboarding_plus_feature_bookmarks_title
        }
    },
    SkipChapters(
        image = IR.drawable.ic_plus_feature_chapters,
    ) {
        override fun title() = if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
            LR.string.onboarding_upgrade_features_chapters
        } else {
            LR.string.onboarding_plus_feature_chapters_title
        }
    },
    CloudStorage(
        image = IR.drawable.ic_plus_feature_cloud_storage,
    ) {
        override fun title() = if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
            LR.string.onboarding_upgrade_features_files
        } else {
            LR.string.onboarding_plus_feature_cloud_storage_title
        }
    },
    WatchPlayback(
        image = IR.drawable.ic_plus_feature_wearable,
    ) {
        override fun title() = if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
            LR.string.onboarding_upgrade_features_wear
        } else {
            LR.string.onboarding_plus_feature_watch_playback
        }
    },
    ThemesIcons(
        image = IR.drawable.ic_plus_feature_themes,
    ) {
        override fun title() = if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
            LR.string.onboarding_upgrade_features_themes
        } else {
            LR.string.onboarding_plus_feature_extra_themes_icons_title
        }
    },
    SlumberStudiosPromo(
        image = IR.drawable.ic_plus_feature_slumber_studios,
    ) {
        override val isMonthlyFeature get() = false
        override val isYearlyFeature get() = FeatureFlag.isEnabled(Feature.SLUMBER_STUDIOS_YEARLY_PROMO)
        override fun title() = if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
            LR.string.onboarding_upgrade_features_slumber
        } else {
            LR.string.onboarding_plus_feature_slumber_studios_title
        }
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
        override fun title() = if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
            LR.string.onboarding_upgrade_patron_plus_features
        } else {
            LR.string.onboarding_patron_feature_everything_in_plus_title
        }
    },
    EarlyAccess(
        image = IR.drawable.ic_new_features,
    ) {
        override fun title() = if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
            LR.string.onboarding_upgrade_patron_early_access
        } else {
            LR.string.onboarding_patron_feature_early_access_title
        }
    },
    CloudStorage(
        image = IR.drawable.ic_cloud_storage,
    ) {
        override fun title() = if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
            LR.string.onboarding_upgrade_patron_cloud_storage
        } else {
            LR.string.onboarding_patron_feature_cloud_storage_title
        }
    },
    ProfileBadge(
        image = IR.drawable.ic_profile_badge,
    ) {
        override fun title() = if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
            LR.string.onboarding_upgrade_patron_badge
        } else {
            LR.string.onboarding_patron_feature_profile_badge_title
        }
    },
    SpecialIcons(
        image = IR.drawable.ic_icons,
    ) {
        override fun title() = if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
            LR.string.onboarding_upgrade_app_icons
        } else {
            LR.string.onboarding_patron_feature_special_icons_title
        }
    },
    UndyingGratitude(
        image = IR.drawable.ic_heart,
    ) {
        override fun title() = if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
            LR.string.onboarding_upgrade_gratitude
        } else {
            LR.string.onboarding_patron_feature_gratitude_title
        }
    },
    ;

    override val isYearlyFeature get() = true
    override val isMonthlyFeature get() = true
}

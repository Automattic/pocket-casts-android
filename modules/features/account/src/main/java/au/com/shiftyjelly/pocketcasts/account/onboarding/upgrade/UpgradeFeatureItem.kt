package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

interface UpgradeFeatureItem {
    @get:DrawableRes val image: Int
    @get:StringRes val title: Int
    @get:StringRes val text: Int?
}

enum class PlusUpgradeFeatureItem(
    override val image: Int,
    override val title: Int,
    override val text: Int? = null,
) : UpgradeFeatureItem {
    DesktopApps(
        image = IR.drawable.ic_desktop_apps,
        title = LR.string.onboarding_plus_feature_desktop_apps_title,
    ),
    Folders(
        image = IR.drawable.ic_folders,
        title = LR.string.onboarding_plus_feature_folders_title,
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
    ),
}

enum class PatronUpgradeFeatureItem(
    override val image: Int,
    override val title: Int,
    override val text: Int? = null,
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

enum class OldPlusUpgradeFeatureItem(
    override val image: Int,
    override val title: Int,
    override val text: Int,
) : UpgradeFeatureItem {
    DesktopApps(
        image = IR.drawable.desktop_apps,
        title = LR.string.onboarding_plus_feature_desktop_apps_title,
        text = LR.string.onboarding_plus_feature_desktop_apps_text,
    ),
    Folders(
        image = IR.drawable.folder,
        title = LR.string.onboarding_plus_feature_folders_title,
        text = LR.string.onboarding_plus_feature_folders_text,
    ),
    CloudStorage(
        image = IR.drawable.cloud_storage,
        title = LR.string.onboarding_plus_feature_cloud_storage_title,
        text = LR.string.onboarding_plus_feature_cloud_storage_text,
    ),
    ThemesIcons(
        image = IR.drawable.themes_icons,
        title = LR.string.onboarding_plus_feature_themes_icons_title,
        text = LR.string.onboarding_plus_feature_themes_icons_text,
    ),
}

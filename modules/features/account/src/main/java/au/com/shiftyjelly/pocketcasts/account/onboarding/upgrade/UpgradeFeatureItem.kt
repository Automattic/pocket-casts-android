package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.images.R

interface UpgradeFeatureItem {
    @get:DrawableRes val image: Int
    @get:StringRes val title: Int
    @get:StringRes val text: Int
}

enum class OldPlusUpgradeFeatureItem(
    override val image: Int,
    override val title: Int,
    override val text: Int,
) : UpgradeFeatureItem {
    DesktopApps(
        image = R.drawable.desktop_apps,
        title = au.com.shiftyjelly.pocketcasts.localization.R.string.onboarding_plus_feature_desktop_apps_title,
        text = au.com.shiftyjelly.pocketcasts.localization.R.string.onboarding_plus_feature_desktop_apps_text,
    ),
    Folders(
        image = R.drawable.folder,
        title = au.com.shiftyjelly.pocketcasts.localization.R.string.onboarding_plus_feature_folders_title,
        text = au.com.shiftyjelly.pocketcasts.localization.R.string.onboarding_plus_feature_folders_text,
    ),
    CloudStorage(
        image = R.drawable.cloud_storage,
        title = au.com.shiftyjelly.pocketcasts.localization.R.string.onboarding_plus_feature_cloud_storage_title,
        text = au.com.shiftyjelly.pocketcasts.localization.R.string.onboarding_plus_feature_cloud_storage_text,
    ),
    ThemesIcons(
        image = R.drawable.themes_icons,
        title = au.com.shiftyjelly.pocketcasts.localization.R.string.onboarding_plus_feature_themes_icons_title,
        text = au.com.shiftyjelly.pocketcasts.localization.R.string.onboarding_plus_feature_themes_icons_text,
    ),
}

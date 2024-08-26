package au.com.shiftyjelly.pocketcasts.sharing.social

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.utils.getPackageInfo
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class SocialPlatform(
    @DrawableRes val logoId: Int,
    @StringRes val nameId: Int,
    val packageId: String?,
) {
    Instagram(
        logoId = IR.drawable.ic_share_logo_instagram,
        nameId = LR.string.share_label_instagram_stories,
        packageId = "com.instagram.android",
    ),
    WhatsApp(
        logoId = IR.drawable.ic_share_logo_whats_app,
        nameId = LR.string.share_label_whats_app,
        packageId = "com.whatsapp",
    ),
    Telegram(
        logoId = IR.drawable.ic_share_logo_telegram,
        nameId = LR.string.share_label_telegram,
        packageId = "org.telegram.messenger",
    ),
    X(
        logoId = IR.drawable.ic_share_logo_x,
        nameId = LR.string.share_label_x,
        packageId = "com.twitter.android",
    ),
    Tumblr(
        logoId = IR.drawable.ic_share_logo_tumblr,
        nameId = LR.string.share_label_tumblr,
        packageId = "com.tumblr",
    ),
    PocketCasts(
        logoId = IR.drawable.ic_share_logo_pocket_casts,
        nameId = LR.string.share_label_copy_link,
        packageId = null,
    ),
    More(
        logoId = IR.drawable.ic_share_logo_more,
        nameId = LR.string.share_label_more,
        packageId = null,
    ),
    ;

    companion object {
        fun getAvailablePlatforms(context: Context): Set<SocialPlatform> = buildSet {
            SocialPlatform.entries.forEach { platform ->
                if (platform.packageId?.let(context::getPackageInfo) != null) {
                    add(platform)
                }
            }
        }
    }
}

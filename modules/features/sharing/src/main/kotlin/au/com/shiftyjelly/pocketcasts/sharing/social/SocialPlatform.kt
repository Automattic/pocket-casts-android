package au.com.shiftyjelly.pocketcasts.sharing.social

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.images.R

internal enum class SocialPlatform(
    @DrawableRes val logoId: Int,
    @StringRes val nameId: Int,
) {
    Instagram(
        logoId = R.drawable.ic_share_logo_instagram,
        nameId = au.com.shiftyjelly.pocketcasts.localization.R.string.share_label_instagram_stories,
    ),
    WhatsApp(
        logoId = R.drawable.ic_share_logo_whats_app,
        nameId = au.com.shiftyjelly.pocketcasts.localization.R.string.share_label_whats_app,
    ),
    Telegram(
        logoId = R.drawable.ic_share_logo_telegram,
        nameId = au.com.shiftyjelly.pocketcasts.localization.R.string.share_label_telegram,
    ),
    X(
        logoId = R.drawable.ic_share_logo_x,
        nameId = au.com.shiftyjelly.pocketcasts.localization.R.string.share_label_x,
    ),
    Tumblr(
        logoId = R.drawable.ic_share_logo_tumblr,
        nameId = au.com.shiftyjelly.pocketcasts.localization.R.string.share_label_tumblr,
    ),
    PocketCasts(
        logoId = R.drawable.ic_share_logo_pocket_casts,
        nameId = au.com.shiftyjelly.pocketcasts.localization.R.string.share_label_copy_link,
    ),
    More(
        logoId = R.drawable.ic_share_logo_more,
        nameId = au.com.shiftyjelly.pocketcasts.localization.R.string.share_label_more,
    ),
}

package au.com.shiftyjelly.pocketcasts.discover.extensions

import android.content.res.ColorStateList
import android.widget.ImageView
import androidx.annotation.AttrRes
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

fun ImageView.updateSubscribeButtonIcon(
    subscribed: Boolean,
    @AttrRes colorSubscribed: Int = UR.attr.support_02,
    @AttrRes colorUnsubscribed: Int = UR.attr.primary_icon_02,
) {
    val drawableRes = if (subscribed) IR.drawable.ic_check_black_24dp else IR.drawable.ic_add_black_24dp
    this.setImageResource(drawableRes)
    this.isEnabled = !subscribed
    this.contentDescription = this.context.getString(if (subscribed) LR.string.podcast_subscribed else LR.string.subscribe)

    val tintColor = context.getThemeColor(if (subscribed) colorSubscribed else colorUnsubscribed)
    this.imageTintList = ColorStateList.valueOf(tintColor)
}

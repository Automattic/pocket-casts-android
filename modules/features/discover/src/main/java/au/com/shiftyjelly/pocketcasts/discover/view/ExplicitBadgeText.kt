package au.com.shiftyjelly.pocketcasts.discover.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ImageSpan
import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.withSave
import androidx.core.view.doOnLayout
import au.com.shiftyjelly.pocketcasts.discover.R
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import kotlin.math.roundToInt
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

internal fun TextView.setPodcastTitleWithExplicitBadge(
    title: CharSequence?,
    explicit: Boolean?,
    @AttrRes badgeTintAttr: Int = UR.attr.primary_text_02,
) {
    val titleText = title?.toString().orEmpty()
    val showExplicitBadge = explicit == true && FeatureFlag.isEnabled(Feature.EXPLICIT_PODCAST_INDICATOR)
    if (!showExplicitBadge || titleText.isEmpty()) {
        setTag(R.id.explicit_badge_title, null)
        text = title
        contentDescription = null
        return
    }

    val explicitDescription = context.getString(LR.string.explicit)
    val badge = AppCompatResources.getDrawable(context, IR.drawable.explicit)?.mutate()
    if (badge == null) {
        setTag(R.id.explicit_badge_title, null)
        text = title
        contentDescription = "$titleText, $explicitDescription"
        return
    }

    val iconSize = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        16f,
        resources.displayMetrics,
    ).roundToInt()
    val wrappedBadge = DrawableCompat.wrap(badge)
    DrawableCompat.setTint(wrappedBadge, context.getThemeColor(badgeTintAttr))
    wrappedBadge.setBounds(0, 0, iconSize, iconSize)

    contentDescription = "$titleText, $explicitDescription"

    if (maxLines != 1) {
        // Multi-line titles wrap rather than ellipsize, so the badge can simply follow the title.
        setTag(R.id.explicit_badge_title, null)
        text = buildTitleWithBadge(titleText, wrappedBadge)
        return
    }

    // Single line titles ellipsize, which would otherwise truncate a trailing badge when the title is long.
    // Reserve room for the icon and ellipsize the title ourselves so the badge stays visible.
    setTag(R.id.explicit_badge_title, titleText)
    if (width > 0) {
        text = buildSingleLineTitleWithBadge(titleText, wrappedBadge, iconSize)
    } else {
        // The width is unknown until layout (e.g. the first bind in a RecyclerView).
        text = titleText
        doOnLayout {
            // Use the tag to make sure the row hasn't been recycled when doing the layout with the badge.
            if (getTag(R.id.explicit_badge_title) == titleText) {
                text = buildSingleLineTitleWithBadge(titleText, wrappedBadge, iconSize)
            }
        }
    }
}

private fun TextView.buildSingleLineTitleWithBadge(
    titleText: String,
    badge: Drawable,
    iconSize: Int,
): CharSequence {
    val available = width - compoundPaddingStart - compoundPaddingEnd
    if (available <= 0) {
        return buildTitleWithBadge(titleText, badge)
    }
    // Reserve space for the gap before the icon and the icon itself.
    val reserved = iconSize + paint.measureText(" ")
    val ellipsizedTitle = TextUtils.ellipsize(
        titleText,
        paint,
        (available - reserved).coerceAtLeast(0f),
        TextUtils.TruncateAt.END,
    )
    return buildTitleWithBadge(ellipsizedTitle, badge)
}

private fun buildTitleWithBadge(
    title: CharSequence,
    badge: Drawable,
): CharSequence {
    // The trailing non-breaking space carries the image, and the preceding space is the gap.
    return SpannableStringBuilder(title)
        .append(' ')
        .append(" ", CenteredImageSpan(badge), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
}

private class CenteredImageSpan(
    private val image: Drawable,
) : ImageSpan(image) {
    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint,
    ) {
        canvas.withSave {
            val transY = top + (bottom - top - image.bounds.height()) / 2
            translate(x, transY.toFloat())
            image.draw(this)
        }
    }
}

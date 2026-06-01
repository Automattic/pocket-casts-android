package au.com.shiftyjelly.pocketcasts.discover.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
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
        text = title
        contentDescription = null
        return
    }

    val explicitDescription = context.getString(LR.string.explicit)
    val badge = AppCompatResources.getDrawable(context, IR.drawable.explicit)?.mutate()
    if (badge == null) {
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

    val textWithBadge = SpannableString("$titleText  ")
    textWithBadge.setSpan(
        CenteredImageSpan(wrappedBadge),
        textWithBadge.length - 1,
        textWithBadge.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
    text = textWithBadge
    contentDescription = "$titleText, $explicitDescription"
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
        canvas.save()
        val transY = top + (bottom - top - image.bounds.height()) / 2
        canvas.translate(x, transY.toFloat())
        image.draw(canvas)
        canvas.restore()
    }
}

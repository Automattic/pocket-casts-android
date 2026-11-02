package au.com.shiftyjelly.pocketcasts.account

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.ImageViewCompat
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeDrawable
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class CarHeaderView(context: Context) : ConstraintLayout(context) {

    val closeButton = AppCompatImageButton(context)

    init {
        val top = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 0 else dp(40)
        closeButton.apply {
            id = R.id.btnClose
            setBackgroundResource(context.getThemeDrawable(androidx.appcompat.R.attr.actionBarItemBackground))
            contentDescription = context.getString(LR.string.cancel)
            setPadding(dp(20), dp(20), dp(20), dp(20))
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageResource(IR.drawable.ic_cancel)
            ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(context.getThemeColor(UR.attr.primary_icon_01)))
        }
        addView(
            closeButton,
            LayoutParams(dp(78), dp(78)).apply {
                startToStart = LayoutParams.PARENT_ID
                topToTop = LayoutParams.PARENT_ID
                marginStart = dp(20)
                topMargin = top
            },
        )
        val logo = AppCompatImageView(context).apply {
            id = R.id.imgLogo
            setImageResource(context.getThemeDrawable(UR.attr.logo_title_horizontal))
        }
        addView(
            logo,
            LayoutParams(dp(225), dp(45)).apply {
                startToEnd = R.id.btnClose
                endToEnd = LayoutParams.PARENT_ID
                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
                horizontalBias = 0f
                marginStart = dp(32)
                topMargin = top
            },
        )
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
}

package au.com.shiftyjelly.pocketcasts.views.tour

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.toRectF
import androidx.core.view.children
import androidx.transition.TransitionManager
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.utils.AnalyticsHelper
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class TourView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val backgroundView = TourBackgroundView(context)

    lateinit var tourName: String
    var steps: List<TourStep>? = null
    var stepIndex: Int = 0
    var currentStepView: TourStepView? = null
    var lastAnchor: Int? = null

    init {
        backgroundView.layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        backgroundView.isClickable = true
        backgroundView.id = UR.id.tour_background
        addView(backgroundView)

        visibility = View.GONE
    }

    fun startTour(steps: List<TourStep>, tourName: String) {
        this.steps = steps
        this.tourName = tourName
        if (steps.isEmpty()) {
            return
        }

        visibility = View.VISIBLE
        this.currentStepView = null
        showStep(steps.first())

        AnalyticsHelper.tourStarted(tourName)
    }

    fun showStep(step: TourStep) {
        val margin = 16.dpToPx(context)
        val inset = 8.dpToPx(context)

        if (currentStepView == null) {
            val stepView = TourStepView(context)
            val layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            layoutParams.marginStart = margin
            layoutParams.marginEnd = margin
            layoutParams.startToStart = LayoutParams.PARENT_ID
            layoutParams.endToEnd = LayoutParams.PARENT_ID
            stepView.layoutParams = layoutParams
            stepView.id = UR.id.stepview
            addView(stepView)

            this.currentStepView = stepView
        }
        val stepView = currentStepView ?: return
        stepView.setup(step) {
            stepIndex += 1
            this.steps?.let { steps ->
                if (stepIndex < steps.size) {
                    showStep(steps[stepIndex])
                } else {
                    AnalyticsHelper.tourCompleted(tourName)
                    (this.parent as? ViewGroup)?.removeView(this)
                }
            }
        }
        val closeText = if (stepIndex == 0) "Close" else "End Tour"
        stepView.setupCloseButton(closeText) {
            AnalyticsHelper.tourCancelled(tourName, atStep = stepIndex)
            (this.parent as? ViewGroup)?.removeView(this)
        }
        val stepText = if (stepIndex == 0) "NEW" else "$stepIndex of ${(steps?.size ?: 0) - 1}"
        val stepColor = if (stepIndex == 0) context.getThemeColor(UR.attr.support_05) else context.getThemeColor(UR.attr.primary_text_02)
        stepView.setupStepText(stepText, stepColor)

        val newConstraints = ConstraintSet().apply { clone(this@TourView) }

        if (step.viewTag != null) {
            val viewTag = step.viewTag

            val parent = (parent as? ViewGroup) ?: return
            val childView = when (viewTag) {
                is TourViewTag.ViewId -> parent.findViewById(viewTag.viewId)
                is TourViewTag.ChildWithClass<*> -> parent.findViewById<ViewGroup?>(viewTag.viewId)?.children?.find { it::class.java.canonicalName == viewTag.className?.canonicalName }
                is TourViewTag.TabId -> parent.children.filterIsInstance<BottomNavigationView>().firstOrNull()?.children?.filterIsInstance<BottomNavigationMenuView>()?.firstOrNull()?.children?.elementAtOrNull(viewTag.tabId)
            } ?: return

            val childDrawingRect = Rect()
            childView.getDrawingRect(childDrawingRect)
            parent.offsetDescendantRectToMyCoords(childView, childDrawingRect)
            childDrawingRect.inset(-inset, -inset)
            backgroundView.moveCutout(childDrawingRect.toRectF())

            lastAnchor?.let { newConstraints.clear(UR.id.stepview, it) } // Remove the constraint to the previous view
            if (step.gravity == Gravity.TOP) {
                newConstraints.connect(stepView.id, ConstraintSet.BOTTOM, LayoutParams.PARENT_ID, ConstraintSet.BOTTOM, height - childDrawingRect.top + margin)
                lastAnchor = ConstraintSet.BOTTOM
            } else {
                newConstraints.connect(stepView.id, ConstraintSet.TOP, LayoutParams.PARENT_ID, ConstraintSet.TOP, childDrawingRect.bottom + margin)
                lastAnchor = ConstraintSet.TOP
            }
        } else {
            newConstraints.connect(stepView.id, ConstraintSet.BOTTOM, LayoutParams.PARENT_ID, ConstraintSet.BOTTOM, margin)
            lastAnchor = ConstraintSet.BOTTOM

            backgroundView.hideCutout()
        }

        TransitionManager.beginDelayedTransition(this)
        newConstraints.applyTo(this)
    }
}

sealed class TourViewTag {
    data class ViewId(@IdRes val viewId: Int) : TourViewTag()
    data class ChildWithClass<T>(@IdRes val viewId: Int, val className: Class<T>?) : TourViewTag()
    data class TabId(val tabId: Int) : TourViewTag()
}

data class TourStep(val title: String, val description: String, val buttonText: String, val viewTag: TourViewTag?, val gravity: Int)

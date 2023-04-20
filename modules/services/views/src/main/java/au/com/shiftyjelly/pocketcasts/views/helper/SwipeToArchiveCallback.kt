package au.com.shiftyjelly.pocketcasts.views.helper

import android.graphics.Canvas
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.extensions.setRippleBackground
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import au.com.shiftyjelly.pocketcasts.ui.R as UR

interface RowSwipeable {
    val episodeRow: ViewGroup
    val episode: Episode?
    val swipeLeftIcon: ImageView
    val positionAdapter: Int
    val leftRightIcon1: ImageView
    val leftRightIcon2: ImageView
    val isMultiSelecting: Boolean
    val rightToLeftSwipeLayout: ViewGroup
    val leftToRightSwipeLayout: ViewGroup
    val upNextAction: Settings.UpNextAction
    val leftIconDrawablesRes: List<EpisodeItemTouchHelper.IconWithBackground>
    val rightIconDrawableRes: List<EpisodeItemTouchHelper.IconWithBackground>
}

class EpisodeItemTouchHelper(onLeftItem1: (episode: Episode, index: Int) -> Unit, onLeftItem2: (episode: Episode, index: Int) -> Unit, onSwipeLeftAction: (episode: Episode, index: Int) -> Unit) : MultiSwipeHelper(object : SwipeToArchiveCallback() {
    override fun onSwiped(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, direction: Int): Boolean {
        val episodeViewHolder = viewHolder as? RowSwipeable
            ?: return false

        val rowTranslation = episodeViewHolder.episodeRow.translationX
        val episode = episodeViewHolder.episode ?: return false
        val multiItemCutoff = episodeViewHolder.episodeRow.width * getMultiItemCutoffThreshold()
        if (direction == ItemTouchHelper.LEFT && rowTranslation < 0) {
            if (abs(rowTranslation) > multiItemCutoff) {
                onSwipeLeftAction(episode, episodeViewHolder.positionAdapter)
                clearView(recyclerView, viewHolder)
                return true
            } else {
                episodeViewHolder.swipeLeftIcon.setOnClickListener {
                    onSwipeLeftAction(episode, episodeViewHolder.positionAdapter)
                    clearView(recyclerView, viewHolder)
                }
                episodeViewHolder.swipeLeftIcon.setRippleBackground(true)
                return false
            }
        } else if (direction == ItemTouchHelper.RIGHT && rowTranslation > 0) {
            if (rowTranslation > multiItemCutoff) {
                onLeftItem1(episode, episodeViewHolder.positionAdapter)
                clearView(recyclerView, viewHolder)
                return true
            } else {
                episodeViewHolder.leftRightIcon1.setOnClickListener {
                    onLeftItem1(episode, episodeViewHolder.positionAdapter)
                    clearView(recyclerView, viewHolder)
                }
                episodeViewHolder.leftRightIcon1.setRippleBackground(true)
                episodeViewHolder.leftRightIcon2.setOnClickListener {
                    onLeftItem2(episode, episodeViewHolder.positionAdapter)
                    clearView(recyclerView, viewHolder)
                }
                episodeViewHolder.leftRightIcon2.setRippleBackground(true)
                return false
            }
        } else {
            clearView(recyclerView, viewHolder)
            return true
        }
    }

    override fun getClickableViews(viewHolder: RecyclerView.ViewHolder): List<View> {
        return if (viewHolder is RowSwipeable) {
            listOf(viewHolder.leftRightIcon1, viewHolder.leftRightIcon2, viewHolder.swipeLeftIcon)
        } else {
            emptyList()
        }
    }
}) {
    data class IconWithBackground(
        @DrawableRes val iconRes: Int,
        @ColorInt val backgroundColor: Int
    )

    fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder?) {
        if (viewHolder == null) return
        mCallback.clearView(recyclerView, viewHolder)
        mDx = 0f
        mDy = 0f
        mRecoverAnimations.clear()
    }

    enum class SwipeAction(val analyticsValue: String) {
        UP_NEXT_REMOVE("up_next_remove"),
        UP_NEXT_ADD_TOP("up_next_add_top"),
        UP_NEXT_ADD_BOTTOM("up_next_add_bottom"),
        UP_NEXT_MOVE_TOP("up_next_move_top"),
        UP_NEXT_MOVE_BOTTOM("up_next_move_bottom"),
        DELETE("delete"),
        UNARCHIVE("unarchive"),
        ARCHIVE("archive"),
    }

    enum class SwipeSource(val analyticsValue: String) {
        PODCAST_DETAILS("podcast_details"),
        FILTERS("filters"),
        DOWNLOADS("downloads"),
        LISTENING_HISTORY("listening_history"),
        STARRED("starred"),
        FILES("files"),
        UP_NEXT("up_next"),
    }
}

private abstract class SwipeToArchiveCallback() : MultiSwipeHelper.SimpleCallback(0, ItemTouchHelper.LEFT.or(ItemTouchHelper.RIGHT)) {
    var swipeDirection: Int? = 0

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        if (viewHolder !is RowSwipeable) {
            return 0
        }

        return super.getMovementFlags(recyclerView, viewHolder)
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)

        val episodeViewHolder = viewHolder as? RowSwipeable
            ?: return
        val foregroundView = episodeViewHolder.episodeRow
        val rightToLeftLayout = episodeViewHolder.rightToLeftSwipeLayout
        val leftToRightLayout = episodeViewHolder.leftToRightSwipeLayout

        ItemTouchHelper.Callback.getDefaultUIUtil().onSelected(foregroundView)
        ItemTouchHelper.Callback.getDefaultUIUtil().onSelected(rightToLeftLayout)
        ItemTouchHelper.Callback.getDefaultUIUtil().onSelected(leftToRightLayout)
    }

    override fun getMultiItemStopSize(viewHolder: RecyclerView.ViewHolder): Float {
        return if (swipeDirection ?: 0 > 0) 140.dpToPx(viewHolder.itemView.context).toFloat() else 70.dpToPx(viewHolder.itemView.context).toFloat()
    }

    override fun getMultiItemCutoffThreshold(): Float {
        return 0.5f
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return getMultiItemStopSize(viewHolder) - 1f
    }

    override fun augmentUpdateDxDy(dx: Float, dy: Float): Pair<Float, Float> {
        val transformedDx = if (swipeDirection ?: 0 < 0) {
            min(dx, 0f)
        } else if (swipeDirection ?: 0 > 0) {
            max(dx, 0f)
        } else {
            dx
        }

        return Pair(transformedDx, dy)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        return false
    }

    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        val episodeViewHolder = viewHolder as? RowSwipeable
            ?: return
        if (actionState == MultiSwipeHelper.ACTION_STATE_IDLE || swipeDirection ?: 0 > 0 && dX <= 0 || swipeDirection ?: 0 < 0 && dX >= 0 || episodeViewHolder.isMultiSelecting) {
            return
        }

        val foregroundView = episodeViewHolder.episodeRow
        val rightToLeftLayout = episodeViewHolder.rightToLeftSwipeLayout
        val leftToRightLayout = episodeViewHolder.leftToRightSwipeLayout
        val item1Icon = episodeViewHolder.leftIconDrawablesRes[0]
        val item2Icon = episodeViewHolder.leftIconDrawablesRes.getOrNull(1)

        defaultDrawWithoutTranslation(recyclerView, foregroundView, isCurrentlyActive)
        foregroundView.translationX = dX

        swipeDirection = sign(dX).toInt()

        rightToLeftLayout.isVisible = true
        leftToRightLayout.isVisible = true

        if (foregroundView.translationX < 0) {
            // Is swiping from the right
            leftToRightLayout.translationX = foregroundView.translationX

            val iconView = episodeViewHolder.swipeLeftIcon
            val rightIcon = episodeViewHolder.rightIconDrawableRes.first()
            iconView.setImageResource(rightIcon.iconRes)
            rightToLeftLayout.setBackgroundColor(rightIcon.backgroundColor)

            if (abs(foregroundView.translationX) / foregroundView.width < getMultiItemCutoffThreshold()) {
                iconView.x = max(rightToLeftLayout.width + foregroundView.translationX, rightToLeftLayout.width - iconView.width.toFloat())
            } else {
                iconView.x = rightToLeftLayout.width + foregroundView.translationX
            }
        } else {
            // Is swiping from the left
            rightToLeftLayout.translationX = foregroundView.translationX

            val item1 = leftToRightLayout.findViewById<ViewGroup>(UR.id.leftRightItem1)
            val item2 = leftToRightLayout.findViewById<ViewGroup>(UR.id.leftRightItem2)

            item1.setBackgroundColor(item1Icon.backgroundColor)
            if (item2Icon != null) {
                item2.setBackgroundColor(item2Icon.backgroundColor)
            }

            episodeViewHolder.leftRightIcon1.setImageResource(item1Icon.iconRes)
            if (item2Icon != null) {
                episodeViewHolder.leftRightIcon2.setImageResource(item2Icon.iconRes)
            } else {
                episodeViewHolder.leftRightIcon2.setImageDrawable(null)
            }

            if (foregroundView.translationX / foregroundView.width <= getMultiItemCutoffThreshold() && item2Icon != null) {
                item1.setBackgroundColor(item1Icon.backgroundColor)
                episodeViewHolder.leftRightIcon1.setImageResource(item1Icon.iconRes)

                if (item1.x >= 10f && item1.x == item2.x) {
                    // Transition between modes
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        item1.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    }
                }
                item1.x = foregroundView.translationX / 2 - item1.width
                item2.x = foregroundView.translationX - item2.width
            } else {
                item1.setBackgroundColor(item1Icon.backgroundColor)

                if (item1.x >= 10f && item1.x < item2.x) {
                    // Transition between modes
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        item1.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    }
                }
                item1.x = foregroundView.translationX - item1.width
                item2.x = foregroundView.translationX - item2.width
            }
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        val episodeViewHolder = viewHolder as? RowSwipeable
            ?: return
        val foregroundView = episodeViewHolder.episodeRow
        val rightToLeftLayout = episodeViewHolder.rightToLeftSwipeLayout
        val leftToRightLayout = episodeViewHolder.leftToRightSwipeLayout

        rightToLeftLayout.translationX = 0f
        rightToLeftLayout.isVisible = false
        leftToRightLayout.isVisible = false
        leftToRightLayout.translationX = 0f
        foregroundView.translationX = 0f

        foregroundView.elevation = findMaxElevation(recyclerView, viewHolder.itemView)

        swipeDirection = null
    }

    // Shamelessly copy pasted from ItemTouchUiUtilImpl.java
    private fun defaultDrawWithoutTranslation(
        recyclerView: RecyclerView,
        view: View,
        isCurrentlyActive: Boolean
    ) {
        if (Build.VERSION.SDK_INT >= 21) {
            if (isCurrentlyActive) {
                var originalElevation: Any? = view.getTag(androidx.recyclerview.R.id.item_touch_helper_previous_elevation)
                if (originalElevation == null) {
                    originalElevation = ViewCompat.getElevation(view)
                    val newElevation = 1f + findMaxElevation(recyclerView, view)
                    ViewCompat.setElevation(view, newElevation)
                    view.setTag(androidx.recyclerview.R.id.item_touch_helper_previous_elevation, originalElevation)
                }
            }
        }
    }

    private fun findMaxElevation(recyclerView: RecyclerView, itemView: View): Float {
        val childCount = recyclerView.childCount
        var max = 0f
        for (i in 0 until childCount) {
            val child = recyclerView.getChildAt(i)
            if (child === itemView) {
                continue
            }
            val elevation = ViewCompat.getElevation(child)
            if (elevation > max) {
                max = elevation
            }
        }
        return max
    }
}

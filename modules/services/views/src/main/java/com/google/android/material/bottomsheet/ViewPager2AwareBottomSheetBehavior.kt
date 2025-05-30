package com.google.android.material.bottomsheet

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

/**
 * [BottomSheetBehavior] that has a support for detecting nested scrolling in [ViewPager2].
 *
 * See [GitHub issue](https://github.com/Automattic/pocket-casts-android/issues/1752) for more context.
 */
open class ViewPager2AwareBottomSheetBehavior<V : View> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : BottomSheetBehavior<V>(context, attrs) {
    private var preFlingInterceptor: PreFlingInterceptor? = null

    fun setPreFlingInterceptor(interceptor: PreFlingInterceptor) {
        preFlingInterceptor = interceptor
    }

    override fun onNestedPreFling(coordinatorLayout: CoordinatorLayout, child: V, target: View, velocityX: Float, velocityY: Float): Boolean {
        val shouldDisableFling = preFlingInterceptor?.shouldInterceptFlingGesture(velocityX, velocityY) ?: false
        return if (shouldDisableFling) {
            preFlingInterceptor?.onFlingIntercepted(velocityX, velocityY)
            false
        } else {
            super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY)
        }
    }

    override fun findScrollingChild(view: View): View? {
        if (!view.isVisible) {
            return null
        }
        if (ViewCompat.isNestedScrollingEnabled(view)) {
            return view
        }
        if (view is ViewPager2) {
            val selectedPageIndex = view.currentItem
            val pagerRecycler = requireNotNull(view[0] as? RecyclerView) {
                "First child of ViewPager2 is expected to be a RecyclerView"
            }
            val viewHolder = pagerRecycler.findViewHolderForAdapterPosition(selectedPageIndex) ?: return null
            return findScrollingChild(viewHolder.itemView)
        }
        if (view is ViewGroup) {
            view.forEach { childView ->
                val scrollingChild = findScrollingChild(childView)
                if (scrollingChild != null) {
                    return scrollingChild
                }
            }
        }
        return null
    }

    interface PreFlingInterceptor {
        fun shouldInterceptFlingGesture(velocityX: Float, velocityY: Float): Boolean

        fun onFlingIntercepted(velocityX: Float, velocityY: Float)
    }
}

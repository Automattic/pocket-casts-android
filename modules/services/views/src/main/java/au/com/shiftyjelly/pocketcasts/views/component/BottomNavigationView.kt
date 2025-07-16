/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package au.com.shiftyjelly.pocketcasts.views.component

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import com.google.android.material.R
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.internal.ThemeEnforcement
import com.google.android.material.navigation.NavigationBarMenuView
import com.google.android.material.navigation.NavigationBarView
import kotlin.math.min

/**
 * This is a copy of [com.google.android.material.bottomnavigation.BottomNavigationView] but it doesn't apply
 * window insets on its own. We experience issues on Android pre SDK 30, where insets aren't applied
 * consistently. Removing automatic application of them from this class gives us more control over it.
 */
@SuppressLint("RestrictedApi", "PrivateResource")
class BottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.bottomNavigationStyle,
    defStyleRes: Int = R.style.Widget_Design_BottomNavigationView,
) : NavigationBarView(context, attrs, defStyleAttr, defStyleRes) {
    var isItemHorizontalTranslationEnabled: Boolean
        get() = (menuView as BottomNavigationMenuView).isItemHorizontalTranslationEnabled
        set(value) {
            val menuView = menuView as BottomNavigationMenuView
            if (menuView.isItemHorizontalTranslationEnabled != value) {
                menuView.isItemHorizontalTranslationEnabled = value
                presenter.updateMenuView(false)
            }
        }

    init {
        val attributes = ThemeEnforcement.obtainTintedStyledAttributes(
            context,
            attrs,
            R.styleable.BottomNavigationView,
            defStyleAttr,
            defStyleRes,
        )
        isItemHorizontalTranslationEnabled = attributes.getBoolean(
            R.styleable.BottomNavigationView_itemHorizontalTranslationEnabled,
            true,
        )
        if (attributes.hasValue(R.styleable.BottomNavigationView_android_minHeight)) {
            minimumHeight = attributes.getDimensionPixelSize(R.styleable.BottomNavigationView_android_minHeight, 0)
        }
        attributes.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minHeightSpec = makeMinHeightSpec(heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, minHeightSpec)
    }

    private fun makeMinHeightSpec(measureSpec: Int): Int {
        var minHeight = suggestedMinimumHeight
        return if (MeasureSpec.getMode(measureSpec) != MeasureSpec.EXACTLY && minHeight > 0) {
            minHeight += paddingTop + paddingBottom

            return MeasureSpec.makeMeasureSpec(
                min(MeasureSpec.getSize(measureSpec), minHeight),
                MeasureSpec.EXACTLY,
            )
        } else {
            measureSpec
        }
    }

    override fun getMaxItemCount(): Int {
        return MAX_ITEM_COUNT
    }

    override fun createNavigationBarMenuView(context: Context): NavigationBarMenuView {
        return BottomNavigationMenuView(context)
    }

    companion object {
        private const val MAX_ITEM_COUNT = 5
    }
}

package au.com.shiftyjelly.pocketcasts.view

import android.view.View
import android.view.ViewGroup

class BottomNavHideManager(val rootView: View, val bottomNavigation: ViewGroup) {
    var lastNavY = 0f
    var navHeightScale = 0f
    var navHeight = 0f

    fun onSlide(slideOffset: Float) {
        bottomNavigation.let { navigation ->
            if (navHeightScale == 0f) {
                navHeight = navigation.height.toFloat()
                navHeightScale = 1f / (navHeight / rootView.height.toFloat())
            }
            var newNavY = navHeight * (navHeightScale * slideOffset)
            if (newNavY > navHeight) {
                newNavY = navHeight
            }
            if (newNavY != lastNavY) {
                lastNavY = newNavY
                navigation.animate().cancel()
                navigation.animate().translationY(newNavY).setDuration(0).start()
            }
        }
    }
}

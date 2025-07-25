package au.com.shiftyjelly.pocketcasts.views.fragments

interface TopScrollable {
    // Returns `true` if the Fragment did actually scroll
    // (was not at the top already)
    fun scrollToTop(): Boolean
}

package au.com.shiftyjelly.pocketcasts.views.helper

interface HasBackstack {
    fun onBackPressed(): Boolean
    fun getBackstackCount(): Int
}

package au.com.shiftyjelly.pocketcasts.analytics

interface AccountStatusInfo {
    fun isLoggedIn(): Boolean
    fun getUuid(): String?
}

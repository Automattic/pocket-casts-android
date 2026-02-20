package au.com.shiftyjelly.pocketcasts.analytics

interface AccountStatusInfo {
    fun isLoggedIn(): Boolean
    fun getUserIds(): UserIds
    fun recreateAnonId(): String
}

data class UserIds(
    val accountId: String?,
    val anonId: String,
) {
    val id get() = accountId ?: anonId
}

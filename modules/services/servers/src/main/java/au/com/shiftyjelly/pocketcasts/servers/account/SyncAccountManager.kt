package au.com.shiftyjelly.pocketcasts.servers.account

import android.accounts.Account
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.preferences.RefreshToken
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import com.jakewharton.rxrelay2.BehaviorRelay

interface SyncAccountManager {

    val isLoggedInObservable: BehaviorRelay<Boolean>

    fun getUuid(): String?
    fun isLoggedIn(): Boolean
    fun isGoogleLogin(): Boolean
    fun getEmail(): String?
    fun setEmail(email: String)
    fun peekAccessToken(account: Account? = null): AccessToken?
    fun getAccessTokenBlocking(onTokenErrorUiShown: () -> Unit): AccessToken?
    suspend fun getAccessTokenSuspend(onTokenErrorUiShown: () -> Unit): AccessToken?
    fun invalidateAccessToken()
    fun signOut()
    fun updateEmail(email: String)
    fun setRefreshToken(refreshToken: RefreshToken)
    fun setAccessToken(accessToken: AccessToken)
    fun getRefreshToken(account: Account): RefreshToken?
    suspend fun loginWithGoogle(idToken: String, syncServerManager: SyncServerManager, signInSource: SignInSource,): LoginResult
    suspend fun loginWithEmailAndPassword(email: String, password: String, syncServerManager: SyncServerManager, signInSource: SignInSource,): LoginResult
    suspend fun createUserWithEmailAndPassword(email: String, password: String, syncServerManager: SyncServerManager): LoginResult
    suspend fun forgotPassword(email: String, syncServerManager: SyncServerManager, onSuccess: () -> Unit, onError: (String) -> Unit)
    suspend fun refreshAccessToken(account: Account? = null, syncServerManager: SyncServerManager): AccessToken?
}

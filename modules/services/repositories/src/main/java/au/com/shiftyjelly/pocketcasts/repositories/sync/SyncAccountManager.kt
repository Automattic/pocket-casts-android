package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.accounts.Account
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.preferences.AccountConstants
import au.com.shiftyjelly.pocketcasts.preferences.RefreshToken
import au.com.shiftyjelly.pocketcasts.servers.sync.LoginIdentity
import au.com.shiftyjelly.pocketcasts.servers.sync.TokenHandler
import au.com.shiftyjelly.pocketcasts.utils.Optional
import io.reactivex.Flowable
import kotlinx.coroutines.flow.Flow

/**
 * The only class that should use this class is the
 * [SyncManager] class. Consider using that instead of this class.
 */
interface SyncAccountManager : TokenHandler {
    override suspend fun getAccessToken(): AccessToken?
    fun addAccount(email: String, uuid: String, refreshToken: RefreshToken, accessToken: AccessToken, loginIdentity: LoginIdentity)
    fun getAccount(): Account?
    fun getEmail(): String?
    fun emailFlow(): Flow<String?>
    fun emailFlowable(): Flowable<Optional<String>>
    fun getLoginIdentity(): LoginIdentity?
    fun getRefreshToken(account: Account? = null): RefreshToken?
    fun getSignInType(account: Account): AccountConstants.SignInType
    fun getUuid(): String?
    override fun invalidateAccessToken()
    fun isLoggedIn(): Boolean
    fun peekAccessToken(account: Account): AccessToken?
    fun setAccessToken(accessToken: AccessToken)
    fun setEmail(email: String)
    fun setRefreshToken(refreshToken: RefreshToken)
    fun signOut()
}

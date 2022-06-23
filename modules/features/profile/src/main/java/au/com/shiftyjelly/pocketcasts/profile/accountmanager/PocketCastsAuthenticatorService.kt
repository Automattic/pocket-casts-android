package au.com.shiftyjelly.pocketcasts.profile.accountmanager

import android.app.Service
import android.content.Intent
import android.os.IBinder
import au.com.shiftyjelly.pocketcasts.account.AccountAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PocketCastsAuthenticatorService : Service() {
    @Inject lateinit var accountAuth: AccountAuth
    lateinit var authenticator: PocketCastsAccountAuthenticator

    override fun onCreate() {
        super.onCreate()

        authenticator = PocketCastsAccountAuthenticator(this, accountAuth)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return authenticator.iBinder
    }
}

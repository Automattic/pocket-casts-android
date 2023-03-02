package au.com.shiftyjelly.pocketcasts.profile.accountmanager

import android.app.Service
import android.content.Intent
import android.os.IBinder
import au.com.shiftyjelly.pocketcasts.servers.account.SyncAccountManager
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PocketCastsAuthenticatorService : Service() {
    @Inject lateinit var syncAccountManager: SyncAccountManager
    @Inject lateinit var syncServerManager: SyncServerManager
    lateinit var authenticator: PocketCastsAccountAuthenticator

    override fun onCreate() {
        super.onCreate()

        authenticator = PocketCastsAccountAuthenticator(this, syncAccountManager, syncServerManager)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return authenticator.iBinder
    }
}

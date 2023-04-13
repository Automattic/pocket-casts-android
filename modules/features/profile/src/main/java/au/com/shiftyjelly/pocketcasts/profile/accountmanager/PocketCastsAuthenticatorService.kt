package au.com.shiftyjelly.pocketcasts.profile.accountmanager

import android.app.Service
import android.content.Intent
import android.os.IBinder
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PocketCastsAuthenticatorService : Service() {
    @Inject lateinit var syncManager: SyncManager
    lateinit var authenticator: PocketCastsAccountAuthenticator

    override fun onCreate() {
        super.onCreate()
        authenticator = PocketCastsAccountAuthenticator(this, syncManager)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return authenticator.iBinder
    }
}

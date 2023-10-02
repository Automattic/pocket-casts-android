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

    private var authenticator: PocketCastsAccountAuthenticator? = null

    override fun onCreate() {
        super.onCreate()
        authenticator = PocketCastsAccountAuthenticator(this, syncManager)
    }

    override fun onDestroy() {
        super.onDestroy()
        authenticator = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return authenticator?.iBinder
    }
}

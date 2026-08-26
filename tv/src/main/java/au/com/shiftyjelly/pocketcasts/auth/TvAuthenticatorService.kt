package au.com.shiftyjelly.pocketcasts.auth

import android.app.Service
import android.content.Intent
import android.os.IBinder

class TvAuthenticatorService : Service() {
    private var authenticator: TvAccountAuthenticator? = null

    override fun onCreate() {
        super.onCreate()
        authenticator = TvAccountAuthenticator(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return authenticator?.iBinder
    }
}

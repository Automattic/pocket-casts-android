package au.com.shiftyjelly.pocketcasts.servers.sync.old

import android.content.Context
import android.os.Build
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.di.OldSyncServerRetrofit
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Single
import retrofit2.Retrofit
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncOldServerManager @Inject constructor(@OldSyncServerRetrofit retrofit: Retrofit, val settings: Settings, @ApplicationContext val context: Context) {

    val server = retrofit.create(SyncOldServer::class.java)

    fun syncUpdate(data: String, lastModified: String): Single<SyncUpdateResponse> {
        val fields = mutableMapOf<String, String>()

        addDeviceFields(fields)

        val email = settings.getSyncEmail()
        val password = settings.getSyncPassword()
        if (email == null || password == null) {
            return Single.error(Exception("Not logged in"))
        }
        addUserSecurity(email, password, fields)

        fields.put("data", data)
        fields.put("device_utc_time_ms", System.currentTimeMillis().toString())
        fields.put("last_modified", lastModified)

        return server.syncUpdate(fields)
    }

    private fun addUserSecurity(email: String, password: String, fields: MutableMap<String, String>) {
        with(fields) {
            put("email", email)
            put("password", password)
            settings.getSyncRefreshToken()?.let { put("token", it) }
        }
    }

    private fun addDeviceFields(fields: MutableMap<String, String>) {
        with(fields) {
            put("v", Settings.PARSER_VERSION)
            put("av", settings.getVersion())
            put("ac", "" + settings.getVersionCode())
            put("dt", "2")
            put("c", Locale.getDefault().country)
            put("l", Locale.getDefault().language)
            put("m", Build.MODEL)
        }
    }
}

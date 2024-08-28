package au.com.shiftyjelly.pocketcasts.account.watchsync

import androidx.datastore.core.Serializer
import au.com.shiftyjelly.pocketcasts.preferences.RefreshToken
import au.com.shiftyjelly.pocketcasts.servers.sync.LoginIdentity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
data class WatchSyncAuthData(
    @field:Json(name = "refreshToken") val refreshToken: RefreshToken,
    @field:Json(name = "loginIdentity") val loginIdentity: LoginIdentity,
)

class WatchSyncAuthDataSerializer @Inject constructor(
    moshi: Moshi,
) : Serializer<WatchSyncAuthData?> {
    private val adapter = moshi.adapter(WatchSyncAuthData::class.java)

    override val defaultValue: WatchSyncAuthData? = null

    override suspend fun readFrom(input: InputStream): WatchSyncAuthData? {
        val string = InputStreamReader(input).readText()
        return adapter.fromJson(string)
    }

    override suspend fun writeTo(t: WatchSyncAuthData?, output: OutputStream) {
        withContext(Dispatchers.IO) {
            if (t != null) {
                val jsonString = adapter.toJson(t)
                output.write(jsonString.toByteArray())
            }
        }
    }
}

package au.com.shiftyjelly.pocketcasts.account.watchsync

import androidx.datastore.core.Serializer
import au.com.shiftyjelly.pocketcasts.preferences.RefreshToken
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginIdentity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream

@JsonClass(generateAdapter = true)
data class WatchSyncAuthData(
    @field:Json(name = "refreshToken") val refreshToken: RefreshToken,
    @field:Json(name = "loginIdentity") val loginIdentity: LoginIdentity,
)

object WatchSyncAuthDataSerializer : Serializer<WatchSyncAuthData?> {

    private val adapter = WatchSyncAuthDataJsonAdapter(
        Moshi.Builder()
            .add(RefreshToken::class.java, RefreshToken.Adapter)
            .add(LoginIdentity.Adapter)
            .build()
    )

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

package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import retrofit2.HttpException
import timber.log.Timber

@JsonClass(generateAdapter = true)
data class TokenErrorResponse(
    @Json(name = "error") val error: String,
    @Json(name = "error_description") val errorDescription: String? = null,
)

fun HttpException.parseTokenErrorResponse(moshi: Moshi): TokenErrorResponse? {
    val errorBody = this.response()?.errorBody() ?: return null
    return try {
        moshi.adapter(TokenErrorResponse::class.java).fromJson(errorBody.source())
    } catch (e: Exception) {
        Timber.e(e)
        null
    }
}

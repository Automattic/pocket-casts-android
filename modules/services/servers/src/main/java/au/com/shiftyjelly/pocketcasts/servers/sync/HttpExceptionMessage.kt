package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import retrofit2.HttpException

fun HttpException.parseErrorMessage(): String? {
    val errorBody = this.response()?.errorBody()
    if (errorBody != null) {
        val errorMoshi = Moshi.Builder().build()

        try {
            val response = errorMoshi.adapter(PCErrorBody::class.java).fromJson(errorBody.source())
            if (response != null) {
                return response.message
            }
        } catch (e: Exception) {
            return null
        }
    }

    return null
}

@JsonClass(generateAdapter = true)
data class PCErrorBody(@field:Json(name = "errorMessage") val message: String)

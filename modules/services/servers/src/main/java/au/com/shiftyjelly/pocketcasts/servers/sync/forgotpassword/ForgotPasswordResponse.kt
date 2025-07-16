package au.com.shiftyjelly.pocketcasts.servers.sync.forgotpassword

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ForgotPasswordResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String,
)

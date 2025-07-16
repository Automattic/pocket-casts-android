package au.com.shiftyjelly.pocketcasts.servers.sync.forgotpassword

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ForgotPasswordRequest(
    @Json(name = "email") val email: String,
)

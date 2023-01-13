package au.com.shiftyjelly.pocketcasts.servers.sync.forgotpassword

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ForgotPasswordResponse(
    @field:Json(name = "success") val success: Boolean,
    @field:Json(name = "message") val message: String
)

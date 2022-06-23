package au.com.shiftyjelly.pocketcasts.servers

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiServer {

    @POST("/user/login")
    fun login(@Body request: ApiTokenRequest): Call<ApiTokenResponse>
}

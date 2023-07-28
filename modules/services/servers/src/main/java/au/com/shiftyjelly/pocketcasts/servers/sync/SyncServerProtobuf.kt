package au.com.shiftyjelly.pocketcasts.servers.sync

import com.pocketcasts.service.api.BookmarkRequest
import com.pocketcasts.service.api.BookmarksResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface SyncServerProtobuf {

    @Headers("Content-Type: application/octet-stream")
    @POST("/user/bookmark/list")
    suspend fun getBookmarkList(@Header("Authorization") authorization: String, @Body request: BookmarkRequest): BookmarksResponse
}
